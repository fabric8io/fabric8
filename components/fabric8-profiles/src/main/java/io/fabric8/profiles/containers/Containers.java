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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import io.fabric8.profiles.Profiles;
import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.containers.karaf.KarafProjectReifier;

/**
 * Utility class for reading container configurations and generating containers.
 */
public class Containers {

    public static final String NAME_PROPERTY = "name";
    public static final String PROFILES_PROPERTY = "profiles";
    public static final String CONTAINER_TYPE_PROPERTY = "container-type";

    private static final String DEFAULT_CONTAINER_TYPE =
        KarafProjectReifier.CONTAINER_TYPE + " " + JenkinsfileReifier.CONTAINER_TYPE;

    private static final String CONTAINERS = "containers/%s.cfg";

    private final Path repository;
    private final Profiles profiles;
    private Map<String, ProjectReifier> reifierMap;

    /**
     * @param repository    container configuration directory.
     * @param reifierMap    map from container types to reifiers.
     * @param profiles      profiles repository.
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
        // read container config
        final Properties config = getContainerConfig(name);

        // container profiles and types
        List<String> containerProfiles = Arrays.asList(
            config.getProperty(PROFILES_PROPERTY, Profiles.DEFAULT_PROFILE).split(" "));
        final String[] containerType = config.getProperty(CONTAINER_TYPE_PROPERTY, DEFAULT_CONTAINER_TYPE).split(" ");

        // get reifier from type
        final ProjectReifier[] reifiers = getProjectReifiers(containerType);

        // temp dir for materialized profile
        final Path profilesDir = Files.createTempDirectory(target, "profiles-");

        try {
            // remove ensemble profiles fabric-ensemble-* from fabric8 v1
            containerProfiles = containerProfiles.stream()
                .filter(p -> !p.matches("fabric\\-ensemble\\-.*"))
                .collect(Collectors.toList());

            // materialize profile
            profiles.materialize(profilesDir, containerProfiles.toArray(new String[containerProfiles.size()]));

            // reify
            for (ProjectReifier reifier : reifiers) {
                reifier.reify(target, config, profilesDir);
            }

        } finally {
            ProfilesHelpers.deleteDirectory(profilesDir);
        }
    }

    private ProjectReifier[] getProjectReifiers(String[] containerType) throws IOException {
        final List<ProjectReifier> reifiers = new ArrayList<ProjectReifier>();
        for (String type : containerType) {
            final ProjectReifier reifier = reifierMap.get(type);
            if (reifier == null) {
                throw new IOException("Unknown container type " + type);
            }
            reifiers.add(reifier);
        }
        return reifiers.toArray(new ProjectReifier[reifiers.size()]);
    }

    private Properties getContainerConfig(String name) throws IOException {
        final Path configPath = repository.resolve(String.format(CONTAINERS, name));
        if (!Files.exists(configPath)) {
            throw new IOException("Missing container config " + configPath);
        }
        final Properties config = ProfilesHelpers.readPropertiesFile(configPath);
        config.put(NAME_PROPERTY, name);
        return config;
    }
}
