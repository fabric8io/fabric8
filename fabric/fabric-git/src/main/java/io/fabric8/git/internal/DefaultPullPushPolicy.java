/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.git.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.fabric8.api.DataStore;
import io.fabric8.api.GitContext;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CannotDeleteCurrentBranchException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.gitective.core.CommitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default {@link PullPushPolicy}.
 */
public final class DefaultPullPushPolicy implements PullPushPolicy  {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(DefaultPullPushPolicy.class);
    
    private final Git git;
    private final String remoteRef;
    private final int gitTimeout;
    
    private String lastFetchWarning;
    
    DefaultPullPushPolicy(Git git, String remoteRef, int gitTimeout) {
        this.git = git;
        this.remoteRef = remoteRef;
        this.gitTimeout = gitTimeout;
    }

    @Override
    public boolean doPull(GitContext context, CredentialsProvider credentialsProvider, boolean doDeleteBranches, Set<String> versionCache) {
        try {
            Repository repository = git.getRepository();
            StoredConfig config = repository.getConfig();
            String url = config.getString("remote", remoteRef, "url");
            if (url == null) {
                LOGGER.info("No remote repository defined, so not doing a pull");
                return false;
            }
            
            LOGGER.info("Performing a pull on remote URL: {}", url);
            
            try {
                git.fetch().setTimeout(gitTimeout).setCredentialsProvider(credentialsProvider).setRemote(remoteRef).call();
                lastFetchWarning = null;
            } catch (Exception ex) {
                String fetchWarning = ex.getMessage();
                if (!fetchWarning.equals(lastFetchWarning)) {
                    LOGGER.warn("Fetch failed because of: " + fetchWarning);
                    LOGGER.info("Fetch failed - the error will be ignored", ex);
                    lastFetchWarning = fetchWarning;
                }
                return false;
            }

            // Get local and remote branches
            Map<String, Ref> localBranches = new HashMap<String, Ref>();
            Map<String, Ref> remoteBranches = new HashMap<String, Ref>();
            Set<String> gitVersions = new HashSet<String>();
            for (Ref ref : git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()) {
                if (ref.getName().startsWith("refs/remotes/" + remoteRef + "/")) {
                    String name = ref.getName().substring(("refs/remotes/" + remoteRef + "/").length());
                    remoteBranches.put(name, ref);
                    gitVersions.add(name);
                } else if (ref.getName().startsWith("refs/heads/")) {
                    String name = ref.getName().substring(("refs/heads/").length());
                    localBranches.put(name, ref);
                    gitVersions.add(name);
                }
            }

            boolean hasChanged = false;
            
            // Check git commits
            if (!remoteBranches.isEmpty()) {
                for (String versionId : gitVersions) {
                    // Delete unneeded local branches.
                    if (!remoteBranches.containsKey(versionId)) {
                        // We never want to delete the master branch.
                        if (doDeleteBranches && !versionId.equals(GitHelpers.MASTER_BRANCH)) {
                            try {
                                git.branchDelete().setBranchNames(localBranches.get(versionId).getName()).setForce(true).call();
                            } catch (CannotDeleteCurrentBranchException ex) {
                                git.checkout().setName(GitHelpers.MASTER_BRANCH).setForce(true).call();
                                git.branchDelete().setBranchNames(localBranches.get(versionId).getName()).setForce(true).call();
                            }
                            versionCache.remove(versionId);
                            hasChanged = true;
                        }
                    } else if (!localBranches.containsKey(versionId)) {
                        // Create new local branches
                        versionCache.add(versionId);
                        git.checkout().setCreateBranch(true).setName(versionId).setStartPoint(remoteRef + "/" + versionId)
                                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).setForce(true).call();
                        hasChanged = true;
                    } else {
                        String localCommit = localBranches.get(versionId).getObjectId().getName();
                        String remoteCommit = remoteBranches.get(versionId).getObjectId().getName();
                        if (!localCommit.equals(remoteCommit)) {
                            git.clean().setCleanDirectories(true).call();
                            git.checkout().setName("HEAD").setForce(true).call();
                            git.checkout().setName(versionId).setForce(true).call();
                            MergeResult result = git.merge().setFastForward(FastForwardMode.FF_ONLY).include(remoteBranches.get(versionId).getObjectId()).call();
                            hasChanged = result.getMergeStatus() != MergeResult.MergeStatus.ALREADY_UP_TO_DATE && hasChanged(git, localCommit, remoteCommit);
                        }
                    }
                }
            }
            
            if (hasChanged) {
                LOGGER.info("Changed after pull!");
                LOGGER.debug("Called from ...", new RuntimeException());
                return true;
            } else {
                LOGGER.info("No change after pull!");
                return false;
            }
            
        } catch (Throwable ex) {
            LOGGER.debug("Failed to pull from the remote git repo " + GitHelpers.getRootGitDirectory(git), ex);
            LOGGER.warn("Failed to pull from the remote git repo " + GitHelpers.getRootGitDirectory(git) + " due " + ex.getMessage() + ". This exception is ignored.");
            return false;
        }
    }
    
    @Override
    public boolean doPush(GitContext context, CredentialsProvider credentialsProvider, List<PushResult> results) throws Exception {
        
        StoredConfig config = git.getRepository().getConfig();
        String remoteUrl = config.getString("remote", remoteRef, "url");
        if (remoteUrl == null) {
            LOGGER.info("No remote repository defined, so not doing a push");
            return false;
        }

        LOGGER.info("Pushing last change to: {}", remoteUrl);
        
        int retries = 5;
        Iterator<PushResult> resit = null;
        TransportException lastPushException = null;
        List<RemoteRefUpdate> rejectedUpdates = new ArrayList<>();
        while (resit == null && retries-- > 0) {
            try {
                resit = git.push().setTimeout(gitTimeout).setCredentialsProvider(credentialsProvider).setPushAll().call().iterator();
                lastPushException = null;
            } catch (TransportException ex) {
                lastPushException = ex;
                Thread.sleep(1000L);
            }
        }
        
        // Collect the updates that are not ok
        while (resit != null && resit.hasNext()) {
            PushResult pushResult = resit.next();
            if (results != null) {
                results.add(pushResult);
            }
            for (RemoteRefUpdate refUpdate : pushResult.getRemoteUpdates()) {
                LOGGER.info("Remote ref update: {}" + refUpdate);
                Status status = refUpdate.getStatus();
                if (status != Status.OK && status != Status.UP_TO_DATE) {
                    rejectedUpdates.add(refUpdate);
                }
            }
        }
        
        // Reset to the last known good rev and make the commit/push fail
        if (lastPushException != null || !rejectedUpdates.isEmpty()) {
            String checkoutId = context.getCheckoutId();
            if (checkoutId != null) {
                String branch = GitHelpers.currentBranch(git);
                RevCommit commit = CommitUtils.getCommit(git.getRepository(), checkoutId);
                LOGGER.warn("Resetting branch '{}' to: {}", branch, commit);
                git.reset().setMode(ResetType.HARD).setRef(checkoutId).call();
            }
            if (!rejectedUpdates.isEmpty()) {
                throw new IllegalStateException("Cannot fast forward, remote repository has already changed.");
            } else {
                throw new IllegalStateException("Cannot push last profile update.", lastPushException);
            }
        }
        
        return false;
    }

    /**
     * Checks if there is an actual difference between two commits.
     * In some cases a container may push a commit, without actually modifying anything.
     * So comparing the commit hashes is not always enough. We need to actually diff the two commits.
     *
     * @param git    The {@link Git} instance to use.
     * @param before The hash of the first commit.
     * @param after  The hash of the second commit.
     */
    private boolean hasChanged(Git git, String before, String after) throws IOException, GitAPIException {
        if (isCommitEqual(before, after)) {
            return false;
        }
        Repository db = git.getRepository();
        List<DiffEntry> entries = git.diff().setOldTree(getTreeIterator(db, before)).setNewTree(getTreeIterator(db, after)).call();
        return entries.size() > 0;
    }

    private AbstractTreeIterator getTreeIterator(Repository db, String name) throws IOException {
        final ObjectId id = db.resolve(name);
        if (id == null)
            throw new IllegalArgumentException(name);
        final CanonicalTreeParser p = new CanonicalTreeParser();
        final ObjectReader or = db.newObjectReader();
        try {
            p.reset(or, new RevWalk(db).parseTree(id));
            return p;
        } finally {
            or.release();
        }
    }

    private static boolean isCommitEqual(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}