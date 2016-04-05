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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Properties;

import io.fabric8.profiles.Profiles;
import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.containers.karaf.KarafProjectReifier;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.junit.Test;

import static io.fabric8.profiles.ProfilesHelpers.deleteDirectory;
import static io.fabric8.profiles.TestHelpers.PROJECT_BASE_DIR;

/**
 * Test Containers.
 */
public class ContainersTest {

    @Test
    public void testReify() throws Exception {
        // temp karaf project output dir
        Path target = PROJECT_BASE_DIR.resolve("target/test-data/karaf1");
        deleteDirectory(target);
        Files.createDirectories(target);

        // temp profile git repo
        final Path repository = PROJECT_BASE_DIR.resolve("target/test-data/it-repo");
        final Path profilesRoot = repository.resolve("profiles");
        final Path configsRoot = repository.resolve("configs");
        deleteDirectory(repository);
        Files.createDirectories(profilesRoot);
        Files.createDirectories(configsRoot);

        // copy integration test repository
        ProfilesHelpers.copyDirectory(PROJECT_BASE_DIR.resolve("src/test/it-repo"), repository);

        try (final Git profileRepo = new InitCommand().setDirectory(repository.toFile()).call()) {
            profileRepo.add().addFilepattern(".").call();
            profileRepo.commit().setMessage("Adding version 1.0").call();
            profileRepo.branchRename().setNewName("1.0").call();

            final Properties karafDefaults = new Properties();
            karafDefaults.put("groupId", "io.fabric8.karaf-swarm");
            karafDefaults.put("description", "Karaf Swarm container");

            final HashMap<String, ProjectReifier> reifierMap = new HashMap<>();
            reifierMap.put(KarafProjectReifier.CONTAINER_TYPE, new KarafProjectReifier(karafDefaults));
            reifierMap.put(JenkinsfileReifier.CONTAINER_TYPE, new JenkinsfileReifier(karafDefaults));

            final Containers containers = new Containers(configsRoot, reifierMap, new Profiles(profilesRoot));
            containers.reify(target, "root");
        }
    }

}