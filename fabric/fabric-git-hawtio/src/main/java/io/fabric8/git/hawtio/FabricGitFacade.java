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
package io.fabric8.git.hawtio;

import static io.fabric8.git.internal.GitHelpers.getRootGitDirectory;
import io.fabric8.api.GitContext;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.Validatable;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.api.scr.ValidationSupport;
import io.fabric8.git.GitDataStore;
import io.fabric8.git.internal.GitHelpers;
import io.fabric8.git.internal.GitOperation;
import io.hawt.git.CommitInfo;
import io.hawt.git.CommitTreeInfo;
import io.hawt.git.FileContents;
import io.hawt.git.FileInfo;
import io.hawt.git.GitFacadeMXBean;
import io.hawt.git.GitFacadeSupport;
import io.hawt.util.Strings;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
import org.eclipse.jgit.util.Base64;

@ThreadSafe
@Component(name = "io.fabric8.git.hawtio", label = "Fabric8 Git Hawtio Service", immediate = true, metatype = false)
@Service(GitFacadeMXBean.class)
public final class FabricGitFacade extends GitFacadeSupport implements Validatable {

    @Reference(referenceInterface = GitDataStore.class)
    private final ValidatingReference<GitDataStore> gitDataStore = new ValidatingReference<GitDataStore>();

    private final ValidationSupport active = new ValidationSupport();

    @Activate
    void activate() throws Exception {
        super.init();
        active.setValid();
    }

    @Deactivate
    void deactivate() throws Exception {
        active.setInvalid();
        super.destroy();
    }

    @Override
    public boolean isValid() {
        return active.isValid();
    }

    @Override
    public void assertValid() {
        active.assertValid();
    }

    @Override
    public String getDefaultObjectName() {
        return "hawtio:type=GitFacade,repo=fabric";
    }

