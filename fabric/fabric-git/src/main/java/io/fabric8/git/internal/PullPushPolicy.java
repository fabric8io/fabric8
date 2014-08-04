package io.fabric8.git.internal;

import io.fabric8.api.GitContext;

import java.util.List;
import java.util.Set;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;

public interface PullPushPolicy {

    /**
     * Pull the version/profile state from the remote repository
     * @return true if the local workspace changed
     */
    boolean doPull(GitContext context, CredentialsProvider credentialsProvider, boolean doDeleteBranches, Set<String> versionCache);

    /**
     * Push the version/profile state to the remote repository
     * @return true if the local workspace changed
     */
    boolean doPush(GitContext context, CredentialsProvider credentialsProvider, List<PushResult> results) throws Exception;

}