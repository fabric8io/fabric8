/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.docker.provider.javacontainer;

import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.DownloadManagers;
import io.fabric8.agent.mvn.Parser;
import io.fabric8.agent.utils.AgentUtils;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.common.util.Objects;
import io.fabric8.common.util.Strings;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DtoHelper;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.docker.api.Docker;
import io.fabric8.docker.provider.DockerConstants;
import io.fabric8.utils.Closeables;
import io.fabric8.utils.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;

/**
 * Creates a docker image, adding java deployment units from the profile metadata.
 */
public class javaContainerImageBuilder {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(javaContainerImageBuilder.class);


    public String generateContainerImage(FabricService fabric, Container container, List<Profile> profileList, Docker docker, JavaContainerOptions options, ExecutorService downloadExecutor, Map<String, String> envVars) throws Exception {
        String libDir = options.getJavaLibraryPath();
        String libDirPrefix = libDir;
        if (!libDir.endsWith("/") && !libDir.endsWith(File.separator)) {
            libDirPrefix += File.separator;
        }
        Map<String, Parser> artifacts = new TreeMap<String, Parser>();
        for (Profile profile : profileList) {
            DownloadManager downloadManager = DownloadManagers.createDownloadManager(fabric, profile, downloadExecutor);
            Map<String, Parser> profileArtifacts = AgentUtils.getProfileArtifacts(downloadManager, profile);
            artifacts.putAll(profileArtifacts);
            appendMavenDependencies(artifacts, profile);
        }

        URI mavenRepoURI = fabric.getMavenRepoURI();
        String repoTextPrefix = mavenRepoURI.toString();
        int idx = repoTextPrefix.indexOf("://");
        if (idx > 0) {
            repoTextPrefix = repoTextPrefix.substring(idx + 3);
        }
        repoTextPrefix = "http://" + fabric.getZooKeeperUser() + ":" + fabric.getZookeeperPassword() + "@" + repoTextPrefix;

        String baseImage = options.getBaseImage();
        String tag = options.getNewImageTag();

        StringBuilder buffer = new StringBuilder();
        buffer.append("FROM " + baseImage + "\n\n");

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
            String filePath = libDirPrefix + fileName;

            buffer.append("ADD " + url + " " + filePath + "\n");
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

        String[] copiedEnvVars = DockerConstants.JAVA_CONTAINER_ENV_VARS.ALL_ENV_VARS;
        for (String envVarName : copiedEnvVars) {
            String value = envVars.get(envVarName);
            if (value != null) {
                buffer.append("ENV " + envVarName + " " + value  + " \n");
            }
        }

        String entryPoint = options.getEntryPoint();
        if (Strings.isNotBlank(entryPoint)) {
            buffer.append("CMD " + entryPoint + "\n");
        }

        String source = buffer.toString();

        // TODO we should keep a cache of the Dockerfile text for each profile so we don't create it each time

        // lets use the command line for now....
        File tmpFile = File.createTempFile("fabric-", ".dockerfiledir");
        tmpFile.delete();
        tmpFile.mkdirs();
        File dockerFile = new File(tmpFile, "Dockerfile");
        Files.writeToFile(dockerFile, source.getBytes());

        // lets use the docker command line for now...
        String commands = "docker build -t " + tag + " " + tmpFile.getCanonicalPath();

        Process process = null;
        Runtime runtime = Runtime.getRuntime();
        String message = commands;
        LOGGER.info("Executing commands: " + message);
        String answer = null;
        try {
            process = runtime.exec(commands);
            answer = parseCreatedImage(process.getInputStream(), message);
            processErrors(process.getErrorStream(), message);
        } catch (Exception e) {
            LOGGER.error("Failed to execute process " + "stdin" + " for " +
                    message +
                    ": " + e, e);
            throw e;
        }
        LOGGER.info("Created Image: " + answer);
        return answer;
    }

    protected void appendMavenDependencies(Map<String, Parser> artifacts, Profile profile) {
        List<String> configurationFileNames = profile.getConfigurationFileNames();
        for (String configurationFileName : configurationFileNames) {
            if (configurationFileName.startsWith("modules/") && configurationFileName.endsWith("-requirements.json")) {
                byte[] data = profile.getFileConfiguration(configurationFileName);
                try {
                    ProjectRequirements requirements = DtoHelper.getMapper().readValue(data, ProjectRequirements.class);
                    if (requirements != null) {
                        DependencyDTO rootDependency = requirements.getRootDependency();
                        if (rootDependency != null) {
                            addMavenDependencies(artifacts, rootDependency);
                        }
                    }

                } catch (IOException e) {
                    LOGGER.error("Failed to parse project requirements from " + configurationFileName + ". " + e, e);
                }
            }
        }
    }

    protected void addMavenDependencies(Map<String, Parser> artifacts, DependencyDTO dependency) throws MalformedURLException {
        String url = dependency.toBundleUrl();
        Parser parser = Parser.parsePathWithSchemePrefix(url);
        String scope = dependency.getScope();
        if (!artifacts.containsKey(url) && !artifacts.containsValue(parser) && !(Objects.equal("test", scope))) {
            LOGGER.debug("Adding url: " + url + " parser: " + parser);
            artifacts.put(url, parser);
        }
        List<DependencyDTO> children = dependency.getChildren();
        if (children != null) {
            for (DependencyDTO child : children) {
                addMavenDependencies(artifacts, child);
            }
        }
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

    protected void processErrors(InputStream inputStream, String message) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                LOGGER.info(line);
            }

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
