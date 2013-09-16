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
package org.fusesource.fabric.git.hawtio;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.internal.Objects;
import org.fusesource.fabric.service.support.InvalidComponentException;
import org.fusesource.fabric.service.support.Validatable;
import org.fusesource.fabric.git.internal.GitContext;
import org.fusesource.fabric.git.internal.GitDataStore;
import org.fusesource.fabric.git.internal.GitHelpers;
import org.fusesource.fabric.git.internal.GitOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.hawt.git.CommitInfo;
import io.hawt.git.FileContents;
import io.hawt.git.FileInfo;
import io.hawt.git.GitFacadeMXBean;
import io.hawt.git.GitFacadeSupport;
import io.hawt.util.Strings;
import static org.fusesource.fabric.git.internal.GitHelpers.getRootGitDirectory;

/**
 */
@Component(name = "org.fusesource.fabric.git.hawtio", description = "Fabric Git Hawtio Service", immediate = true)
@Service(GitFacadeMXBean.class)
public class FabricGitFacade extends GitFacadeSupport implements Validatable {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricGitFacade.class);

    @Reference
    private DataStore dataStore;
    private GitDataStore gitDataStore;

    private final AtomicBoolean active = new AtomicBoolean();

    @Override
    public synchronized boolean isValid() {
        return active.get();
    }

    @Override
    public synchronized void assertValid() {
        if (isValid() == false)
            throw new InvalidComponentException();
    }

    @Activate
    public synchronized void init() throws Exception {
        active.set(true);
        try {
            if (gitDataStore == null) {
                Objects.notNull(dataStore, "dataStore");
                if (dataStore instanceof GitDataStore) {
                    setGitDataStore((GitDataStore) dataStore);
                }
            }
            Objects.notNull(gitDataStore, "gitDataStore");
            super.init();
        } catch (Exception ex) {
            active.set(false);
            throw ex;
        }
    }

    @Deactivate
    public synchronized void destroy() throws Exception {
        try {
            super.destroy();
        } finally {
            active.set(false);
        }
    }

    protected String getDefaultObjectName() {
        return "io.hawt.git:type=GitFacade,repo=fabric";
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public void setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
        if (dataStore instanceof GitDataStore) {
            setGitDataStore((GitDataStore) dataStore);
        } else if (dataStore != null) {
            LOG.warn("DataStore is not a GitDataStore " + dataStore + " expected " + GitDataStore.class + " but had " + dataStore.getClass());
        }
    }

    public GitDataStore getGitDataStore() {
        return gitDataStore;
    }

    public void setGitDataStore(GitDataStore gitDataStore) {
        this.gitDataStore = gitDataStore;
    }

    @Override
    public String getContent(final String objectId, final String blobPath) {
        return gitReadOperation(new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                return doGetContent(git, objectId, blobPath);
            }
        });
    }

    public FileContents read(final String branch, final String pathOrEmpty) throws IOException, GitAPIException {
        return gitReadOperation(new GitOperation<FileContents>() {
            public FileContents call(Git git, GitContext context) throws Exception {
                File rootDir = getRootGitDirectory(git);
                return doRead(git, rootDir, branch, pathOrEmpty);
            }
        });
    }

    public FileInfo exists(final String branch, final String pathOrEmpty) throws IOException, GitAPIException {
        return gitReadOperation(new GitOperation<FileInfo>() {
            public FileInfo call(Git git, GitContext context) throws Exception {
                File rootDir = getRootGitDirectory(git);
                return doExists(git, rootDir, branch, pathOrEmpty);
            }
        });
    }

    public List<String> completePath(final String branch, final String completionText, final boolean directoriesOnly) {
        return gitReadOperation(new GitOperation<List<String>>() {
            public List<String> call(Git git, GitContext context) throws Exception {
                File rootDir = getRootGitDirectory(git);
                return doCompletePath(git, rootDir, branch, completionText, directoriesOnly);
            }
        });
    }

    @Override
    public String readJsonChildContent(final String branch, final String path, String fileNameWildcardOrBlank,
                                       final String search) throws IOException {
        final String fileNameWildcard = (Strings.isBlank(fileNameWildcardOrBlank)) ? "*.json"
                : fileNameWildcardOrBlank;
        return gitReadOperation(new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                File rootDir = getRootGitDirectory(git);
                return doReadJsonChildContent(git, rootDir, branch, path, fileNameWildcard, search);
            }
        });
    }


    public CommitInfo write(final String branch, final String path, final String commitMessage,
                      final String authorName, final String authorEmail, final String contents) {
        final PersonIdent personIdent = new PersonIdent(authorName, authorEmail);
        return gitWriteOperation(personIdent, new GitOperation<CommitInfo>() {
            public CommitInfo call(Git git, GitContext context) throws Exception {
                checkoutBranch(git, branch);
                File rootDir = getRootGitDirectory(git);
                return doWrite(git, rootDir, branch, path, contents, personIdent, commitMessage);
            }
        });
    }


    @Override
    public void revertTo(final String branch, final String objectId, final String blobPath, final String commitMessage,
                         final String authorName, final String authorEmail) {
        final PersonIdent personIdent = new PersonIdent(authorName, authorEmail);
        gitWriteOperation(personIdent, new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                File rootDir = getRootGitDirectory(git);
                return doRevert(git, rootDir, branch, objectId, blobPath, commitMessage, personIdent);
            }
        });
    }

    public void rename(final String branch, final String oldPath, final String newPath, final String commitMessage,
                       final String authorName, final String authorEmail) {
        final PersonIdent personIdent = new PersonIdent(authorName, authorEmail);
        gitWriteOperation(personIdent, new GitOperation<RevCommit>() {
            public RevCommit call(Git git, GitContext context) throws Exception {
                File rootDir = getRootGitDirectory(git);
                return doRename(git, rootDir, branch, oldPath, newPath, commitMessage, personIdent);
            }
        });
    }

    public void remove(final String branch, final String path, final String commitMessage,
                       final String authorName, final String authorEmail) {
        final PersonIdent personIdent = new PersonIdent(authorName, authorEmail);
        gitWriteOperation(personIdent, new GitOperation<RevCommit>() {
            public RevCommit call(Git git, GitContext context) throws Exception {
                File rootDir = getRootGitDirectory(git);
                return doRemove(git, rootDir, branch, path, commitMessage, personIdent);
            }
        });
    }

    @Override
    public CommitInfo createDirectory(final String branch, final String path, final String commitMessage,
                                     final String authorName, final String authorEmail) {
        final PersonIdent personIdent = new PersonIdent(authorName, authorEmail);
        return gitWriteOperation(personIdent, new GitOperation<CommitInfo>() {
            public CommitInfo call(Git git, GitContext context) throws Exception {
                checkoutBranch(git, branch);
                File rootDir = getRootGitDirectory(git);
                return doCreateDirectory(git, rootDir, branch, path, personIdent, commitMessage);
            }
        });
    }

    public List<String> branches() {
        return gitReadOperation(new GitOperation<List<String>>() {
            public List<String> call(Git git, GitContext context) throws Exception {
                return doListBranches(git);
            }
        });
    }

    public String getHEAD() {
        return gitReadOperation(new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                return doGetHead(git);
            }
        });
    }

    public List<CommitInfo> history(final String branch, final String objectId, final String path, final int limit) {
        return gitReadOperation(new GitOperation<List<CommitInfo>>() {
            public List<CommitInfo> call(Git git, GitContext context) throws Exception {
                return doHistory(git, branch, objectId, path, limit);
            }
        });
    }

    @Override
    public String diff(final String objectId, final String baseObjectId, final String path) {
        return gitReadOperation(new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                return doDiff(git, objectId, baseObjectId, path);
            }
        });
    }


    protected boolean isPushOnCommit() {
        return true;
    }

    protected Iterable<PushResult> doPush(Git git) throws Exception {
        return gitDataStore.doPush(git, null);
    }

    protected <T> T gitReadOperation(GitOperation<T> operation) {
        return gitDataStore.gitReadOperation(operation);
    }

    protected <T> T gitWriteOperation(PersonIdent personIdent,
                              GitOperation<T> operation) {
        GitContext context = new GitContext();
        context.requireCommit();
        return gitDataStore.gitOperation(personIdent, operation, true, context);
    }

    protected <T> T gitOperation(PersonIdent personIdent,
                              GitOperation<T> operation, boolean pullFirst) {
        return gitDataStore.gitOperation(personIdent, operation, pullFirst);
    }

    protected <T> T gitOperation(GitOperation<T> operation) {
        return gitDataStore.gitOperation(operation);
    }

    protected void checkoutBranch(Git git, String branch) throws GitAPIException {
        if (Strings.isBlank(branch)) {
            branch = "master";
        }
        GitHelpers.checkoutBranch(git, branch, gitDataStore.getRemote());
    }
}
