/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.openshift.agent;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class for working with a git repo for a Fabric managed OpenShift cartridge
 */
public class CartridgeGitRepository {
    private static final transient Logger LOG = LoggerFactory.getLogger(CartridgeGitRepository.class);

    public static final String DEFAULT_GIT_PATH = File.separator + "git" + File.separator
            + "fabric-openshift";
    public static final String DEFAULT_LOCAL_LOCATION = System.getProperty("karaf.data") + DEFAULT_GIT_PATH;

    private final File localRepo;

    private String remoteName = "origin";
    private Git git;


    public CartridgeGitRepository(String containerId) {
        localRepo = new File(DEFAULT_LOCAL_LOCATION + File.separator + containerId);
    }


    /**
     * Clones or pulls the remote repository and returns the directory with the checkout
     */
    public void cloneOrPull(final String repo, final CredentialsProvider credentials) throws Exception {
        if (!localRepo.exists() && !localRepo.mkdirs()) {
            throw new IOException("Failed to create local repository");
        }
        File gitDir = new File(localRepo, ".git");
        if (!gitDir.exists()) {
            LOG.info("Cloning remote repo " + repo);
            CloneCommand command = Git.cloneRepository().setCredentialsProvider(credentials).
                    setURI(repo).setDirectory(localRepo).setRemote(remoteName);
            git = command.call();
        } else {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder.setGitDir(gitDir)
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();

            git = new Git(repository);


            // update the remote repo just in case
            StoredConfig config = repository.getConfig();
            config.setString("remote", remoteName, "url", repo);
            config.setString("remote", remoteName, "fetch",
                    "+refs/heads/*:refs/remotes/" + remoteName + "/*");

            String branch = "master";
            config.setString("branch", branch, "remote", remoteName);
            config.setString("branch", branch, "merge", "refs/heads/" + branch);

            try {
                config.save();
            } catch (IOException e) {
                LOG.error(
                        "Failed to save the git configuration to " + localRepo + " with remote repo: "
                                + repo
                                + ". " + e, e);
            }

            // now pull
            LOG.info("Pulling from remote repo " + repo);
            git.pull().setCredentialsProvider(credentials).setRebase(true).call();
        }
    }

    public File getLocalRepo() {
        return localRepo;
    }

    public Git getGit() throws IOException {
        return git;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }
}
