/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.forge.rest.main;

import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.RepositoryDTO;
import io.fabric8.utils.Files;
import io.fabric8.utils.Strings;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 */
@Singleton
public class ProjectFileSystem {
    private static final transient Logger LOG = LoggerFactory.getLogger(ProjectFileSystem.class);

    private final RepositoryCache repositoryCache;
    private final String rootProjectFolder;
    private final String remote;
    private final String jenkinsWorkflowGitUrl;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

    @Inject
    public ProjectFileSystem(RepositoryCache repositoryCache,
                             @ConfigProperty(name = "PROJECT_FOLDER", defaultValue = "/tmp/fabric8-forge") String rootProjectFolder,
                             @ConfigProperty(name = "GIT_REMOTE_BRANCH_NAME", defaultValue = "origin") String remote,
                             @ConfigProperty(name = "JENKINS_WORKFLOW_GIT_REPOSITORY") String jenkinsWorkflowGitUrl) {
        this.repositoryCache = repositoryCache;
        this.rootProjectFolder = rootProjectFolder;
        this.remote = remote;
        this.jenkinsWorkflowGitUrl = jenkinsWorkflowGitUrl;
        LOG.info("Using jenkins workflow library: " + this.jenkinsWorkflowGitUrl);
    }

    public String getRemote() {
        return remote;
    }

    public String getUserProjectFolderLocation(UserDetails userDetails) {
        File projectFolder = getUserProjectFolder(userDetails);
        return projectFolder.getAbsolutePath();
    }

    public File getUserProjectFolder(UserDetails userDetails) {
        String gitUser = userDetails.getUser();
        return getUserProjectFolder(gitUser);
    }

    public File getUserProjectFolder(String gitUser) {
        File root = new File(rootProjectFolder);
        File userFolder = new File(root, "user");
        File projectFolder = new File(userFolder, gitUser);
        projectFolder.mkdirs();
        return projectFolder;
    }

    public File getJenkinsWorkflowFolder() {
        File root = new File(rootProjectFolder);
        File workflowFolder = new File(root, "jenkinsWorkflows");
        workflowFolder.mkdirs();
        return workflowFolder;
    }

    public File getSecretsFolder(String namespace, String secretName, String dataKey) {
        File root = new File(rootProjectFolder);
        File namespacesFolder = new File(root, "secrets");
        File namespaceFolder = new File(namespacesFolder, namespace);
        File secretsFolder = new File(namespaceFolder, secretName);
        File dataFile = new File(secretsFolder, dataKey);
        secretsFolder.mkdirs();
        return dataFile;
    }

    public File getNamespaceProjectFolder(String namespace, String projectId, String secretNamespace, String secretName) {
        if (Strings.isNullOrBlank(secretNamespace)) {
            secretNamespace = namespace;
        }
        if (Strings.isNullOrBlank(secretName)) {
            secretName = "_null";
        }
        File projectFolder = new File(new File(rootProjectFolder), "namespace/" + secretNamespace + "/" + secretName + "/" + namespace + "/" + projectId);
        projectFolder.mkdirs();
        return projectFolder;
    }

    public File getUserProjectFolder(String user, String project) {
        File userFolder = getUserProjectFolder(user);
        File projectFolder = new File(userFolder, project);
        return projectFolder;
    }

