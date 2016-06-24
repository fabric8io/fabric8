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
package io.fabric8.profiles;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

import static io.fabric8.profiles.ProfilesHelpers.readJsonFile;
import static io.fabric8.profiles.ProfilesHelpers.readPropertiesFile;
import static io.fabric8.profiles.ProfilesHelpers.readYamlFile;
import static io.fabric8.profiles.ProfilesHelpers.recusivelyCollectFileListing;
import static io.fabric8.profiles.ProfilesHelpers.toBytes;
import static io.fabric8.profiles.ProfilesHelpers.toJsonBytes;
import static io.fabric8.profiles.ProfilesHelpers.toYamlBytes;

public class Profiles {

    private final Path repository;

    /**
     * @param repository directory should be a repository containing profile configurations.
     */
    public Profiles(Path repository) {
        this.repository = repository;
    }

    /**
     * @param target       is the directory where resulting materialized profile configuration will be written to.
     * @param profileNames a list of profile names that will be combined to create the materialized profile.
     */
    public void materialize(Path target, String... profileNames) throws IOException {
        ArrayList<String> profileSearchOrder = new ArrayList<>();
        for (String profileName : profileNames) {
            collectProfileNames(profileSearchOrder, profileName);
        }

        HashSet<String> files = new HashSet<>();
        for (String profileName : profileSearchOrder) {
            files.addAll(listFiles(profileName));
        }

        System.out.println("profile search order" + profileSearchOrder);
        System.out.println("files: " + files);
        for (String file : files) {
            try (InputStream is = materializeFile(file, profileSearchOrder)) {
                Files.copy(is, target.resolve(file), StandardCopyOption.REPLACE_EXISTING);
            }
        }

    }

    private InputStream materializeFile(String fileName, ArrayList<String> profileSearchOrder) throws IOException {
        if (fileName.endsWith(".properties")) {

            // later property files in the profile overwrite values
            // in previous properties.
            Properties properties = new Properties();
            for (String profile : profileSearchOrder) {
                Path path = getProfilePath(profile).resolve(fileName);
                if (Files.exists(path)) {
                    ProfilesHelpers.merge(properties, readPropertiesFile(path));
                }
            }
            return new ByteArrayInputStream(toBytes(properties));
        } else if (fileName.endsWith(".json")) {
            JsonNode node = null;
            for (String profile : profileSearchOrder) {
                Path path = getProfilePath(profile).resolve(fileName);
                if (Files.exists(path)) {
                    node = ProfilesHelpers.merge(node, readJsonFile(path));
                }
            }
            return new ByteArrayInputStream(toJsonBytes(node));
        } else if (fileName.endsWith(".yml")) {
            JsonNode node = null;
            for (String profile : profileSearchOrder) {
                Path path = getProfilePath(profile).resolve(fileName);
                if (Files.exists(path)) {
                    node = ProfilesHelpers.merge(node, readYamlFile(path));
                }
            }
            return new ByteArrayInputStream(toYamlBytes(node));
        } else {
            // Last profile in list wins, since we cant merge these types of files.
            Path last = null;
            for (String profile : profileSearchOrder) {
                Path path = getProfilePath(profile).resolve(fileName);
                if (Files.exists(path)) {
                    last = path;
                }
            }
            return Files.newInputStream(last);
        }
    }

    private ArrayList<String> listFiles(String profileName) throws IOException {
        ArrayList<String> rc = new ArrayList<>();
        Path dir = getProfilePath(profileName);
        recusivelyCollectFileListing(rc, dir, dir);
        return rc;
    }

    private void collectProfileNames(ArrayList<String> target, String profileName) throws IOException {
        if (target.contains(profileName)) {
            return;
        }

        Path path = getProfilePath(profileName);
        if (!Files.exists(path)) {
            throw new IOException("Profile directory does not exists: " + path);
        }
        Properties props = new Properties();
        Path agentProperties = path.resolve("io.fabric8.agent.properties");
        if (Files.exists(agentProperties)) {
            props = readPropertiesFile(agentProperties);
        }

        String parents = props.getProperty("attribute.parents", "default".equals(profileName) ? "" : "default");
        for (String parent : parents.split(",")) {
            parent = parent.trim();
            if (!parent.isEmpty()) {
                collectProfileNames(target, parent);
            }
        }

        target.add(profileName);
    }


    private Path getProfilePath(String profileName) {
        return repository.resolve(profileName.replaceAll("-", "/") + ".profile");
    }


}