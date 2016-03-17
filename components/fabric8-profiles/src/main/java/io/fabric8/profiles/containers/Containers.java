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
import java.util.function.Predicate;

import io.fabric8.profiles.Profiles;
import io.fabric8.profiles.ProfilesHelpers;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

public class Containers {

    public static final String DEFAULT_CONTAINER_TYPE = "karaf";

    private static final String VERSION_PROPERTY = "version";
    private static final String NAME_PROPERTY = "name";

    private static final String CONTAINERS = "fabric/configs/containers/%s.cfg";
    private static final String VERSIONS = "fabric/configs/versions/%s/containers/%s";

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final Path repository;
    private Map<String, ProjectReifier> reifierMap;
    private String remoteUri;

    /**
     * @param repository container configuration directory, exported from ZK repository.
     * @param reifierMap map from container types to reifiers.
     * @param remoteUri  uri for remote profile git repo.
     */
    public Containers(Path repository, Map<String, ProjectReifier> reifierMap, String remoteUri) {
        this.repository = repository;
        this.reifierMap = reifierMap;
        this.remoteUri = remoteUri;
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

        // clone the Profiles repo and checkout required branch in a temp dir
        final Path tempRepo = Files.createTempDirectory(target, "profile-repo-");
        try (Git clonedRepo = new CloneCommand()
                .setURI(this.remoteUri)
                .setBranch(version)
                .setCloneAllBranches(false)
                .setDirectory(tempRepo.toFile())
                .call()) {

            // validate repo
            if (Files.list(tempRepo).filter(new Predicate<Path>() {
                @Override
                public boolean test(Path file) {
                    return !".git".matches(file.getFileName().toString());
                }
            }).count() == 0) {
                throw new IOException("Missing version branch " + version + " in profiles repo " + remoteUri);
            }

            // reify
            reifier.reify(target, config, new Profiles(tempRepo), containerProfiles);

        } catch (GitAPIException e) {
            throw new IOException("Error cloning Profile version " + version + ": " + e.getMessage(), e);
        } finally {
            ProfilesHelpers.deleteDirectory(tempRepo);
        }
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
