/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.rest.git;

import io.fabric8.forge.rest.git.dto.CommitInfo;
import io.fabric8.forge.rest.git.dto.CommitTreeInfo;
import io.fabric8.forge.rest.git.dto.FileDTO;
import io.fabric8.forge.rest.git.dto.StatusDTO;
import io.fabric8.forge.rest.main.GitHelpers;
import io.fabric8.forge.rest.main.ProjectFileSystem;
import io.fabric8.forge.rest.main.UserDetails;
import io.fabric8.utils.Files;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Strings;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.gitective.core.BlobUtils;
import org.gitective.core.CommitFinder;
import org.gitective.core.CommitUtils;
import org.gitective.core.PathFilterUtils;
import org.gitective.core.filter.commit.CommitLimitFilter;
import org.gitective.core.filter.commit.CommitListFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import static io.fabric8.forge.rest.main.GitHelpers.configureCommand;
import static io.fabric8.forge.rest.main.GitHelpers.disableSslCertificateChecks;
import static io.fabric8.forge.rest.main.GitHelpers.doAddCommitAndPushFiles;

/**
 */
public class RepositoryResource {
    private static final transient Logger LOG = LoggerFactory.getLogger(RepositoryResource.class);

    private final File gitFolder;
    private final File basedir;
    private final UserDetails userDetails;
    private final String remoteRepository;
    private final GitLockManager lockManager;
    private final ProjectFileSystem projectFileSystem;
    private final String origin;
    private final String cloneUrl;
    private final String branch;
    private final PersonIdent personIdent;
    private String message;

    public RepositoryResource(File basedir, File gitFolder, UserDetails userDetails, String origin, String branch, String remoteRepository, GitLockManager lockManager, ProjectFileSystem projectFileSystem, String cloneUrl) throws IOException, GitAPIException {
        this.basedir = basedir;
        this.gitFolder = gitFolder;
        this.userDetails = userDetails;
        this.remoteRepository = remoteRepository;
        this.lockManager = lockManager;
        this.projectFileSystem = projectFileSystem;
        this.origin = origin;
        this.cloneUrl = cloneUrl;
        String user = userDetails.getUser();
        String authorEmail = userDetails.getEmail();
        this.personIdent = new PersonIdent(user, authorEmail);
        this.branch = branch;
    }

    protected static String getFilePattern(String path) {
        return trimLeadingSlash(path);
    }

