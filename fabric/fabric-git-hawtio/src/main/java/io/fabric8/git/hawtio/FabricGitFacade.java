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

import io.fabric8.api.GitContext;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.Validatable;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.api.scr.ValidationSupport;
import io.fabric8.git.GitDataStore;
import io.fabric8.git.internal.GitHelpers;
import io.fabric8.git.internal.GitOperation;
import io.hawt.git.*;
import io.hawt.util.Files;
import io.hawt.util.Function;
import io.hawt.util.Strings;
import org.apache.felix.scr.annotations.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import static io.fabric8.git.internal.GitHelpers.getRootGitDirectory;
import static io.hawt.git.GitHelper.doUploadFiles;

@ThreadSafe
@Component(name = "io.fabric8.git.hawtio", label = "Fabric8 Git Hawtio Service", immediate = true, metatype = false)
@Service({GitFacadeMXBean.class, GitFileManager.class})
public final class FabricGitFacade extends GitFacadeSupport implements Validatable {

    private static final Logger LOG = LoggerFactory.getLogger(FabricGitFacade.class);

    private final Object lock = new Object();

    private PersonIdent stashPersonIdent;

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
        GitHelpers.checkoutBranch(git, branch);
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

    /**
     * Uploads the given local file name to the given branch and path; unzipping any zips if the flag is true
     */
    @Override
    public void uploadFile(String branch, String path, boolean unzip, String sourceFileName, String destName) throws IOException, GitAPIException {
        Map<String, File> uploadedFiles = new HashMap<>();
        File sourceFile = new File(sourceFileName);
        if (!sourceFile.exists()) {
            throw new IllegalArgumentException("Source file does not exist: " + sourceFile);
        }
        uploadedFiles.put(destName, sourceFile);
        uploadFiles(branch, path, unzip, uploadedFiles);
    }

    /**
     * Uploads a list of files to the given branch and path
     */
    public void uploadFiles(String branch, String path, final boolean unzip, final Map<String, File> uploadFiles) throws IOException, GitAPIException {
        LOG.info("uploadFiles: branch: " + branch + " path: " + path + " unzip: " + unzip + " uploadFiles: " + uploadFiles);

        WriteCallback<Object> callback = new WriteCallback<Object>() {
            @Override
            public Object apply(WriteContext context) throws IOException, GitAPIException {
                File folder = context.getFile();
                // lets copy the files into the folder so we can add them to git
                List<File> copiedFiles = new ArrayList<>();
                Set<Map.Entry<String, File>> entries = uploadFiles.entrySet();
                for (Map.Entry<String, File> entry : entries) {
                    String name = entry.getKey();
                    File uploadFile = entry.getValue();
                    File copiedFile = new File(folder, name);
                    Files.copy(uploadFile, copiedFile);
                    copiedFiles.add(copiedFile);
                }
                doUploadFiles(context, folder, unzip, copiedFiles);
                return null;
            }
        };
        writeFile(branch, path, callback);
    }

    @Override
    public <T> T readFile(final String branch, final String pathOrEmpty, final Function<File,T> callback) throws IOException, GitAPIException {
        return gitOperation(getStashPersonIdent(), new Callable<T>() {
            @Override
            public String toString() {
                return "doReadFile(" + branch + ", " + pathOrEmpty + ", " + callback + ")";
            }

            @Override
            public T call() throws Exception {
                Git git = gitDataStore.get().getGit();
                return doReadFile(git, getRootGitDirectory(git), branch, pathOrEmpty, callback);
            }
        });
    }

    @Override
    public <T> T writeFile(final String branch, final String pathOrEmpty, final WriteCallback<T> callback) throws IOException, GitAPIException {
        return gitOperation(getStashPersonIdent(), new Callable<T>() {
            @Override
            public String toString() {
                return "doWriteFile(" + branch + ", " + pathOrEmpty + ", " + callback + ")";
            }

            @Override
            public T call() throws Exception {
                Git git = gitDataStore.get().getGit();
                return doWriteFile(git, getRootGitDirectory(git), branch, pathOrEmpty, callback);
            }
        });
    }

    public PersonIdent getStashPersonIdent() {
        if (stashPersonIdent == null) {
            stashPersonIdent = new PersonIdent("dummy", "dummy");
        }
        return stashPersonIdent;
    }

    /**
     * Performs the given operations on a clean git repository
     */
    protected <T> T gitOperation(PersonIdent personIdent, Callable<T> callable) {
        synchronized (lock) {
            try {
                T answer = callable.call();
                return answer;
            } catch (Exception e) {
                throw new RuntimeIOException(e);
            }
        }
    }
}
