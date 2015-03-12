/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.rest.main;

import io.fabric8.cdi.annotations.Service;
import io.fabric8.forge.rest.dto.ExecutionRequest;
import io.fabric8.forge.rest.hooks.CommandCompletePostProcessor;
import io.fabric8.forge.rest.ui.RestUIContext;
import io.fabric8.repo.git.CreateRepositoryDTO;
import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.JsonHelper;
import io.fabric8.repo.git.RepositoryDTO;
import io.fabric8.utils.Files;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.furnace.util.Strings;
import io.fabric8.forge.rest.dto.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * For new projects; lets git add, git commit, git push otherwise lets git add/commit/push any new/udpated changes
 */
public class GitCommandCompletePostProcessor implements CommandCompletePostProcessor {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitCommandCompletePostProcessor.class);
    private final String gitUser;
    private final String gitPassword;
    private final String gogsUrl;

    @Inject
    public GitCommandCompletePostProcessor(@Service("GOGS_HTTP_SERVICE") String gogsUrl,
                                           @ConfigProperty(name = "GIT_DEFAULT_USER") String gitUser,
                                           @ConfigProperty(name = "GIT_DEFAULT_PASSWORD") String gitPassword) {
        this.gogsUrl = gogsUrl;
        this.gitUser = gitUser;
        this.gitPassword = gitPassword;
    }

    @Override
    public void firePostCompleteActions(String name, ExecutionRequest executionRequest, RestUIContext context, CommandController controller, ExecutionResult results) {
        if (name.equals("project-new")) {
            String targetLocation = null;
            String named = null;
            List<Map<String, String>> inputList = executionRequest.getInputList();
            for (Map<String, String> map : inputList) {
                if (Strings.isNullOrEmpty(targetLocation)) {
                    targetLocation = map.get("targetLocation");
                }
                if (Strings.isNullOrEmpty(named)) {
                    named = map.get("named");
                }
            }
            if (Strings.isNullOrEmpty(targetLocation)) {
                LOG.warn("No targetLocation could be found!");
            } else if (Strings.isNullOrEmpty(named)) {
                LOG.warn("No named could be found!");
            } else {
                File basedir = new File(targetLocation, named);
                if (!basedir.isDirectory() || !basedir.exists()) {
                    LOG.warn("Generated project folder does not exist: " + basedir.getAbsolutePath());
                } else {
                    // lets git init...
                    System.out.println("About to git init folder " + basedir.getAbsolutePath());
                    InitCommand initCommand = Git.init();
                    initCommand.setDirectory(basedir);
                    try {
                        Git git = initCommand.call();
                        LOG.info("Initialised an empty git configuration repo at {}", basedir.getAbsolutePath());

                        String address = gogsUrl.toString();
                        int idx = address.indexOf("://");
                        if (idx > 0) {
                            address = "http" + address.substring(idx);
                        }
                        if (!address.endsWith("/")) {
                            address += "/";
                        }

                        // TODO take these from the request?
                        String user = gitUser;
                        String password = gitPassword;
                        String authorEmail = "dummy@gmail.com";

                        // lets create the repository
                        GitRepoClient repoClient = new GitRepoClient(address, user, password);
                        CreateRepositoryDTO createRepository = new CreateRepositoryDTO();
                        createRepository.setName(named);

                        String fullName = null;
                        RepositoryDTO repository = repoClient.createRepository(createRepository);
                        if (repository != null) {
                            System.out.println("Got repository: " + JsonHelper.toJson(repository));
                            fullName = repository.getFullName();
                        }
                        if (Strings.isNullOrEmpty(fullName)) {
                            fullName = user + "/" + named;
                        }
                        String htmlUrl = address + user + "/" + named;
                        String remote = address + user + "/" + named + ".git";
                        //results.appendOut("Created git repository " + fullName + " at: " + htmlUrl);

                        results.setOutputProperty("fullName", fullName);
                        results.setOutputProperty("cloneUrl", remote);
                        results.setOutputProperty("htmlUrl", htmlUrl);

                        // now lets import the code and publish
                        LOG.info("Using remote: " + remote);
                        String branch = "master";
                        configureBranch(git, branch, remote);

                        CredentialsProvider credentials = new UsernamePasswordCredentialsProvider(user, password);
                        PersonIdent personIdent = new PersonIdent(user, authorEmail);

                        doAddCommitAndPushFiles(git, credentials, personIdent, remote, branch);
                    } catch (Exception e) {
                        handleException(e);
                    }

                }
            }
        } else {
            File basedir = context.getInitialSelectionFile();
            String absolutePath = basedir != null ? basedir.getAbsolutePath() : null;
            System.out.println("===== added or mutated files in folder: " + absolutePath);
            if (basedir != null) {
                File gitFolder = new File(basedir, ".git");
                if (gitFolder.exists() && gitFolder.isDirectory()) {
                    System.out.println("======== has .git folder so lets add/commit files then push!");

                }
            }
        }
    }

    protected void configureBranch(Git git, String branch, String remote) {
        // lets update the merge config
        if (!Strings.isNullOrEmpty(branch)) {
            StoredConfig config = git.getRepository().getConfig();
            if (Strings.isNullOrEmpty(config.getString("branch", branch, "remote")) || Strings.isNullOrEmpty(config.getString("branch", branch, "merge"))) {
                config.setString("branch", branch, "remote", remote);
                config.setString("branch", branch, "merge", "refs/heads/" + branch);
                try {
                    config.save();
                } catch (IOException e) {
                    LOG.error("Failed to save the git configuration to " + git.getRepository().getDirectory()
                            + " with branch " + branch + " on remote repo: " + remote + " due: " + e.getMessage() + ". This exception is ignored.", e);
                }
            }
        }
    }

    private void addFiles(Git git, File... files) throws GitAPIException, IOException {
        File rootDir = GitHelpers.getRootGitDirectory(git);
        for (File file : files) {
            String relativePath = getFilePattern(rootDir, file);
            git.add().addFilepattern(relativePath).call();
        }
    }

    private String getFilePattern(File rootDir, File file) throws IOException {
        String relativePath = Files.getRelativePath(rootDir, file);
        if (relativePath.startsWith(File.separator)) {
            relativePath = relativePath.substring(1);
        }
        return relativePath.replace(File.separatorChar, '/');
    }


    protected void doAddCommitAndPushFiles(Git git, CredentialsProvider credentials, PersonIdent personIdent, String remote, String branch) throws IOException, GitAPIException {
/*
        File rootDir = GitHelpers.getRootGitDirectory(git);
        File[] files = rootDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!ignoreRootFile(file)) {
                    String relativePath = getFilePattern(rootDir, file);
                    git.add().addFilepattern(relativePath).call();
                }
            }
        }
*/
        git.add().addFilepattern(".").call();
        doCommitAndPush(git, "Initial import", credentials, personIdent, remote, branch);
    }

    protected RevCommit doCommitAndPush(Git git, String message, CredentialsProvider credentials, PersonIdent author, String remote, String branch) throws IOException, GitAPIException {
        CommitCommand commit = git.commit().setAll(true).setMessage(message);
        if (author != null) {
            commit = commit.setAuthor(author);
        }

        RevCommit answer = commit.call();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Committed " + answer.getId() + " " + answer.getFullMessage());
        }

        if (isPushOnCommit()) {
            Iterable<PushResult> results = git.push().setCredentialsProvider(credentials).setRemote(remote).call();
            for (PushResult result : results) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Pushed " + result.getMessages() + " " + result.getURI() + " branch: " + branch + " updates: " + GitHelpers.toString(result.getRemoteUpdates()));
                }
            }
        }
        return answer;

    }

    protected boolean isPushOnCommit() {
        return true;
    }

    protected boolean ignoreRootFile(File file) {
        String name = file.getName().toLowerCase();
        return name.equals(".git") || name.equals("tmp") || name.equals("target");
    }

    protected void handleException(Throwable e) {
        LOG.warn("Caught: " + e, e);
        throw new RuntimeException(e);
    }
}
