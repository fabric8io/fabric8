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
import io.fabric8.forge.rest.main.GitHelpers;
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
import org.eclipse.jgit.api.errors.GitAPIException;
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
    private final String origin;
    private final String branch;
    private final Repository repository;
    private final Git git;
    private final PersonIdent personIdent;
    private String message;

    public RepositoryResource(File gitFolder, UserDetails userDetails, String origin, String branch, String remoteRepository) throws IOException, GitAPIException {
        this.gitFolder = gitFolder;
        this.userDetails = userDetails;
        this.remoteRepository = remoteRepository;
        this.basedir = gitFolder.getParentFile();
        this.origin = origin;
        String user = userDetails.getUser();
        String authorEmail = userDetails.getEmail();
        this.personIdent = new PersonIdent(user, authorEmail);
        this.branch = branch;
        if (!gitFolder.exists() || !gitFolder.isDirectory()) {
            throw new IOException(".git folder does not exist at " + gitFolder.getPath());
        }

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        this.repository = builder.setGitDir(gitFolder)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        this.git = new Git(repository);
        if (Strings.isNullOrBlank(origin)) {
            throw new IOException("Could not find remote git URL for folder " + gitFolder.getPath());
        }
        checkoutBranch();
    }


    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @GET
    @Path("content/{path:.*}")
    public Response fileDetails(@PathParam("path") String path) throws Exception {
        final File file = getRelativeFile(path);
        if (LOG.isDebugEnabled()) {
            LOG.debug("reading file: " + file.getPath());
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            List<FileDTO> answer = new ArrayList<>();
            if (files != null) {
                for (File child : files) {
                    FileDTO dto = createFileDTO(child, false);
                    if (dto != null) {
                        answer.add(dto);
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
    public Response rawFile(@PathParam("path") String path) throws Exception {
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
    @Path("diff/{objectId1}/{objectId2}")
    public String diff(@PathParam("objectId1") String objectId, @PathParam("objectId2") String baseObjectId) throws IOException {
        return diff(objectId, baseObjectId, null);
    }

    @GET
    @Path("diff/{objectId1}/{objectId2}/{path:.*}")
    public String diff(@PathParam("objectId1") String objectId, @PathParam("objectId2") String baseObjectId, @PathParam("path") String pathOrBlobPath) throws IOException {
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
    public CommitInfo commitInfo(@PathParam("commitId") String commitId) {
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
    public List<CommitTreeInfo> getCommitTree(@PathParam("commitId") String commitId) {
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
    public List<CommitInfo> history(@QueryParam("limit") int limit) {
        return history(null, null, limit);
    }

    @GET
    @Path("history/{commitId}/{path:.*}")
    public List<CommitInfo> history(@PathParam("commitId") String objectId, @PathParam("path") String pathOrBlobPath, @QueryParam("limit") int limit) {
        List<CommitInfo> results = new ArrayList<CommitInfo>();
        Repository r = git.getRepository();

        try {
            String head = getHEAD();
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
                ObjectId branchObjectId = getBranchObjectId();
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
    public CommitInfo createDirectory(String path) throws Exception {
        File file = getRelativeFile(path);
        if (file.exists()) {
            return null;
        }
        file.mkdirs();
        String filePattern = getFilePattern(path);
        AddCommand add = git.add().addFilepattern(filePattern).addFilepattern(".");
        add.call();

        CommitCommand commit = git.commit().setAll(true).setAuthor(personIdent).setMessage(message);
        RevCommit revCommit = commitThenPush(commit);
        return createCommitInfo(revCommit);
    }


    @POST
    @Path("revert/{commitId}/{path:.*}")
    public void revert(@PathParam("commitId") String objectId, @PathParam("path") String blobPath) throws Exception {
        String contents = doGetContent(objectId, blobPath);
        if (contents != null) {
            doWrite(blobPath, contents.getBytes(), personIdent, message);
        }
    }

    @POST
    @Path("mv/{path:.*}")
    public RevCommit rename(@QueryParam("old") String oldPath, @PathParam("path") String newPath) throws Exception {
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
            return commitThenPush(commit);
        } else {
            return null;
        }
    }

    @POST
    @Path("rm/{path:.*}")
    public RevCommit remove(String path) throws Exception {
        File file = getRelativeFile(path);
        if (file.exists()) {
            Files.recursiveDelete(file);

            String filePattern = getFilePattern(path);
            git.rm().addFilepattern(filePattern).call();
            CommitCommand commit = git.commit().setAll(true).setAuthor(personIdent).setMessage(message);
            return commitThenPush(commit);
        } else {
            return null;
        }
    }

    @GET
    @Path("listBranches")
    public List<String> listBranches() throws GitAPIException {
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


    protected Response uploadFile(@PathParam("path") String path, String message, final InputStream body) throws Exception {
        this.message = message;
        final File file = getRelativeFile(path);

        return writeOperation(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("writing file: " + file.getPath());
                }
                file.getParentFile().mkdirs();
                IOHelpers.writeTo(file, body);
                return Response.ok("updated " + file.getPath()).build();
            }
        });
    }

    protected CommitInfo doWrite(String path, byte[] contents, PersonIdent personIdent, String commitMessage) throws Exception {
        File file = getRelativeFile(path);
        file.getParentFile().mkdirs();

        Files.writeToFile(file, contents);

        String filePattern = getFilePattern(path);
        AddCommand add = git.add().addFilepattern(filePattern).addFilepattern(".");
        add.call();

        CommitCommand commit = git.commit().setAll(true).setAuthor(personIdent).setMessage(commitMessage);
        RevCommit revCommit = commitThenPush(commit);
        return createCommitInfo(revCommit);
    }

    protected static String getFilePattern(String path) {
        return trimLeadingSlash(path);
    }

    protected RevCommit commitThenPush(CommitCommand commit) throws Exception {
        RevCommit answer = commit.call();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Committed " + answer.getId() + " " + answer.getFullMessage());
        }
        if (isPushOnCommit()) {
            Iterable<PushResult> results = doPush();
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

    protected <T> T writeOperation(Callable<T> callback) throws Exception {
        String user = userDetails.getUser();
        String authorEmail = userDetails.getEmail();
        String branch = userDetails.getBranch();

        T answer = callback.call();
        disableSslCertificateChecks();

        CredentialsProvider credentials = userDetails.createCredentialsProvider();
        PersonIdent personIdent = new PersonIdent(user, authorEmail);

        if (Strings.isNullOrBlank(message)) {
            message = "";
        }
        doAddCommitAndPushFiles(git, credentials, personIdent, branch, origin, message, isPushOnCommit());
        return answer;
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

    protected String getHEAD() {
        RevCommit commit = CommitUtils.getHead(git.getRepository());
        return commit.getName();
    }

    public static String trimLeadingSlash(String name) {
        if (name != null && name.startsWith("/")) {
            name = name.substring(1);
        }
        return name;
    }

    protected ObjectId getBranchObjectId() {
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

    public String currentBranch() {
        try {
            return git.getRepository().getBranch();
        } catch (IOException e) {
            LOG.warn("Failed to get the current branch due: " + e.getMessage() + ". This exception is ignored.", e);
            return null;
        }
    }

    protected void checkoutBranch() throws GitAPIException {
        String current = currentBranch();
        if (Objects.equals(current, branch)) {
            return;
        }
        // lets check if the branch exists
        CheckoutCommand command = git.checkout().setName(branch);
        boolean exists = localBranchExists(branch);
        if (!exists) {
            command = command.setCreateBranch(true).setForce(true).
                    setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                    setStartPoint(getRemote() + "/" + branch);
        }
        Ref ref = command.call();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Checked out branch " + branch + " with results " + ref.getName());
        }
        configureBranch(branch);
    }


    protected String doGetContent(String objectId, String pathOrBlobPath) {
        objectId = defaultObjectId(objectId);
        Repository r = git.getRepository();
        String blobPath = trimLeadingSlash(pathOrBlobPath);
        return BlobUtils.getContent(r, objectId, blobPath);
    }

    protected String defaultObjectId(String objectId) {
        if (objectId == null || objectId.trim().length() == 0) {
            RevCommit commit = CommitUtils.getHead(git.getRepository());
            objectId = commit.getName();
        }
        return objectId;
    }

    protected void configureBranch(String branch) {
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

    protected boolean localBranchExists(String branch) throws GitAPIException {
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

    protected Iterable<PushResult> doPush() throws Exception {
        CredentialsProvider credentials = userDetails.createCredentialsProvider();
        return this.git.push().setCredentialsProvider(credentials).setRemote(getRemote()).call();
    }
}