    @Override
    public String getContent(final String objectId, final String blobPath) {
        assertValid();
        return gitReadOperation(new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                return doGetContent(git, objectId, blobPath);
            }
        });
    }

    @Override
    public List<CommitTreeInfo> getCommitTree(final String commitId) {
        assertValid();
        return gitReadOperation(new GitOperation<List<CommitTreeInfo>>() {
            public List<CommitTreeInfo> call(Git git, GitContext context) throws Exception {
                return doGetCommitTree(git, commitId);
            }
        });
    }

    @Override
    public CommitInfo getCommitInfo(final String commitId) {
        assertValid();
        return gitReadOperation(new GitOperation<CommitInfo>() {
            public CommitInfo call(Git git, GitContext context) throws Exception {
                return doGetCommitInfo(git, commitId);
            }
        });
    }
    @Override
    public FileContents read(final String branch, final String pathOrEmpty) throws IOException, GitAPIException {
        assertValid();
        return gitReadOperation(new GitOperation<FileContents>() {
            public FileContents call(Git git, GitContext context) throws Exception {
                File rootDir = getRootGitDirectory(git);
                return doRead(git, rootDir, branch, pathOrEmpty);
            }
        });
    }

    @Override
    public FileInfo exists(final String branch, final String pathOrEmpty) throws IOException, GitAPIException {
        assertValid();
        return gitReadOperation(new GitOperation<FileInfo>() {
            public FileInfo call(Git git, GitContext context) throws Exception {
                File rootDir = getRootGitDirectory(git);
                return doExists(git, rootDir, branch, pathOrEmpty);
            }
        });
    }

    @Override
    public List<String> completePath(final String branch, final String completionText, final boolean directoriesOnly) {
        assertValid();
        return gitReadOperation(new GitOperation<List<String>>() {
            public List<String> call(Git git, GitContext context) throws Exception {
                File rootDir = getRootGitDirectory(git);
                return doCompletePath(git, rootDir, branch, completionText, directoriesOnly);
            }
        });
    }

    @Override
    public String readJsonChildContent(final String branch, final String path, String fileNameWildcardOrBlank, final String search) throws IOException {
        assertValid();
        final String fileNameWildcard = (Strings.isBlank(fileNameWildcardOrBlank)) ? "*.json" : fileNameWildcardOrBlank;
        return gitReadOperation(new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                File rootDir = getRootGitDirectory(git);
                return doReadJsonChildContent(git, rootDir, branch, path, fileNameWildcard, search);
            }
        });
    }

    @Override
    public CommitInfo write(final String branch, final String path, final String commitMessage, final String authorName, final String authorEmail, final String contents) {
        assertValid();
        final PersonIdent personIdent = new PersonIdent(authorName, authorEmail);
        return gitWriteOperation(personIdent, new GitOperation<CommitInfo>() {
            public CommitInfo call(Git git, GitContext context) throws Exception {
                checkoutBranch(git, branch);
                File rootDir = getRootGitDirectory(git);
                byte[] data = contents.getBytes();
                CommitInfo answer = doWrite(git, rootDir, branch, path, data, personIdent, commitMessage);
                context.commitMessage(commitMessage);
                return answer;
            }
        });
    }

    @Override
    public CommitInfo writeBase64(final String branch, final String path, final String commitMessage, final String authorName, final String authorEmail, final String contents) {
        assertValid();
        final PersonIdent personIdent = new PersonIdent(authorName, authorEmail);
        return gitWriteOperation(personIdent, new GitOperation<CommitInfo>() {
            public CommitInfo call(Git git, GitContext context) throws Exception {
                checkoutBranch(git, branch);
                File rootDir = getRootGitDirectory(git);
                byte[] data = Base64.decode(contents);
                CommitInfo answer = doWrite(git, rootDir, branch, path, data, personIdent, commitMessage);
                context.commitMessage(commitMessage);
                return answer;
            }
        });
    }

    @Override
    public void revertTo(final String branch, final String objectId, final String blobPath, final String commitMessage, final String authorName, final String authorEmail) {
        assertValid();
        final PersonIdent personIdent = new PersonIdent(authorName, authorEmail);
        gitWriteOperation(personIdent, new GitOperation<Void>() {
            public Void call(Git git, GitContext context) throws Exception {
                checkoutBranch(git, branch);
                File rootDir = getRootGitDirectory(git);
                Void answer = doRevert(git, rootDir, branch, objectId, blobPath, commitMessage, personIdent);
                context.commitMessage(commitMessage);
                return answer;
            }
        });
    }

    @Override
    public void rename(final String branch, final String oldPath, final String newPath, final String commitMessage, final String authorName, final String authorEmail) {
        assertValid();
        final PersonIdent personIdent = new PersonIdent(authorName, authorEmail);
        gitWriteOperation(personIdent, new GitOperation<RevCommit>() {
            public RevCommit call(Git git, GitContext context) throws Exception {
                checkoutBranch(git, branch);
                File rootDir = getRootGitDirectory(git);
                RevCommit answer = doRename(git, rootDir, branch, oldPath, newPath, commitMessage, personIdent);
                context.commitMessage(commitMessage);
                return answer;
            }
        });
    }

    @Override
    public void remove(final String branch, final String path, final String commitMessage, final String authorName, final String authorEmail) {
        assertValid();
        final PersonIdent personIdent = new PersonIdent(authorName, authorEmail);
        gitWriteOperation(personIdent, new GitOperation<RevCommit>() {
            public RevCommit call(Git git, GitContext context) throws Exception {
                checkoutBranch(git, branch);
                File rootDir = getRootGitDirectory(git);
                RevCommit answer = doRemove(git, rootDir, branch, path, commitMessage, personIdent);
                context.commitMessage(commitMessage);
                return answer;
            }
        });
    }

    @Override
    public void createBranch(final String fromBranch, final String newBranch) {
        gitWriteOperation(null, new GitOperation<Object>() {
            public Object call(Git git, GitContext context) throws Exception {
                doCreateBranch(git, fromBranch, newBranch);
                context.commitMessage("Created branch from " + fromBranch + " to " + newBranch);
                return null;
            }
        });
    }

    @Override
    public CommitInfo createDirectory(final String branch, final String path, final String commitMessage, final String authorName, final String authorEmail) {
        assertValid();
        final PersonIdent personIdent = new PersonIdent(authorName, authorEmail);
        return gitWriteOperation(personIdent, new GitOperation<CommitInfo>() {
            public CommitInfo call(Git git, GitContext context) throws Exception {
                checkoutBranch(git, branch);
                File rootDir = getRootGitDirectory(git);
                CommitInfo answer = doCreateDirectory(git, rootDir, branch, path, personIdent, commitMessage);
                context.commitMessage(commitMessage);
                return answer;
            }
        });
    }

    @Override
    public List<String> branches() {
        assertValid();
        return gitReadOperation(new GitOperation<List<String>>() {
            public List<String> call(Git git, GitContext context) throws Exception {
                return doListBranches(git);
            }
        });
    }

    @Override
    public String getHEAD() {
        assertValid();
        return gitReadOperation(new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                return doGetHead(git);
            }
        });
    }

    @Override
    public List<CommitInfo> history(final String branch, final String objectId, final String path, final int limit) {
        assertValid();
        return gitReadOperation(new GitOperation<List<CommitInfo>>() {
            public List<CommitInfo> call(Git git, GitContext context) throws Exception {
                return doHistory(git, branch, objectId, path, limit);
            }
        });
    }

    @Override
    public String diff(final String objectId, final String baseObjectId, final String path) {
        assertValid();
        return gitReadOperation(new GitOperation<String>() {
            public String call(Git git, GitContext context) throws Exception {
                return doDiff(git, objectId, baseObjectId, path);
            }
        });
    }

    @Override
    public boolean isPushOnCommit() {
        assertValid();
        return true;
    }

    @Override
    public Iterable<PushResult> doPush(Git git) throws Exception {
        assertValid();
        return gitDataStore.get().doPush(git, new GitContext());
    }

    @Override
    public void checkoutBranch(Git git, String branch) throws GitAPIException {
        assertValid();
        if (Strings.isBlank(branch)) {
            branch = "master";
        }
        GitHelpers.createOrCheckoutBranch(git, branch, "origin");
    }

    public void uploadFile(String p1, String p2, boolean p3, String p4, String p5) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private <T> T gitReadOperation(GitOperation<T> gitop) {
        GitContext context = new GitContext();
        return gitDataStore.get().gitOperation(context, gitop, null);
    }

    private <T> T gitWriteOperation(PersonIdent personIdent, GitOperation<T> gitop) {
        GitContext context = new GitContext().requireCommit().requirePush();
        return gitDataStore.get().gitOperation(context, gitop, personIdent);
    }

    // [FIXME] Test case polutes public API
    public void bindGitDataStoreForTesting(GitDataStore gitDataStore) {
        bindGitDataStore(gitDataStore);
    }

    // [FIXME] Test case polutes public API
    public void activateForTesting() throws Exception {
        activate();
    }

    void bindGitDataStore(GitDataStore gitDataStore) {
        this.gitDataStore.bind(gitDataStore);
    }

    void unbindGitDataStore(GitDataStore gitDataStore) {
        this.gitDataStore.unbind(gitDataStore);
    }
}