    public static String trimLeadingSlash(String name) {
        if (name != null && name.startsWith("/")) {
            name = name.substring(1);
        }
        return name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    @GET
    @Path("content/{path:.*}")
    public Response fileDetails(final @PathParam("path") String path) throws Exception {
        return gitReadOperation(new GitOperation<Response>() {
            @Override
            public Response call(Git git, GitContext context) throws Exception {
                return doFileDetails(path);
            }
        });
    }

    protected Response doFileDetails(String path) {
        final File file = getRelativeFile(path);
        if (LOG.isDebugEnabled()) {
            LOG.debug("reading file: " + file.getPath());
        }
        if (!file.exists() || file.isDirectory()) {
            List<FileDTO> answer = new ArrayList<>();
            if (file.exists()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File child : files) {
                        FileDTO dto = createFileDTO(child, false);
                        if (dto != null) {
                            answer.add(dto);
                        }
                    }
                }
            }
            return Response.ok(answer).build();
        } else {
            FileDTO answer = createFileDTO(file, true);
            return Response.ok(answer).build();
        }
    }

    @GET
    @Path("raw/{path:.*}")
    public Response rawFile(final @PathParam("path") String path) throws Exception {
        return gitReadOperation(new GitOperation<Response>() {
            @Override
            public Response call(Git git, GitContext context) throws Exception {
                return doRawFile(path);
            }
        });
    }

    protected Response doRawFile(String path) throws IOException {
        final File file = getRelativeFile(path);
        if (LOG.isDebugEnabled()) {
            LOG.debug("reading file: " + file.getPath());
        }
        if (file.isDirectory()) {
            // TODO return a listing?
            Object directoryDto = null;
            return Response.ok(directoryDto).build();
        } else {
            byte[] data = Files.readBytes(file);
            return Response.ok(data).build();
        }
    }

    @POST
    @Path("content/{path:.*}")
    @Consumes("*/*")
    public Response postFile(@PathParam("path") String path, @QueryParam("message") String message, final InputStream body) throws Exception {
        return uploadFile(path, message, body);
    }

    @POST
    @Path("content/{path:.*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response postFileForm(@PathParam("path") String path, @FormParam("message") String message, @FormParam("file") String body) throws Exception {
        byte[] bytes = body.getBytes();
        return uploadFile(path, message, new ByteArrayInputStream(bytes));
    }

    @GET
    @Path("diff/{objectId1}")
    public String diff(@PathParam("objectId1") String objectId) throws Exception {
        return diff(objectId, null, null);
    }

    @GET
    @Path("diff/{objectId1}/{objectId2}")
    public String diff(@PathParam("objectId1") String objectId, @PathParam("objectId2") String baseObjectId) throws Exception {
        return diff(objectId, baseObjectId, null);
    }

    @GET
    @Path("diff/{objectId1}/{objectId2}/{path:.*}")
    public String diff(final @PathParam("objectId1") String objectId, final @PathParam("objectId2") String baseObjectId, final @PathParam("path") String pathOrBlobPath) throws Exception {
        return gitReadOperation(new GitOperation<String>() {
            @Override
            public String call(Git git, GitContext context) throws Exception {
                return doDiff(git, objectId, baseObjectId, pathOrBlobPath);
            }
        });
    }

    protected String doDiff(Git git, String objectId, String baseObjectId, String pathOrBlobPath) throws IOException {
        Repository r = git.getRepository();
        String blobPath = trimLeadingSlash(pathOrBlobPath);

        RevCommit commit;
        if (Strings.isNotBlank(objectId)) {
            commit = CommitUtils.getCommit(r, objectId);
        } else {
            commit = CommitUtils.getHead(r);
        }
        RevCommit baseCommit = null;
        if (Strings.isNotBlank(baseObjectId)) {
            baseCommit = CommitUtils.getCommit(r, baseObjectId);
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        RawTextComparator cmp = RawTextComparator.DEFAULT;
        DiffFormatter formatter = new DiffFormatter(buffer);
        formatter.setRepository(r);
        formatter.setDiffComparator(cmp);
        formatter.setDetectRenames(true);

        RevTree commitTree = commit.getTree();
        RevTree baseTree;
        if (baseCommit == null) {
            if (commit.getParentCount() > 0) {
                final RevWalk rw = new RevWalk(r);
                RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
                rw.dispose();
                baseTree = parent.getTree();
            } else {
                // FIXME initial commit. no parent?!
                baseTree = commitTree;
            }
        } else {
            baseTree = baseCommit.getTree();
        }

        List<DiffEntry> diffEntries = formatter.scan(baseTree, commitTree);
        if (blobPath != null && blobPath.length() > 0) {
            for (DiffEntry diffEntry : diffEntries) {
                if (diffEntry.getNewPath().equalsIgnoreCase(blobPath)) {
                    formatter.format(diffEntry);
                    break;
                }
            }
        } else {
            formatter.format(diffEntries);
        }
        formatter.flush();
        return buffer.toString();
    }

    @GET
    @Path("commitInfo/{commitId}")
    public CommitInfo commitInfo(final @PathParam("commitId") String commitId) throws Exception {
        return gitReadOperation(new GitOperation<CommitInfo>() {
            @Override
            public CommitInfo call(Git git, GitContext context) throws Exception {
                return doCommitInfo(git, commitId);
            }
        });
    }

    protected CommitInfo doCommitInfo(Git git, String commitId) {
        Repository repository = git.getRepository();
        RevCommit commit = CommitUtils.getCommit(repository, commitId);
        if (commit == null) {
            return null;
        } else {
            return createCommitInfo(commit);
        }
    }

    /**
     * Returns the file changes in a commit
     */
    @GET
    @Path("commitTree/{commitId}")
    public List<CommitTreeInfo> getCommitTree(final @PathParam("commitId") String commitId) throws Exception {
        return gitReadOperation(new GitOperation<List<CommitTreeInfo>>() {
            @Override
            public List<CommitTreeInfo> call(Git git, GitContext context) throws Exception {
                return doGetCommitTree(git, commitId);
            }
        });
    }

    protected List<CommitTreeInfo> doGetCommitTree(Git git, String commitId) {
        Repository repository = git.getRepository();
        List<CommitTreeInfo> list = new ArrayList<CommitTreeInfo>();
        RevCommit commit = CommitUtils.getCommit(repository, commitId);
        if (commit != null) {
            RevWalk rw = new RevWalk(repository);
            try {
                if (commit.getParentCount() == 0) {
                    TreeWalk treeWalk = new TreeWalk(repository);
                    treeWalk.reset();
                    treeWalk.setRecursive(true);
                    treeWalk.addTree(commit.getTree());
                    while (treeWalk.next()) {
                        String pathString = treeWalk.getPathString();
                        ObjectId objectId = treeWalk.getObjectId(0);
                        int rawMode = treeWalk.getRawMode(0);
                        list.add(new CommitTreeInfo(pathString, pathString, 0, rawMode, objectId.getName(), commit.getId().getName(),
                                DiffEntry.ChangeType.ADD));
                    }
                    treeWalk.release();
                } else {
                    RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
                    DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
                    df.setRepository(repository);
                    df.setDiffComparator(RawTextComparator.DEFAULT);
                    df.setDetectRenames(true);
                    List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
                    for (DiffEntry diff : diffs) {
                        String objectId = diff.getNewId().name();
                        if (diff.getChangeType().equals(DiffEntry.ChangeType.DELETE)) {
                            list.add(new CommitTreeInfo(diff.getOldPath(), diff.getOldPath(), 0, diff
                                    .getNewMode().getBits(), objectId, commit.getId().getName(), diff
                                    .getChangeType()));
                        } else if (diff.getChangeType().equals(DiffEntry.ChangeType.RENAME)) {
                            list.add(new CommitTreeInfo(diff.getOldPath(), diff.getNewPath(), 0, diff
                                    .getNewMode().getBits(), objectId, commit.getId().getName(), diff
                                    .getChangeType()));
                        } else {
                            list.add(new CommitTreeInfo(diff.getNewPath(), diff.getNewPath(), 0, diff
                                    .getNewMode().getBits(), objectId, commit.getId().getName(), diff
                                    .getChangeType()));
                        }
                    }
                }
            } catch (Throwable e) {
                LOG.warn("Failed to walk tree for commit " + commitId + ". " + e, e);
            } finally {
                rw.dispose();
            }
        }
        return list;
    }

    @GET
    @Path("history")
    public List<CommitInfo> history(@QueryParam("limit") int limit) throws Exception {
        return history(null, null, limit);
    }

    @GET
    @Path("history/{commitId}/{path:.*}")
    public List<CommitInfo> history(@PathParam("commitId") final String objectId, @PathParam("path") final String pathOrBlobPath, @QueryParam("limit") final int limit) throws Exception {
        return gitReadOperation(new GitOperation<List<CommitInfo>>() {
            @Override
            public List<CommitInfo> call(Git git, GitContext context) throws Exception {
                return doHistory(git, objectId, pathOrBlobPath, limit);
            }
        });
    }

    protected List<CommitInfo> doHistory(Git git, String objectId, String pathOrBlobPath, int limit) {
        List<CommitInfo> results = new ArrayList<CommitInfo>();
        Repository r = git.getRepository();

        try {
            String head = getHEAD(git);
        } catch (Exception e) {
            LOG.error("Cannot find HEAD of this git repository! " + e, e);
            return results;
        }

        String path = trimLeadingSlash(pathOrBlobPath);

        CommitFinder finder = new CommitFinder(r);
        CommitListFilter filter = new CommitListFilter();
        if (Strings.isNotBlank(path)) {
            finder.setFilter(PathFilterUtils.and(path));
        }
        finder.setFilter(filter);

        if (limit > 0) {
            finder.setFilter(new CommitLimitFilter(limit).setStop(true));
        }
        if (Strings.isNotBlank(objectId)) {
            finder.findFrom(objectId);
        } else {
            if (Strings.isNotBlank(branch)) {
                ObjectId branchObjectId = getBranchObjectId(git);
                if (branchObjectId != null) {
                    finder = finder.findFrom(branchObjectId);
                } else {
                    finder = finder.findInBranches();
                }

            } else {
                finder.find();
            }
        }
        List<RevCommit> commits = filter.getCommits();
        for (RevCommit entry : commits) {
            CommitInfo commitInfo = createCommitInfo(entry);
            results.add(commitInfo);
        }
        return results;
    }

    @POST
    @Path("mkdir/{path:.*}")
    public CommitInfo createDirectory(@PathParam("commitId") final String path) throws Exception {
        return gitWriteOperation(new GitOperation<CommitInfo>() {
            @Override
            public CommitInfo call(Git git, GitContext context) throws Exception {
                return doCreateDirectory(git, path);
            }
        });
    }

    protected CommitInfo doCreateDirectory(Git git, String path) throws Exception {
        File file = getRelativeFile(path);
        if (file.exists()) {
            return null;
        }
        file.mkdirs();
        String filePattern = getFilePattern(path);
        AddCommand add = git.add().addFilepattern(filePattern).addFilepattern(".");
        add.call();

        CommitCommand commit = git.commit().setAll(true).setAuthor(personIdent).setMessage(message);
        RevCommit revCommit = commitThenPush(git, commit);
        return createCommitInfo(revCommit);
    }

    @POST
    @Path("revert/{commitId}/{path:.*}")
    public CommitInfo revert(@PathParam("commitId") final String objectId, @PathParam("path") final String blobPath) throws Exception {
        return gitWriteOperation(new GitOperation<CommitInfo>() {
            @Override
            public CommitInfo call(Git git, GitContext context) throws Exception {
                return doRevert(git, objectId, blobPath);
            }
        });
    }

    protected CommitInfo doRevert(Git git, String objectId, String blobPath) throws Exception {
        String contents = doGetContent(git, objectId, blobPath);
        if (contents != null) {
            return doWrite(git, blobPath, contents.getBytes(), personIdent, message);
        } else {
            return null;
        }
    }

    @POST
    @Path("mv/{path:.*}")
    public CommitInfo rename(@QueryParam("old") final String oldPath, @PathParam("path") final String newPath) throws Exception {
        return gitWriteOperation(new GitOperation<CommitInfo>() {
            @Override
            public CommitInfo call(Git git, GitContext context) throws Exception {
                return doRename(git, oldPath, newPath);
            }
        });
    }

    protected CommitInfo doRename(Git git, String oldPath, String newPath) throws Exception {
        File file = getRelativeFile(oldPath);
        File newFile = getRelativeFile(newPath);
        if (file.exists()) {
            File parentFile = newFile.getParentFile();
            parentFile.mkdirs();
            if (!parentFile.exists()) {
                throw new IOException("Could not create directory " + parentFile + " when trying to move " + file + " to " + newFile + ". Maybe a file permission issue?");
            }
            file.renameTo(newFile);
            String filePattern = getFilePattern(newPath);
            git.add().addFilepattern(filePattern).call();
            CommitCommand commit = git.commit().setAll(true).setAuthor(personIdent).setMessage(message);
            return createCommitInfo(commitThenPush(git, commit));
        } else {
            return null;
        }
    }

    @POST
    @Path("rm")
    @Consumes({"application/xml", "application/json", "text/json"})
    public CommitInfo remove(final List<String> paths) throws Exception {
        return gitWriteOperation(new GitOperation<CommitInfo>() {
            @Override
            public CommitInfo call(Git git, GitContext context) throws Exception {
                return doRemove(git, paths);
            }
        });
    }

    protected CommitInfo doRemove(Git git, List<String> paths) throws Exception {
        if (paths != null && paths.size() > 0) {
            int count = 0;
            for (String path : paths) {
                File file = getRelativeFile(path);
                if (file.exists()) {
                    count++;
                    Files.recursiveDelete(file);
                    String filePattern = getFilePattern(path);
                    git.rm().addFilepattern(filePattern).call();
                }
            }
            if (count > 0) {
                CommitCommand commit = git.commit().setAll(true).setAuthor(personIdent).setMessage(message);
                return createCommitInfo(commitThenPush(git, commit));
            }
        }
        return null;
    }

    @POST
    @Path("rm/{path:.*}")
    public CommitInfo remove(@PathParam("path") final String path) throws Exception {
        return gitWriteOperation(new GitOperation<CommitInfo>() {
            @Override
            public CommitInfo call(Git git, GitContext context) throws Exception {
                return doRemove(git, path);
            }
        });
    }

    @POST
    @Path("removeProject")
    public Response remove() throws Exception {
        return lockManager.withLock(gitFolder, new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                LOG.info("Removing clone of project at " + basedir);
                Files.recursiveDelete(basedir);
                return Response.ok(new StatusDTO(basedir.getName(), "remove project")).build();
            }
        });
    }

    protected CommitInfo doRemove(Git git, String path) throws Exception {
        File file = getRelativeFile(path);
        if (file.exists()) {
            Files.recursiveDelete(file);
            String filePattern = getFilePattern(path);
            git.rm().addFilepattern(filePattern).call();
            CommitCommand commit = git.commit().setAll(true).setAuthor(personIdent).setMessage(message);
            return createCommitInfo(commitThenPush(git, commit));
        } else {
            return null;
        }
    }

    @GET
    @Path("listBranches")
    public List<String> listBranches() throws Exception {
        return gitReadOperation(new GitOperation<List<String>>() {
            @Override
            public List<String> call(Git git, GitContext context) throws Exception {
                return doListBranches(git);
            }
        });
    }

    protected List<String> doListBranches(Git git) throws Exception {
        SortedSet<String> names = new TreeSet<String>();
        List<Ref> call = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        for (Ref ref : call) {
            String name = ref.getName();
            int idx = name.lastIndexOf('/');
            if (idx >= 0) {
                name = name.substring(idx + 1);
            }
            if (name.length() > 0) {
                names.add(name);
            }
        }
        return new ArrayList<String>(names);
    }

    public <T> T gitReadOperation(GitOperation<T> operation) throws Exception {
        GitContext context = new GitContext();
        return gitOperation(context, operation);
    }

    public <T> T gitWriteOperation(GitOperation<T> operation) throws Exception {
        GitContext context = new GitContext();
        context.setRequireCommit(true);
        context.setRequirePush(true);
        return gitOperation(context, operation);
    }

    protected <T> T gitOperation(final GitContext context, final GitOperation<T> operation) throws Exception {
        return lockManager.withLock(gitFolder, new Callable<T>() {

            @Override
            public T call() throws Exception {
                projectFileSystem.cloneRepoIfNotExist(userDetails, basedir, cloneUrl);

                FileRepositoryBuilder builder = new FileRepositoryBuilder();
                Repository repository = builder.setGitDir(gitFolder)
                        .readEnvironment() // scan environment GIT_* variables
                        .findGitDir() // scan up the file system tree
                        .build();

                Git git = new Git(repository);
                if (Strings.isNullOrBlank(origin)) {
                    throw new IOException("Could not find remote git URL for folder " + gitFolder.getPath());
                }

                CredentialsProvider credentials = userDetails.createCredentialsProvider();

                disableSslCertificateChecks();
                LOG.info("Stashing local changes to the repo");
                boolean hasHead = true;
                try {
                    git.log().all().call();
                    hasHead = git.getRepository().getAllRefs().containsKey("HEAD");
                } catch (NoHeadException e) {
                    hasHead = false;
                }
                if (hasHead) {
                    // lets stash any local changes just in case..
                    try {
                        git.stashCreate().setPerson(personIdent).setWorkingDirectoryMessage("Stash before a write").setRef("HEAD").call();
                    } catch (Throwable e) {
                        LOG.error("Failed to stash changes: " + e, e);
                        Throwable cause = e.getCause();
                        if (cause != null && cause != e) {
                            LOG.error("Cause: " + cause, cause);
                        }
                    }
                }

                checkoutBranch(git);
                if (context.isRequirePull()) {
                    doPull(git, context);
                }

                T result = operation.call(git, context);

                if (Strings.isNullOrBlank(message)) {
                    message = "";
                }
                if (context.isRequireCommit()) {
                    doAddCommitAndPushFiles(git, userDetails, personIdent, branch, origin, message, isPushOnCommit());
                }
                return result;
            }
        });
    }

    protected void doPull(Git git, GitContext context) throws GitAPIException {
        LOG.info("Performing a pull in git repository " + this.gitFolder + " on remote URL: " + this.remoteRepository);
        CredentialsProvider cp = userDetails.createCredentialsProvider();
        PullCommand command = git.pull();
        configureCommand(command, userDetails);
        command.setCredentialsProvider(cp).setRebase(true).call();
    }

    protected Response uploadFile(final String path, final String message, final InputStream body) throws Exception {
        return gitWriteOperation(new GitOperation<Response>() {
            @Override
            public Response call(Git git, GitContext context) throws Exception {
                return doUploadFile(path, message, body);
            }
        });
    }

    protected Response doUploadFile(final String path, String message, final InputStream body) throws Exception {
        this.message = message;
        final File file = getRelativeFile(path);

        boolean exists = file.exists();
        if (LOG.isDebugEnabled()) {
            LOG.debug("writing file: " + file.getPath());
        }
        file.getParentFile().mkdirs();
        IOHelpers.writeTo(file, body);
        String status = exists ? "updated" : "created";
        return Response.ok(new StatusDTO(path, status)).build();
    }

    protected CommitInfo doWrite(Git git, String path, byte[] contents, PersonIdent personIdent, String commitMessage) throws Exception {
        File file = getRelativeFile(path);
        file.getParentFile().mkdirs();

        Files.writeToFile(file, contents);

        String filePattern = getFilePattern(path);
        AddCommand add = git.add().addFilepattern(filePattern).addFilepattern(".");
        add.call();

        CommitCommand commit = git.commit().setAll(true).setAuthor(personIdent).setMessage(commitMessage);
        RevCommit revCommit = commitThenPush(git, commit);
        return createCommitInfo(revCommit);
    }

    protected RevCommit commitThenPush(Git git, CommitCommand commit) throws Exception {
        RevCommit answer = commit.call();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Committed " + answer.getId() + " " + answer.getFullMessage());
        }
        if (isPushOnCommit()) {
            Iterable<PushResult> results = doPush(git);
            for (PushResult result : results) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Pushed " + result.getMessages() + " " + result.getURI() + " branch: " + branch + " updates: " + toString(result.getRemoteUpdates()));
                }
            }
        }
        return answer;
    }

    protected File getRelativeFile(String path) {
        return new File(basedir, trimLeadingSlash(path));
    }

    protected boolean isPushOnCommit() {
        return true;
    }

    public CommitInfo createCommitInfo(RevCommit entry) {
        final Date date = GitHelpers.getCommitDate(entry);
        String author = entry.getAuthorIdent().getName();
        boolean merge = entry.getParentCount() > 1;
        String shortMessage = entry.getShortMessage();
        String sha = entry.getName();
        return new CommitInfo(sha, author, date, merge, shortMessage);
    }

    protected String getHEAD(Git git) {
        RevCommit commit = CommitUtils.getHead(git.getRepository());
        return commit.getName();
    }

    protected ObjectId getBranchObjectId(Git git) {
        Ref branchRef = null;
        try {
            String branchRevName = "refs/heads/" + branch;
            List<Ref> branches = git.branchList().call();
            for (Ref ref : branches) {
                String revName = ref.getName();
                if (Objects.equals(branchRevName, revName)) {
                    branchRef = ref;
                    break;
                }
            }
        } catch (GitAPIException e) {
            LOG.warn("Failed to find branches " + e, e);
        }

        ObjectId branchObjectId = null;
        if (branchRef != null) {
            branchObjectId = branchRef.getObjectId();
        }
        return branchObjectId;
    }

    public String currentBranch(Git git) {
        try {
            return git.getRepository().getBranch();
        } catch (IOException e) {
            LOG.warn("Failed to get the current branch due: " + e.getMessage() + ". This exception is ignored.", e);
            return null;
        }
    }

    protected void checkoutBranch(Git git) throws GitAPIException {
        String current = currentBranch(git);
        if (Objects.equals(current, branch)) {
            return;
        }
        // lets check if the branch exists
        CheckoutCommand command = git.checkout().setName(branch);
        boolean exists = localBranchExists(git, branch);
        if (!exists) {
            command = command.setCreateBranch(true).setForce(true).
                    setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                    setStartPoint(getRemote() + "/" + branch);
        }
        Ref ref = command.call();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Checked out branch " + branch + " with results " + ref.getName());
        }
        configureBranch(git, branch);
    }


    protected String doGetContent(Git git, String objectId, String pathOrBlobPath) {
        objectId = defaultObjectId(git, objectId);
        Repository r = git.getRepository();
        String blobPath = trimLeadingSlash(pathOrBlobPath);
        return BlobUtils.getContent(r, objectId, blobPath);
    }

    protected String defaultObjectId(Git git, String objectId) {
        if (objectId == null || objectId.trim().length() == 0) {
            RevCommit commit = CommitUtils.getHead(git.getRepository());
            objectId = commit.getName();
        }
        return objectId;
    }

    protected void configureBranch(Git git, String branch) {
        // lets update the merge config
        if (Strings.isNotBlank(branch)) {
            StoredConfig config = git.getRepository().getConfig();
            if (Strings.isNullOrBlank(config.getString("branch", branch, "remote")) || Strings.isNullOrBlank(config.getString("branch", branch, "merge"))) {
                config.setString("branch", branch, "remote", getRemote());
                config.setString("branch", branch, "merge", "refs/heads/" + branch);
                try {
                    config.save();
                } catch (IOException e) {
                    LOG.error("Failed to save the git configuration to " + basedir
                            + " with branch " + branch + " on remote repo: " + remoteRepository + " due: " + e.getMessage() + ". This exception is ignored.", e);
                }
            }
        }
    }

    protected boolean localBranchExists(Git git, String branch) throws GitAPIException {
        List<Ref> list = git.branchList().call();
        String fullName = "refs/heads/" + branch;
        boolean localBranchExists = false;
        for (Ref ref : list) {
            String name = ref.getName();
            if (Objects.equals(name, fullName)) {
                localBranchExists = true;
            }
        }
        return localBranchExists;
    }

    protected String getRemote() {
        return origin;
    }

    protected FileDTO createFileDTO(File file, boolean includeContent) {
        File parentFile = file.getParentFile();
        String relativePath = null;
        try {
            relativePath = trimLeadingSlash(Files.getRelativePath(basedir, parentFile));
        } catch (IOException e) {
            LOG.warn("Failed to find relative path of " + parentFile.getPath() + ". " + e, e);
        }
        FileDTO answer = FileDTO.createFileDTO(file, relativePath, includeContent);
        String path = answer.getPath();
        if (path.equals(".git")) {
            // lets ignore the git folder!
            return null;
        }
        // TODO use the path to generate the links...
        // TODO generate the SHA
        return answer;
    }

    protected String toString(Collection<RemoteRefUpdate> updates) {
        StringBuilder builder = new StringBuilder();
        for (RemoteRefUpdate update : updates) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(update.getMessage() + " " + update.getRemoteName() + " " + update.getNewObjectId());
        }
        return builder.toString();
    }

    protected Iterable<PushResult> doPush(Git git) throws Exception {
        PushCommand command = git.push();
        configureCommand(command, userDetails);
        return command.setRemote(getRemote()).call();
    }

}
