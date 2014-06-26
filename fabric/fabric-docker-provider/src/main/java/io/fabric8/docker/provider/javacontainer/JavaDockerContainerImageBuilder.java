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
package io.fabric8.docker.provider.javacontainer;

import io.fabric8.agent.mvn.Parser;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.common.util.Closeables;
import io.fabric8.common.util.Strings;
import io.fabric8.container.process.JavaContainerConfig;
import io.fabric8.container.process.JolokiaAgentHelper;
import io.fabric8.deployer.JavaContainers;
import io.fabric8.docker.api.Docker;
import io.fabric8.docker.provider.CreateDockerContainerOptions;
import io.fabric8.process.manager.support.ProcessUtils;
import io.fabric8.service.child.ChildConstants;
import io.fabric8.service.child.JavaContainerEnvironmentVariables;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static io.fabric8.common.util.Strings.join;
import static io.fabric8.docker.api.DockerFactory.resolveDockerHost;
import static java.util.Arrays.asList;

/**
 * Creates a docker image, adding java deployment units from the profile metadata.
 */
public class JavaDockerContainerImageBuilder {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(JavaDockerContainerImageBuilder.class);

    private File tempDirectory;

    public String generateContainerImage(FabricService fabric, Container container, List<Profile> profileList, Docker docker, JavaContainerOptions options, JavaContainerConfig javaConfig, CreateDockerContainerOptions containerOptions, ExecutorService downloadExecutor, Map<String, String> envVars) throws Exception {
        String libDirAndSeparator = ensureEndsWithFileSeparator(options.getJavaLibraryPath());
        String homeDirAndSeparator = ensureEndsWithFileSeparator(options.getHomePath());
        Map<String, Parser> artifacts = JavaContainers.getJavaContainerArtifacts(fabric, profileList, downloadExecutor);

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

        Set<Map.Entry<String, Parser>> entries = artifacts.entrySet();
        for (Map.Entry<String, Parser> entry : entries) {
            Parser parser = entry.getValue();
            String path = parser.getArtifactPath();
            String url = repoTextPrefix + path;
            String version = parser.getVersion();
            String snapshotModifier = "";
            // avoid the use of the docker cache for snapshot dependencies
            if (version != null && version.contains("SNAPSHOT")) {
                long time = new Date().getTime();
                url += "?t=" + time;
                snapshotModifier = "-" + time;
            }
            String fileName = parser.getArtifact() + "-" + version + snapshotModifier + "." + parser.getType();
            String filePath = libDirAndSeparator + fileName;

            dockerFile.add(url, filePath);
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
        if (Strings.isNotBlank(restAPI)) {
            addContainerOverlays(dockerFile, restAPI, fabric, container, profileList, docker, options, javaConfig, containerOptions, envVars, homeDirAndSeparator);
        } else {
            LOGGER.error("Cannot perform container overlays as there is no REST API for fabric8!");
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

        // TODO we should keep a cache of the Dockerfile text for each profile so we don't create it each time

        // lets use the command line for now....
        File tmpFile = File.createTempFile("fabric-", ".dockerfiledir");
        tmpFile.delete();
        tmpFile.mkdirs();

        dockerFile.writeTo(new File(tmpFile, "Dockerfile"));

        // lets use the docker command line for now...
        String[] commands = new String[]{"docker", "build", "-t", tag, tmpFile.getCanonicalPath()};

        String message = join(asList(commands), " ");
        LOGGER.info("Executing commands: " + message);
        String answer = null;
        String errors = null;
        String dockerHost = resolveDockerHost();
        try {
            ProcessBuilder dockerBuild = new ProcessBuilder().command(commands);
            Map<String, String> env = dockerBuild.environment();
            env.put("DOCKER_HOST", dockerHost);
            Process process = dockerBuild.start();
            answer = parseCreatedImage(process.getInputStream(), message);
            errors = processErrors(process.getErrorStream(), message);
        } catch (Exception e) {
            LOGGER.error("Failed to execute process " + "stdin" + " for " +
                    message +
                    ": " + e, e);
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

    protected void addContainerOverlays(DockerFileBuilder dockerFile, String restAPI, FabricService fabricService, Container container, List<Profile> profiles, Docker docker, JavaContainerOptions options, JavaContainerConfig javaConfig, CreateDockerContainerOptions containerOptions, Map<String, String> environmentVariables, String homeDirAndSeparator) throws Exception {
        Set<String> profileIds = containerOptions.getProfiles();
        String versionId = containerOptions.getVersion();
        String layout = javaConfig.getOverlayFolder();
        if (layout != null) {
            for (Profile profile : profiles) {
                Map<String, String> configuration = ProcessUtils.getProcessLayout(profile, layout);
                if (configuration != null && !configuration.isEmpty()) {
                    String profileRestApi = restAPI + "/version/" + profile.getVersion() + "/profile/"
                            + profile.getId() + "/overlay/file/" + layout + (layout.endsWith("/") ? "" : "/");
                    Map variables = Profiles.getOverlayConfiguration(fabricService, profileIds, versionId, ChildConstants.TEMPLATE_VARIABLES_PID);
                    if (variables == null) {
                        variables = new HashMap();
                    } else {
                        CuratorFramework curator = fabricService.adapt(CuratorFramework.class);
                        JolokiaAgentHelper.substituteEnvironmentVariableExpressions(variables, environmentVariables, fabricService, curator);
                    }
                    variables.putAll(environmentVariables);
                    LOGGER.info("Using template variables for MVEL: " + variables);
                    new ApplyConfigurationStep(dockerFile, profileRestApi, configuration, variables, getTempDirectory(), homeDirAndSeparator).install();
                }
            }
        }
        Map<String, String> overlayResources = Profiles.getOverlayConfiguration(fabricService, profileIds, versionId, ChildConstants.PROCESS_CONTAINER_OVERLAY_RESOURCES_PID);
        if (overlayResources != null && !overlayResources.isEmpty()) {
            File baseDir = getTempDirectory();
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
                        // TODO find the URL of the resource in the maven repo and add it like we do with maven dependencies above
                        LOGGER.warn("TODO - add overlay resources into a docker file for URL: " + url);
/*
                        File newFile = new File(baseDir, localPath);
                        newFile.getParentFile().mkdirs();
                        InputStream stream = url.openStream();
                        if (stream != null) {
                            Files.copy(stream, new BufferedOutputStream(new FileOutputStream(newFile)));

                            // now lets add to the Dockerfile
                            dockerFile.add(newFile.getAbsolutePath(), localPath);
                        }
*/
                    }
                }
            }
        }
    }

    protected File getTempDirectory() throws IOException {
        if (tempDirectory == null) {
            tempDirectory = File.createTempFile("fabric8-docker-image", "dir");
            tempDirectory.delete();
            tempDirectory.mkdirs();
            tempDirectory.deleteOnExit();
        }
        return tempDirectory;
    }

    protected String parseCreatedImage(InputStream inputStream, String message) throws Exception {
        String answer = null;
        String prefix = "Successfully built";
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                LOGGER.info("message: " + line);
                line = line.trim();
                if (line.length() > 0 && line.startsWith(prefix)) {
                    answer = line.substring(prefix.length()).trim();
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to process stdin for " +
                    message +
                    ": " + e, e);
            throw e;
        } finally {
            Closeables.closeQuitely(reader);
        }
        return answer;
    }

    protected String processErrors(InputStream inputStream, String message) throws Exception {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append(line);
                LOGGER.info(line);
            }
            return builder.toString();

        } catch (Exception e) {
            LOGGER.error("Failed to process stderr for " +
                    message +
                    ": " + e, e);
            throw e;
        } finally {
            Closeables.closeQuitely(reader);
        }
    }
}
