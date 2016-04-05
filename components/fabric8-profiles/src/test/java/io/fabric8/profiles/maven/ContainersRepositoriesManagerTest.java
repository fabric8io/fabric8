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
package io.fabric8.profiles.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.fabric8.profiles.ProfilesHelpers;

import org.codehaus.plexus.PlexusTestCase;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;

import static io.fabric8.profiles.TestHelpers.PROJECT_BASE_DIR;

/**
 * Test container repository update using Mojo.
 */
public class ContainersRepositoriesManagerTest extends PlexusTestCase {

    @Test
    public void testExecute() throws Exception {
        // set mojo parameters
        ContainersRepositoriesManager manager = new ContainersRepositoriesManager();

        final Path target = PROJECT_BASE_DIR.resolve("target/generated-containers");
        ProfilesHelpers.deleteDirectory(target);
        Files.createDirectories(target);

        // create test git repo
        Path repoSource = PROJECT_BASE_DIR.resolve("src/test/it-repo");
        Path sourceDir = PROJECT_BASE_DIR.resolve("target/generated-repo");
        ProfilesHelpers.copyDirectory(repoSource, sourceDir);

        manager.sourceDirectory = sourceDir.toFile();
        manager.targetDirectory = target.toFile();
        try (Git sourceRepo = Git.init().setDirectory(manager.sourceDirectory).call()) {

            // create branch, add source and commit
            initRepo(sourceRepo);

            // generate containers
            ContainersGenerator generator = new ContainersGenerator();
            generator.sourceDirectory = manager.sourceDirectory;
            generator.targetDirectory = manager.targetDirectory;

            generator.execute();

            // create remote repos for containers
            Path remoteRoot = Paths.get("/tmp/remotes/");
            Files.list(generator.targetDirectory.toPath()).forEach(path -> {
                    Path containerName = path.getFileName();
                    Path containerRepoPath = remoteRoot.resolve(containerName);

                    if (!Files.isDirectory(containerRepoPath.resolve(".git"))) {
                        try {
                            Files.createDirectories(containerRepoPath);
                        } catch (IOException e) {
                            throw new IllegalArgumentException(e);
                        }
                        try (Git repo = Git.init().setDirectory(containerRepoPath.toFile()).call()) {
                            initRepo(repo);
                        } catch (GitAPIException e) {
                            throw new IllegalArgumentException(e);
                        }
                    }
                }
            );

            // TODO add git config properties
            // TODO setup test git remote for root container

            // push changes to remotes
            manager.execute();
        }
    }

    private void initRepo(Git sourceRepo) throws GitAPIException {
        sourceRepo.add().addFilepattern(".").call();
        sourceRepo.commit().setMessage("Adding version 1.0").call();
        sourceRepo.branchRename().setNewName("1.0").call();
    }
}