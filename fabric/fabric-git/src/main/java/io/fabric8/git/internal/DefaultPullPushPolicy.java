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

import io.fabric8.api.GitContext;
import io.fabric8.api.visibility.VisibleForExternal;
import io.fabric8.git.PullPushPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import io.fabric8.api.gravia.IllegalStateAssertion;
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

    @VisibleForExternal
    public DefaultPullPushPolicy(Git git, String remoteRef, int gitTimeout) {
        this.git = git;
        this.remoteRef = remoteRef;
        this.gitTimeout = gitTimeout;
    }

    @Override
    public synchronized PullPolicyResult doPull(GitContext context, CredentialsProvider credentialsProvider, boolean allowVersionDelete) {
        Repository repository = git.getRepository();
        StoredConfig config = repository.getConfig();
        String remoteUrl = config.getString("remote", remoteRef, "url");
        if (remoteUrl == null) {
            LOGGER.debug("No remote repository defined, so not doing a pull");
            return new AbstractPullPolicyResult();
        }
        
        LOGGER.info("Performing a pull on remote URL: {}", remoteUrl);
        
        Exception lastException = null;
        try {
            git.fetch().setTimeout(gitTimeout).setCredentialsProvider(credentialsProvider).setRemote(remoteRef).call();
        } catch (GitAPIException | JGitInternalException ex) {
            lastException = ex;
        }

        // No meaningful processing after GitAPIException
        if (lastException != null) {
            LOGGER.warn("Pull failed because of: {}", lastException.toString());
            return new AbstractPullPolicyResult(lastException);
        }
        
        // Get local and remote branches
        Map<String, Ref> localBranches = new HashMap<String, Ref>();
        Map<String, Ref> remoteBranches = new HashMap<String, Ref>();
        Set<String> allBranches = new HashSet<String>();
        try {
            for (Ref ref : git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()) {
                if (ref.getName().startsWith("refs/remotes/" + remoteRef + "/")) {
                    String name = ref.getName().substring(("refs/remotes/" + remoteRef + "/").length());
                    remoteBranches.put(name, ref);
                    allBranches.add(name);
                } else if (ref.getName().startsWith("refs/heads/")) {
                    String name = ref.getName().substring(("refs/heads/").length());
                    localBranches.put(name, ref);
                    allBranches.add(name);
                }
            }
            
            boolean localUpdate = false;
            boolean remoteUpdate = false;
            Set<String> versions = new TreeSet<>();
            
            // Remote repository has no branches, force a push
            if (remoteBranches.isEmpty()) {
                LOGGER.debug("Pulled from an empty remote repository");
                return new AbstractPullPolicyResult(versions, false, !localBranches.isEmpty(), null);
            } else {
                LOGGER.debug("Processing remote branches: {}", remoteBranches);
            }
            
            // Verify master branch and do a checkout of it when we have it locally (already)
            IllegalStateAssertion.assertTrue(remoteBranches.containsKey(GitHelpers.MASTER_BRANCH), "Remote repository does not have a master branch");
            if (localBranches.containsKey(GitHelpers.MASTER_BRANCH)) {
                git.checkout().setName(GitHelpers.MASTER_BRANCH).setForce(true).call();
            }
            
            // Iterate over all local/remote branches
            for (String branch : allBranches) {
                
                // Delete a local branch that does not exist remotely, but not master 
                boolean allowDelete = allowVersionDelete && !GitHelpers.MASTER_BRANCH.equals(branch);
                if (localBranches.containsKey(branch) && !remoteBranches.containsKey(branch)) {
                    if (allowDelete) {
                        LOGGER.debug("Deleting local branch: {}", branch);
                        git.branchDelete().setBranchNames(branch).setForce(true).call();
                        localUpdate = true;
                    } else {
                        remoteUpdate = true;
                    }
                } 
                
                // Create a local branch that exists remotely
                else if (!localBranches.containsKey(branch) && remoteBranches.containsKey(branch)) {
                    LOGGER.debug("Adding local branch: {}", branch);
                    git.checkout().setCreateBranch(true).setName(branch).setStartPoint(remoteRef + "/" + branch).setUpstreamMode(SetupUpstreamMode.TRACK).setForce(true).call();
                    versions.add(branch);
                    localUpdate = true;
                }
                
                // Update a local branch that also exists remotely
                else if (localBranches.containsKey(branch) && remoteBranches.containsKey(branch)) {
                    ObjectId localObjectId = localBranches.get(branch).getObjectId();
                    ObjectId remoteObjectId = remoteBranches.get(branch).getObjectId();
                    String localCommit = localObjectId.getName();
                    String remoteCommit = remoteObjectId.getName();
                    if (!localCommit.equals(remoteCommit)) {
                        git.clean().setCleanDirectories(true).call();
                        git.checkout().setName("HEAD").setForce(true).call();
                        git.checkout().setName(branch).setForce(true).call();
                        MergeResult mergeResult = git.merge().setFastForward(FastForwardMode.FF_ONLY).include(remoteObjectId).call();
                        MergeStatus mergeStatus = mergeResult.getMergeStatus();
                        LOGGER.debug("Updating local branch {} with status: {}", branch, mergeStatus);
                        if (mergeStatus == MergeStatus.FAST_FORWARD) {
                            localUpdate = true;
                        } else if (mergeStatus == MergeStatus.ALREADY_UP_TO_DATE) {
                            remoteUpdate = true;
                        } else if (mergeStatus == MergeStatus.ABORTED) {
                            LOGGER.debug("Cannot fast forward branch {}, attempting rebase", branch);
                            RebaseResult rebaseResult = git.rebase().setUpstream(remoteCommit).call();
                            RebaseResult.Status rebaseStatus = rebaseResult.getStatus();
                            if (rebaseStatus == RebaseResult.Status.OK) {
                                localUpdate = true;
                                remoteUpdate = true;
                            } else {
                                LOGGER.warn("Rebase on branch {} failed, restoring remote branch", branch);
                                git.rebase().setOperation(Operation.ABORT).call();
                                git.checkout().setName(GitHelpers.MASTER_BRANCH).setForce(true).call();
                                git.branchDelete().setBranchNames(branch).setForce(true).call();
                                git.checkout().setCreateBranch(true).setName(branch).setStartPoint(remoteRef + "/" + branch).setUpstreamMode(SetupUpstreamMode.TRACK).setForce(true).call();
                                localUpdate = true;
                            }
                        }
                    }
                    versions.add(branch);
                }
            }


            PullPolicyResult result = new AbstractPullPolicyResult(versions, localUpdate, remoteUpdate, null);
            LOGGER.info("Pull result: {}", result);
            return result;
        } catch (Exception ex) {
            return new AbstractPullPolicyResult(ex);
        }
    }
    
    @Override
    public synchronized PushPolicyResult doPush(GitContext context, CredentialsProvider credentialsProvider) {
        
        StoredConfig config = git.getRepository().getConfig();
        String remoteUrl = config.getString("remote", remoteRef, "url");
        if (remoteUrl == null) {
            LOGGER.debug("No remote repository defined, so not doing a push");
            return new AbstractPushPolicyResult();
        }

        LOGGER.info("Pushing last change to: {}", remoteUrl);
        
        Iterator<PushResult> resit = null;
        Exception lastException = null;
        try {
            resit = git.push().setTimeout(gitTimeout).setCredentialsProvider(credentialsProvider).setPushAll().call().iterator();
        } catch (GitAPIException | JGitInternalException ex) {
            lastException = ex;
        }
        
        // Allow the commit to stay in the repository in case of push failure
        if (lastException != null) {
            LOGGER.warn("Cannot push because of: {}", lastException.toString());
            return new AbstractPushPolicyResult(lastException);
        }
        
        List<PushResult> pushResults = new ArrayList<>();
        List<RemoteRefUpdate> acceptedUpdates = new ArrayList<>();
        List<RemoteRefUpdate> rejectedUpdates = new ArrayList<>();
        
        // Collect the updates that are not ok
        while (resit.hasNext()) {
            PushResult pushResult = resit.next();
            pushResults.add(pushResult);
            for (RemoteRefUpdate refUpdate : pushResult.getRemoteUpdates()) {
                Status status = refUpdate.getStatus();
                if (status == Status.OK || status == Status.UP_TO_DATE) {
                    acceptedUpdates.add(refUpdate);
                } else {
                    rejectedUpdates.add(refUpdate);
                }
            }
        }
        
        // Reset to the last known good rev and make the commit/push fail
        for (RemoteRefUpdate rejectedRef : rejectedUpdates) {
            LOGGER.warn("Rejected push: {}" + rejectedRef);
            String refName = rejectedRef.getRemoteName();
            String branch = refName.substring(refName.lastIndexOf('/') + 1);
            try {
                GitHelpers.checkoutBranch(git, branch);
                FetchResult fetchResult = git.fetch().setTimeout(gitTimeout).setCredentialsProvider(credentialsProvider).setRemote(remoteRef).setRefSpecs(new RefSpec("refs/heads/" + branch)).call();
                Ref fetchRef = fetchResult.getAdvertisedRef("refs/heads/" + branch);
                git.branchRename().setOldName(branch).setNewName(branch + "-tmp").call();
                git.checkout().setCreateBranch(true).setName(branch).setStartPoint(fetchRef.getObjectId().getName()).call();
                git.branchDelete().setBranchNames(branch + "-tmp").setForce(true).call();
            } catch (GitAPIException ex) {
                LOGGER.warn("Cannot reset branch {}, because of: {}", branch, ex.toString());
            }
        }
        
        PushPolicyResult result = new AbstractPushPolicyResult(pushResults, acceptedUpdates, rejectedUpdates, lastException);
        LOGGER.info("Push result: {}", result);
        return result;
    }

    static class AbstractPullPolicyResult implements PullPolicyResult {

        private final Set<String> versions = new TreeSet<>();
        private final boolean localUpdate;
        private final boolean remoteUpdate;
        private final Exception lastException;
        
        AbstractPullPolicyResult() {
            this(Collections.<String>emptySet(), false, false, null);
        }

        AbstractPullPolicyResult(Exception lastException) {
            this(Collections.<String>emptySet(), false, false, lastException);
        }

        AbstractPullPolicyResult(Set<String> versions, boolean localUpdate, boolean remoteUpdate, Exception lastException) {
            this.versions.addAll(versions);
            this.localUpdate = localUpdate;
            this.remoteUpdate = remoteUpdate;
            this.lastException = lastException;
        }

        @Override
        public boolean localUpdateRequired() {
            return localUpdate;
        }

        @Override
        public boolean remoteUpdateRequired() {
            return remoteUpdate;
        }

        @Override
        public Set<String> getVersions() {
            return Collections.unmodifiableSet(versions);
        }

        @Override
        public Exception getLastException() {
            return lastException;
        }

        @Override
        public String toString() {
            return "[localUpdate=" + localUpdate + ",remoteUpdate=" + remoteUpdate + ",versions=" + versions + ",error=" + lastException + "]";
        }
    }

    static class AbstractPushPolicyResult implements PushPolicyResult {

        private final List<PushResult> pushResults = new ArrayList<>();
        private final List<RemoteRefUpdate> acceptedUpdates = new ArrayList<>();
        private final List<RemoteRefUpdate> rejectedUpdates = new ArrayList<>();
        private final Exception lastException;
        
        AbstractPushPolicyResult() {
            this(Collections.<PushResult>emptyList(), Collections.<RemoteRefUpdate>emptyList(), Collections.<RemoteRefUpdate>emptyList(), null);
        }

        AbstractPushPolicyResult(Exception lastException) {
            this(Collections.<PushResult>emptyList(), Collections.<RemoteRefUpdate>emptyList(), Collections.<RemoteRefUpdate>emptyList(), lastException);
        }

        AbstractPushPolicyResult(List<PushResult> pushResults, List<RemoteRefUpdate> acceptedUpdates, List<RemoteRefUpdate> rejectedUpdates, Exception lastException) {
            this.pushResults.addAll(pushResults);
            this.acceptedUpdates.addAll(acceptedUpdates);
            this.rejectedUpdates.addAll(rejectedUpdates);
            this.lastException = lastException;
        }

        @Override
        public List<PushResult> getPushResults() {
            return Collections.unmodifiableList(pushResults);
        }

        @Override
        public List<RemoteRefUpdate> getAcceptedUpdates() {
            return Collections.unmodifiableList(acceptedUpdates);
        }

        @Override
        public List<RemoteRefUpdate> getRejectedUpdates() {
            return Collections.unmodifiableList(rejectedUpdates);
        }

        @Override
        public Exception getLastException() {
            return lastException;
        }

        @Override
        public String toString() {
            return "[accepted=" + acceptedUpdates.size() + ",rejected=" + rejectedUpdates.size() + ",error=" + lastException + "]";
        }
    }
}
