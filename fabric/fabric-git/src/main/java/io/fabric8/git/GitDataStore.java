package io.fabric8.git;

import io.fabric8.api.GitContext;
import io.fabric8.git.internal.GitOperation;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.PushResult;

public interface GitDataStore {

    /**
     * Get the name of the remote repository
     */
    String getRemote();

    /**
     * Set the name of the remote repository
     */
    void setRemote(String remote);

    Iterable<PushResult> doPush(Git git, GitContext context) throws Exception;
    
    <T> T gitOperation(GitOperation<T> operation, GitContext context, PersonIdent personIdent);
}