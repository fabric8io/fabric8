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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import io.fabric8.agent.mvn.Parser;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.common.util.Strings;
import io.fabric8.deployer.JavaContainers;
import io.fabric8.docker.api.Docker;
import io.fabric8.service.child.JavaContainerEnvironmentVariables;
import io.fabric8.common.util.Closeables;
import io.fabric8.common.util.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a docker image, adding java deployment units from the profile metadata.
 */
public class JavaDockerContainerImageBuilder {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(JavaDockerContainerImageBuilder.class);

    public String generateContainerImage(FabricService fabric, Container container, List<Profile> profileList, Docker docker, JavaContainerOptions options, ExecutorService downloadExecutor, Map<String, String> envVars) throws Exception {
        String libDir = options.getJavaLibraryPath();
        String libDirPrefix = libDir;
        if (!libDir.endsWith("/") && !libDir.endsWith(File.separator)) {
            libDirPrefix += File.separator;
        }
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

        String[] copiedEnvVars = JavaContainerEnvironmentVariables.ALL_ENV_VARS;
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
        String errors = null;
        try {
            process = runtime.exec(commands);
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
