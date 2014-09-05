/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.docker.provider.customizer;

import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.common.util.Files;
import io.fabric8.common.util.Objects;
import io.fabric8.common.util.Strings;
import io.fabric8.container.process.JavaContainerConfig;
import io.fabric8.container.process.JolokiaAgentHelper;
import io.fabric8.deployer.JavaContainers;
import io.fabric8.docker.api.Docker;
import io.fabric8.docker.api.Dockers;
import io.fabric8.docker.provider.CreateDockerContainerOptions;
import io.fabric8.process.manager.support.ProcessUtils;
import io.fabric8.service.child.JavaContainerEnvironmentVariables;
import org.apache.curator.framework.CuratorFramework;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Creates a docker image, adding deployment units, overlays and environment vairables from the profile metadata.
 */
public class CustomDockerContainerImageBuilder {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(CustomDockerContainerImageBuilder.class);

    public String generateContainerImage(FabricService fabric, Container container, List<Profile> profileList, Docker docker, CustomDockerContainerImageOptions options, JavaContainerConfig javaConfig, CreateDockerContainerOptions containerOptions, ExecutorService downloadExecutor, Map<String, String> envVars) throws Exception {
        String libDirAndSeparator = ensureEndsWithFileSeparator(options.getJavaLibraryPath());
        String deployDirAndSeparator = ensureEndsWithFileSeparator(options.getJavaDeployPath());
        String homeDirAndSeparator = ensureEndsWithFileSeparator(options.getHomePath());
        Map<String, File> artifacts = JavaContainers.getJavaContainerArtifactsFiles(fabric, profileList, downloadExecutor);

        URI mavenRepoURI = fabric.getMavenRepoURI();
        String repoTextPrefix = mavenRepoURI.toString();
        int idx = repoTextPrefix.indexOf("://");
        if (idx > 0) {
            repoTextPrefix = repoTextPrefix.substring(idx + 3);
        }
        repoTextPrefix = "http://" + fabric.getZooKeeperUser() + ":" + fabric.getZookeeperPassword() + "@" + repoTextPrefix;

        String baseImage = options.getBaseImage();
        String tag = options.getNewImageTag();

        DockerFileBuilder dockerFile = DockerFileBuilder.from(baseImage);

        File tmpDockerfileDir = File.createTempFile("fabric-", ".dockerfiledir");
        tmpDockerfileDir.delete();
        tmpDockerfileDir.mkdirs();

        String libDirPath = "lib";
        File uploadLibDir = new File(tmpDockerfileDir, libDirPath);

        String deployDirPath = "deploy";
        File uploadDeployDir = new File(tmpDockerfileDir, deployDirPath);

        String overlaysDirPath = "overlays";
        File overlaysDir = new File(tmpDockerfileDir, overlaysDirPath);

        int libFileCount = 0;
        int deployFileCount = 0;
        Set<Map.Entry<String, File>> entries = artifacts.entrySet();
        for (Map.Entry<String, File> entry : entries) {
            File file = entry.getValue();
            String fileName = file.getName();
            File outputDir;
            if (fileName.toLowerCase().endsWith(".jar")) {
                outputDir = uploadLibDir;
                libFileCount++;
            }
            else {
                outputDir = uploadDeployDir;
                deployFileCount++;
            }
            outputDir.mkdirs();
            Files.copy(file, new File(outputDir, fileName));
        }
        if (libFileCount > 0) {
            dockerFile.add(libDirPath, libDirAndSeparator);
        }
        if (deployFileCount > 0) {
            if (libFileCount == 0 || !Objects.equal(libDirPath, overlaysDirPath)) {
                dockerFile.add(deployDirPath, deployDirAndSeparator);
            }
        }

        if (container != null) {
            List<String> bundles = new ArrayList<String>();
            for (String name : artifacts.keySet()) {
                if (name.startsWith("fab:")) {
                    name = name.substring(4);
                }
                bundles.add(name);
            }
            Collections.sort(bundles);
            container.setProvisionList(bundles);
        }

        String restAPI = fabric.getRestAPI();
        int overlays = 0;
        if (Strings.isNotBlank(restAPI)) {
            overlays = addContainerOverlays(dockerFile, restAPI, fabric, container, profileList, javaConfig, containerOptions, envVars, homeDirAndSeparator, overlaysDir, tmpDockerfileDir);
            String[] childFiles = overlaysDir.list();
            if (childFiles != null && childFiles.length > 0) {
                dockerFile.add(overlaysDirPath, homeDirAndSeparator);
            }
        } else {
            LOGGER.error("Cannot perform container overlays as there is no REST API for fabric8!");
        }

        if (overlays == 0 && libFileCount == 0 && deployFileCount == 0) {
            LOGGER.info("Not creating a custom docker container as no files to deploy or overlays");
            return null;
        }

        String[] copiedEnvVars = JavaContainerEnvironmentVariables.ALL_ENV_VARS;
        for (String envVarName : copiedEnvVars) {
            String value = envVars.get(envVarName);
            if (value != null) {
                dockerFile.env(envVarName, value);
            }
        }

        String entryPoint = options.getEntryPoint();
        if (Strings.isNotBlank(entryPoint)) {
            dockerFile.cmd(entryPoint);
        }

        dockerFile.writeTo(new File(tmpDockerfileDir, "Dockerfile"));

        // lets create a tarball so we can post it to docker via REST
        File tmpArchive = File.createTempFile("fabric8-", ".dockerarchive");
        createDockerArchive(tmpArchive, tmpDockerfileDir);

        String answer = tag;
        Object errors = null;
        try {
            LOGGER.info("POSTing archive " + tmpArchive.getCanonicalPath() + " from docker archive folder " + tmpDockerfileDir.getCanonicalPath());
            Object results = docker.build(tmpArchive, tag, 0, 0, 1, 1);
            LOGGER.info("Docker Build Result: " + results);
        } catch (Exception e) {
            LOGGER.error("Failed to upload docker folder: " + tmpDockerfileDir
                    + ": " + Dockers.dockerErrorMessage(e)
                    + ". " + e, e);
            throw e;
        }
        if (answer == null) {
            LOGGER.error("Failed to create image " + errors);
            throw new CreateDockerImageFailedException("Failed to create docker image: " + errors);
        } else {
            LOGGER.info("Created Image: " + answer);
            return answer;
        }
    }

