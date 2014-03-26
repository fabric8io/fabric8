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
package io.fabric8.git.internal;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bunch of helper methods for working with Git
 */
public class GitHelpers {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitHelpers.class);

    /**
     * Returns the root directory of the git repo which contains the ".git" directory
     */
    public static File getRootGitDirectory(Git git) {
        return git.getRepository().getDirectory().getParentFile();
    }

    public static boolean localBranchExists(Git git, String branch) throws GitAPIException {
        List<Ref> list = git.branchList().call();
        String fullName = "refs/heads/" + branch;
        boolean localBranchExists = false;
        for (Ref ref : list) {
            String name = ref.getName();
            if (equals(name, fullName)) {
                localBranchExists = true;
            }
        }
        return localBranchExists;
    }


    public static String currentBranch(Git git) {
        try {
            return git.getRepository().getBranch();
        } catch (IOException e) {
            LOG.warn("Failed to get the current branch: " + e, e);
            return null;
        }
    }


    public static boolean hasGitHead(Git git) throws GitAPIException, IOException {
        boolean hasHead = true;
        try {
            git.log().all().call();
            hasHead = git.getRepository().getAllRefs().containsKey("HEAD");
        } catch (NoHeadException e) {
            hasHead = false;
        }
        return hasHead;
    }

    public static void createOrCheckoutBranch(Git git, String branch, String remote) throws GitAPIException {
        Ref ref = null;
        String current = currentBranch(git);
        if (!equals(current, branch) && !localBranchExists(git, branch) ) {
            ref = git.checkout().setName(branch).setForce(true).setCreateBranch(true).call();
            configureBranch(git, branch, remote);
        } else {
            ref = git.checkout().setName(branch).setForce(true).call();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Checked out branch " + branch + " with results " + ref.getName());
        }
    }

    public static void checkoutBranch(Git git, String branch) throws GitAPIException {
        String current = currentBranch(git);
        if (equals(current, branch)) {
            return;
        } else if (localBranchExists(git, branch)) {
            Ref ref = git.checkout().setName(branch).setForce(true).call();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Checked out branch " + branch + " with results " + ref.getName());
            }
        } else {
            LOG.debug("Branch " + branch + "not found!");
        }
    }

    public static void removeBranch(Git git, String branch) throws GitAPIException {
        if (localBranchExists(git, branch)) {
            String current = currentBranch(git);
            if (equals(current, branch)) {
                // cannot remove current
                checkoutBranch(git, "master");
            }
            List<String> list = git.branchDelete().setBranchNames(branch).setForce(true).call();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Deleted branch " + branch + " with results " + list);
            }
        } else {
            LOG.debug("Branch " + branch + "not found!");
        }
    }

    protected static void configureBranch(Git git, String branch, String remote) {
        // lets update the merge config
        if (Strings.isNotBlank(branch)) {
            StoredConfig config = git.getRepository().getConfig();
            if (Strings.isNullOrBlank(config.getString("branch", branch, "remote")) || Strings.isNullOrBlank(
                    config.getString("branch", branch, "merge"))) {
                config.setString("branch", branch, "remote", remote);
                config.setString("branch", branch, "merge", "refs/heads/" + branch);
                try {
                    config.save();
                } catch (IOException e) {
                    LOG.error("Failed to configure the branch configuration to " + getRootGitDirectory(git)
                            + " with branch " + branch + " on remote repo: " + remote + ". " + e, e);
                }
            }
        }
    }

    static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
