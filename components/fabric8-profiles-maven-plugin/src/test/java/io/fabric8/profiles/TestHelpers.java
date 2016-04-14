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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

public class TestHelpers {

    static final public Path PROJECT_BASE_DIR;

    static {
        try {
            URL location = TestHelpers.class.getProtectionDomain().getCodeSource().getLocation();
            Path path = Paths.get(location.toURI());
            PROJECT_BASE_DIR = path.resolve("../..").normalize();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static String readTextFile(Path p) throws IOException {
        try(InputStream is = Files.newInputStream(p) ) {
            try(ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                copy(is, os);
                return new String(os.toByteArray(), StandardCharsets.UTF_8);
            }
        }
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte data[] = new byte[1024*4];
        int c;
        while( (c=is.read(data)) >= 0 ) {
            os.write(data, 0, c);
        }
    }

    /**
     * Create remote repos for containers
     * @param sourceDirectory   profile repo directory
     * @throws IOException      on error
     */
    public static void createTestRepos(Path sourceDirectory) throws IOException {
        Path remoteRoot = Paths.get("/tmp/remotes/");
        Files.createDirectories(remoteRoot);

        Files.list(sourceDirectory.resolve("configs/containers"))
            .filter( file -> file.getFileName().toString().endsWith(".cfg") )
            .forEach( path -> {
                String containerName = path.getFileName().toString();
                Path containerRepoPath = remoteRoot.resolve(containerName.substring(0, containerName.lastIndexOf('.')));

                if (!Files.isDirectory(containerRepoPath.resolve(".git"))) {
                    try {
                        Files.createDirectories(containerRepoPath);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                    try (Git ignored = initRepo(containerRepoPath)) {
                    } catch (GitAPIException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
            }
        );
    }

    public static Git initRepo(Path sourceDirectory) throws GitAPIException {
        Git sourceRepo = Git.init().setDirectory(sourceDirectory.toFile()).call();
        sourceRepo.add().addFilepattern(".").call();
        sourceRepo.commit().setMessage("Adding version 1.0").call();
        sourceRepo.branchRename().setNewName("1.0").call();
        return sourceRepo;
    }
}