    protected void createDockerArchive(File archive, File dockerDir) throws IOException {
        TarArchiver archiver = new TarArchiver();
        archiver.addDirectory(dockerDir);
        archiver.setDestFile(archive);
        archiver.createArchive();
    }

    protected String ensureEndsWithFileSeparator(String path) {
        if (path == null) {
            path = ".";
        }
        String answer = path;
        if (!path.endsWith("/") && !path.endsWith(File.separator)) {
            answer += File.separator;
        }
        return answer;
    }

    protected int addContainerOverlays(DockerFileBuilder dockerFile, String restAPI, FabricService fabricService, Container container, List<Profile> profiles, JavaContainerConfig javaConfig, CreateDockerContainerOptions containerOptions, Map<String, String> environmentVariables, String homeDirAndSeparator, File overlaysDir, File tmpDockerfileDir) throws Exception {
        Set<String> profileIds = containerOptions.getProfiles();
        String versionId = containerOptions.getVersion();
        String layout = javaConfig.getOverlayFolder();
        int overlays = 0;
        if (layout != null) {
            for (Profile profile : profiles) {
                Map<String, String> configuration = ProcessUtils.getProcessLayout(fabricService, profile, layout);
                if (configuration != null && !configuration.isEmpty()) {
                    String profileRestApi = restAPI + "/version/" + profile.getVersion() + "/profile/"
                            + profile.getId() + "/overlay/file/" + layout + (layout.endsWith("/") ? "" : "/");
                    Map variables = Profiles.getOverlayConfiguration(fabricService, profileIds, versionId, Constants.TEMPLATE_VARIABLES_PID);
                    if (variables == null) {
                        variables = new HashMap();
                    } else {
                        CuratorFramework curator = fabricService.adapt(CuratorFramework.class);
                        JolokiaAgentHelper.substituteEnvironmentVariableExpressions(variables, environmentVariables, fabricService, curator);
                    }
                    variables.putAll(environmentVariables);
                    LOGGER.info("Using template variables for MVEL: " + variables);
                    overlaysDir.mkdirs();
                    new ApplyConfigurationStep(dockerFile, profileRestApi, configuration, variables, overlaysDir, homeDirAndSeparator).install();
                    overlays++;
                }
            }
        }
        Map<String, String> overlayResources = Profiles.getOverlayConfiguration(fabricService, profileIds, versionId, Constants.PROCESS_CONTAINER_OVERLAY_RESOURCES_PID);
        if (overlayResources != null && !overlayResources.isEmpty()) {
            Set<Map.Entry<String, String>> entries = overlayResources.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                String localPath = entry.getKey();
                String urlText = entry.getValue();
                if (Strings.isNotBlank(urlText)) {
                    URL url = null;
                    try {
                        url = new URL(urlText);
                    } catch (MalformedURLException e) {
                        LOGGER.warn("Ignoring invalid URL '" + urlText + "' for overlay resource " + localPath + ". " + e, e);
                    }
                    if (url != null) {
                        File newFile = new File(tmpDockerfileDir, localPath);
                        newFile.getParentFile().mkdirs();
                        InputStream stream = url.openStream();
                        if (stream != null) {
                            Files.copy(stream, new BufferedOutputStream(new FileOutputStream(newFile)));

                            // now lets add to the Dockerfile
                            dockerFile.add(localPath, homeDirAndSeparator + localPath);
                            overlays++;
                        }
                    }
                }
            }
        }
        return overlays;
    }

}
