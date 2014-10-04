package io.fabric8.git;

import io.fabric8.api.GitContext;
import io.fabric8.git.internal.GitOperation;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.PushResult;

public interface GitDataStore {

    Git getGit();
    
    Iterable<PushResult> doPush(Git git, GitContext context) throws Exception;
    
    <T> T gitOperation(GitContext context, GitOperation<T> operation, PersonIdent personIdent);
}
