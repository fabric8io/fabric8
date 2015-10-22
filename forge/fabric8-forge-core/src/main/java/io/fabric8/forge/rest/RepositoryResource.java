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
package io.fabric8.forge.rest;

import io.fabric8.forge.rest.dto.FileDTO;
import io.fabric8.forge.rest.main.UserDetails;
import io.fabric8.utils.Files;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Strings;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
    private final String origin;
    private final String branch;
    private String message;

    public RepositoryResource(File gitFolder, UserDetails userDetails, String origin, String branch) throws IOException {
        this.gitFolder = gitFolder;
        this.userDetails = userDetails;
        this.basedir = gitFolder.getParentFile();
        this.origin = origin;
        this.branch = branch;
        System.out.println("Using git folder: " + gitFolder);
        if (!gitFolder.exists() || !gitFolder.isDirectory()) {
            throw new IOException(".git folder does not exist at " + gitFolder.getPath());
        }
        if (Strings.isNullOrBlank(origin)) {
            throw new IOException("Could not find remote git URL for folder " + gitFolder.getPath());
        }
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

    protected FileDTO createFileDTO(File file, boolean includeContent) {
        File parentFile = file.getParentFile();
        String relativePath = null;
        try {
            relativePath = Files.getRelativePath(basedir, parentFile);
            if (relativePath != null && relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
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

    protected File getRelativeFile(String path) {
        return new File(basedir, path);
    }

    protected <T> T writeOperation(Callable<T> callback) throws Exception {
        String user = userDetails.getUser();
        String authorEmail = userDetails.getEmail();
        String branch = userDetails.getBranch();

        T answer = callback.call();
        disableSslCertificateChecks();

        CredentialsProvider credentials = userDetails.createCredentialsProvider();
        PersonIdent personIdent = new PersonIdent(user, authorEmail);

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(gitFolder)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        Git git = new Git(repository);

        if (Strings.isNullOrBlank(message)) {
            message = "";
        }
        doAddCommitAndPushFiles(git, credentials, personIdent, branch, origin, message, isPushOnCommit());
        return answer;
    }

    protected boolean isPushOnCommit() {
        return true;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
