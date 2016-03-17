/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.profiles.containers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.fabric8.profiles.Profiles;

public class Containers {

    public static final String DEFAULT_CONTAINER_TYPE = "karaf";

    private static final String VERSION_PROPERTY = "version";
    private static final String NAME_PROPERTY = "name";

    private static final String CONTAINERS = "fabric/configs/containers/%s.cfg";
    private static final String VERSIONS = "fabric/configs/versions/%s/containers/%s";

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final Path repository;
    private Map<String, ProjectReifier> reifierMap;
    private Profiles profiles;

    /**
     * @param repository container configuration directory, exported from ZK repository.
     * @param reifierMap map from container types to reifiers.
     */
    public Containers(Path repository, Map<String, ProjectReifier> reifierMap, Profiles profiles) {
        this.repository = repository;
        this.reifierMap = reifierMap;
        this.profiles = profiles;
    }

    /**
     * @param target        is the directory where reified container will be written to.
     * @param name name of the container to reify.
     * @throws IOException  on error.
     */
    public void reify(Path target, String name) throws IOException {
        // read default version
        final String defaultVersion = getDefaultVersion();

        // read container config
        final Path configPath = getConfigPath(name);
        if (!Files.exists(configPath)) {
            throw new IOException("Missing container config " + configPath);
        }
        final Properties config = getContainerConfig(configPath);
        config.put(NAME_PROPERTY, name);

        // read container profiles
        final String version = config.getProperty(VERSION_PROPERTY, defaultVersion);
        final String[] containerProfiles = getContainerProfiles(name, version);

        final String containerType = config.getProperty("container-type", DEFAULT_CONTAINER_TYPE);
        final ProjectReifier reifier = reifierMap.get(containerType);

        reifier.reify(target, config, profiles, containerProfiles);
    }

    private String getDefaultVersion() throws IOException {
        final Path defaultVersionPath = repository.resolve("fabric/configs/default-version.cfg");
        if (!Files.exists(defaultVersionPath)) {
            throw new IOException("Missing " + defaultVersionPath);
        }
        final List<String> lines = Files.readAllLines(defaultVersionPath, UTF_8);
        if (lines.isEmpty()) {
            throw new IOException("Missing default version value");
        }
        return lines.get(0).trim();
    }

    private Properties getContainerConfig(Path configPath) throws IOException {
        final Properties configProps = new Properties();

        for (String line : Files.readAllLines(configPath, UTF_8)) {
            if (line.contains("=")) {
                final String[] keyValue = line.split("=");
                configProps.put(keyValue[0], keyValue[1]);
            } else {
                configProps.put(VERSION_PROPERTY, line);
            }
        }

        return configProps;
    }

    private String[] getContainerProfiles(String name, String version) throws IOException {
        final Path path = repository.resolve(String.format(VERSIONS, version, name));
        if (Files.notExists(path)) {
            throw new IOException("Missing container profile file " + path);
        }

        final List<String> allLines = Files.readAllLines(path, UTF_8);
        if (allLines.isEmpty()) {
            throw new IOException("Error reading profiles for container " + name + ":" + version);
        }

        return allLines.get(0).split(" ");
    }

    private Path getConfigPath(String containerName) {
        return repository.resolve(String.format(CONTAINERS, containerName));
    }

}
