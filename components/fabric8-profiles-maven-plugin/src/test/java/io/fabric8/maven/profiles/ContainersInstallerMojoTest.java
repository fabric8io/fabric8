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
package io.fabric8.maven.profiles;

import java.nio.file.Files;
import java.nio.file.Path;

import io.fabric8.profiles.ProfilesHelpers;
import io.fabric8.profiles.TestHelpers;

import org.codehaus.plexus.PlexusTestCase;
import org.eclipse.jgit.api.Git;
import org.junit.Test;

import static io.fabric8.profiles.TestHelpers.PROJECT_BASE_DIR;

/**
 * Test container repository update using Mojo.
 */
public class ContainersInstallerMojoTest extends PlexusTestCase {

    @Test
    public void testExecute() throws Exception {
        // set mojo parameters
        ContainersInstallerMojo installerMojo = new ContainersInstallerMojo();

        final Path target = PROJECT_BASE_DIR.resolve("target/generated-containers");
        ProfilesHelpers.deleteDirectory(target);
        Files.createDirectories(target);

        // create test git repo
        Path repoSource = PROJECT_BASE_DIR.resolve("target/it/it-repo");
        Path sourceDir = PROJECT_BASE_DIR.resolve("target/generated-repo");
        ProfilesHelpers.deleteDirectory(sourceDir);
        ProfilesHelpers.copyDirectory(repoSource, sourceDir);

        installerMojo.sourceDirectory = sourceDir.toFile();
        installerMojo.targetDirectory = target.toFile();

        try (Git ignored = TestHelpers.initRepo(sourceDir)) {

            TestHelpers.createTestRepos(repoSource);

            // generate containers
            ContainersGeneratorMojo generatorMojo = new ContainersGeneratorMojo();
            generatorMojo.sourceDirectory = installerMojo.sourceDirectory;
            generatorMojo.targetDirectory = installerMojo.targetDirectory;

            generatorMojo.execute();

            // push changes to remotes
            installerMojo.execute();
        }
    }

}