    public void asyncCloneOrPullJenkinsWorkflows(final UserDetails userDetails) {
        final File folder = getJenkinsWorkflowFolder();
        if (Strings.isNotBlank(jenkinsWorkflowGitUrl)) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        LOG.debug("Cloning or pulling jenkins workflow repo from " + jenkinsWorkflowGitUrl + " to " + folder);
                        UserDetails anonymous = userDetails.createAnonymousDetails();
                        cloneOrPullRepo(anonymous, folder, jenkinsWorkflowGitUrl, null, null);
                    } catch (Exception e) {
                        LOG.error("Failed to clone jenkins workflow repo from : " + jenkinsWorkflowGitUrl + ". " + e, e);
                    }
                }
            });
        } else {
            LOG.warn("Cannot clone jenkins workflow repository as the environment variable JENKINS_WORKFLOW_GIT_REPOSITORY is not defined");
        }
    }

    public String getCloneUrl(String user, String repositoryName, UserDetails userDetails) {
        GitRepoClient repoClient = userDetails.createRepoClient();
        RepositoryDTO dto = repositoryCache.getOrFindUserRepository(user, repositoryName, repoClient);
        if (dto == null) {
            throw new NotFoundException("No repository defined for user: " + user + " and name: " + repositoryName);
        }
        String cloneUrl = dto.getCloneUrl();
        if (Strings.isNullOrBlank(cloneUrl)) {
            throw new NotFoundException("No cloneUrl defined for user repository: " + user + "/" + repositoryName);
        }
        return cloneUrl;
    }

    public File cloneOrPullProjectFolder(String user, String repositoryName, UserDetails userDetails) {
        File projectFolder = getUserProjectFolder(user, repositoryName);
        GitRepoClient repoClient = userDetails.createRepoClient();
        RepositoryDTO dto = repositoryCache.getOrFindUserRepository(user, repositoryName, repoClient);
        if (dto == null) {
            throw new NotFoundException("No repository defined for user: " + user + " and name: " + repositoryName);
        }
        String cloneUrl = dto.getCloneUrl();
        if (Strings.isNullOrBlank(cloneUrl)) {
            throw new NotFoundException("No cloneUrl defined for user repository: " + user + "/" + repositoryName);
        }
        return cloneOrPullRepo(userDetails, projectFolder, cloneUrl, null, null);
    }

    public File cloneOrPullRepo(UserDetails userDetails, File projectFolder, String cloneUrl, File sshPrivateKey, File sshPublicKey) {
        File gitFolder = new File(projectFolder, ".git");
        CredentialsProvider credentialsProvider = userDetails.createCredentialsProvider();
        if (!Files.isDirectory(gitFolder) || !Files.isDirectory(projectFolder)) {
            // lets clone the git repository!
            cloneRepo(projectFolder, cloneUrl, credentialsProvider, sshPrivateKey, sshPublicKey, this.remote);
        } else {
            doPull(gitFolder, credentialsProvider, userDetails.getBranch(), userDetails.createPersonIdent(), userDetails);
        }
        return projectFolder;
    }

    public File cloneRepoIfNotExist(UserDetails userDetails, File projectFolder, String cloneUrl) {
        File gitFolder = new File(projectFolder, ".git");
        CredentialsProvider credentialsProvider = userDetails.createCredentialsProvider();
        if (!Files.isDirectory(gitFolder) || !Files.isDirectory(projectFolder)) {
            // lets clone the git repository!
            cloneRepo(projectFolder, cloneUrl, credentialsProvider, userDetails.getSshPrivateKey(), userDetails.getSshPublicKey(), this.remote);

        }
        return projectFolder;
    }

    public static void cloneRepo(File projectFolder, String cloneUrl, CredentialsProvider credentialsProvider, final File sshPrivateKey, final File sshPublicKey, String remote) {
        // clone the repo!
        boolean cloneAll = true;
        LOG.info("Cloning git repo " + cloneUrl + " into directory " + projectFolder.getAbsolutePath() + " cloneAllBranches: " + cloneAll);
        CloneCommand command = Git.cloneRepository();
        GitHelpers.configureCommand(command, credentialsProvider, sshPrivateKey, sshPublicKey);
        command = command.setCredentialsProvider(credentialsProvider).
                        setCloneAllBranches(cloneAll).setURI(cloneUrl).setDirectory(projectFolder).setRemote(remote);

        try {
            Git git = command.call();
        } catch (Throwable e) {
            LOG.error("Failed to command remote repo " + cloneUrl + " due: " + e.getMessage(), e);
            throw new RuntimeException("Failed to command remote repo " + cloneUrl + " due: " + e.getMessage());
        }
    }

    protected void doPull(File gitFolder, CredentialsProvider cp, String branch, PersonIdent personIdent, UserDetails userDetails) {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder.setGitDir(gitFolder)
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree
                    .build();

            Git git = new Git(repository);

            File projectFolder = repository.getDirectory();

            StoredConfig config = repository.getConfig();
            String url = config.getString("remote", remote, "url");
            if (Strings.isNullOrBlank(url)) {
                LOG.warn("No remote repository url for " + branch + " defined for the git repository at " + projectFolder.getCanonicalPath() + " so cannot pull");
                //return;
            }
            String mergeUrl = config.getString("branch", branch, "merge");
            if (Strings.isNullOrBlank(mergeUrl)) {
                LOG.warn("No merge spec for branch." + branch + ".merge in the git repository at " + projectFolder.getCanonicalPath() + " so not doing a pull");
                //return;
            }

            // lets trash any failed changes
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

            //LOG.info("Resetting the repo");
            //git.reset().setMode(ResetCommand.ResetType.HARD).call();

            LOG.info("Performing a pull in git repository " + projectFolder.getCanonicalPath() + " on remote URL: " + url);
            PullCommand pull = git.pull();
            GitHelpers.configureCommand(pull, userDetails);
            pull.setRebase(true).call();
        } catch (Throwable e) {
            LOG.error("Failed to pull from the remote git repo with credentials " + cp + " due: " + e.getMessage() + ". This exception is ignored.", e);
        }
    }

    public void invokeLater(Runnable runnable, long millis) {
        executorService.schedule(runnable, millis, TimeUnit.MILLISECONDS);
    }

}
