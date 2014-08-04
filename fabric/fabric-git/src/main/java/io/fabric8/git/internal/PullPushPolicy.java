package io.fabric8.git.internal;

import io.fabric8.api.GitContext;

import java.util.List;
import java.util.Set;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;

public interface PullPushPolicy {

    interface PullPolicyResult {
        
        boolean localUpdateRequired();
        
        boolean remoteUpdateRequired();
        
        Set<String> getVersions();
        
        Exception getLastException();
    }

    interface PushPolicyResult {
    
        List<PushResult> getPushResults();
        
        List<RemoteRefUpdate> getAcceptedUpdates();
        
        List<RemoteRefUpdate> getRejectedUpdates();
        
        Exception getLastException();
    }
    
    /**
     * Pull the version/profile state from the remote repository
     */
    PullPolicyResult doPull(GitContext context, CredentialsProvider credentialsProvider);

    /**
     * Push the version/profile state to the remote repository
     */
    PushPolicyResult doPush(GitContext context, CredentialsProvider credentialsProvider);

}