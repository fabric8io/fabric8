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

import io.fabric8.devops.connector.DevOpsConnector;
import io.fabric8.forge.rest.dto.ExecutionRequest;
import io.fabric8.forge.rest.dto.ExecutionResult;
import io.fabric8.forge.rest.hooks.CommandCompletePostProcessor;
import io.fabric8.forge.rest.ui.RestUIContext;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.repo.git.CreateRepositoryDTO;
import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.RepositoryDTO;
import io.fabric8.utils.IOHelpers;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.lib.PersonIdent;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.furnace.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.fabric8.utils.cxf.JsonHelper.toJson;

/**
 * For new projects; lets git add, git commit, git push otherwise lets git add/commit/push any new/udpated changes
 */
// TODO we should try add this into the ConfigureDevOpsStep.execute() block instead!
public class GitCommandCompletePostProcessor implements CommandCompletePostProcessor {
    private static final transient Logger LOG = LoggerFactory.getLogger(GitCommandCompletePostProcessor.class);
    public static final String PROJECT_NEW_COMMAND = "project-new";
    public static final String TARGET_LOCATION_PROPERTY = "targetLocation";
    public static final String DEFAULT_JENKINS_SEED_JOB = "seed";
    private final KubernetesClient kubernetes;
    private final GitUserHelper gitUserHelper;
    private final ProjectFileSystem projectFileSystem;
    private final String jenkinsSeedJob;
    private final boolean createOpenShiftBuildResources;

    @Inject
    public GitCommandCompletePostProcessor(KubernetesClient kubernetes,
                                           GitUserHelper gitUserHelper,
                                           ProjectFileSystem projectFileSystem,
                                           @ConfigProperty(name = "JENKINS_SEED_JOB", defaultValue = DEFAULT_JENKINS_SEED_JOB) String jenkinsSeedJob,
                                           @ConfigProperty(name = "OPENSHIFT_CREATE_BUILD_ON_PROJECT_CREATE", defaultValue = "false") boolean createOpenShiftBuildResources) {
        this.kubernetes = kubernetes;
        this.gitUserHelper = gitUserHelper;
        this.projectFileSystem = projectFileSystem;
        this.jenkinsSeedJob = jenkinsSeedJob;
        this.createOpenShiftBuildResources = createOpenShiftBuildResources;
    }

    @Override
    public UserDetails preprocessRequest(String name, ExecutionRequest executionRequest, HttpServletRequest request) {
        UserDetails userDetails = gitUserHelper.createUserDetails(request);
        // TODO this isn't really required if there's a secret associated with the BuildConfig source
        if (Strings.isNullOrEmpty(userDetails.getUser()) || Strings.isNullOrEmpty(userDetails.getUser())) {
            throw new NotAuthorizedException("You must authenticate to be able to perform this command");
        }

        if (Objects.equals(name, PROJECT_NEW_COMMAND)) {
            List<Map<String, String>> inputList = executionRequest.getInputList();
            if (inputList != null) {
                Map<String, String> page1 = inputList.get(0);
                if (page1 != null) {
                    if (page1.containsKey(TARGET_LOCATION_PROPERTY)) {
                        page1.put(TARGET_LOCATION_PROPERTY, projectFileSystem.getUserProjectFolderLocation(userDetails));
                    }
/*

                    page1.put(TARGET_LOCATION_PROPERTY, projectFileSystem.getUserProjectFolderLocation(userDetails));
*/
                }
            }
        }
        return userDetails;
    }


    @Override
    public void firePostCompleteActions(String name, ExecutionRequest executionRequest, RestUIContext context, CommandController controller, ExecutionResult results, HttpServletRequest request) {
        UserDetails userDetails = gitUserHelper.createUserDetails(request);

        String user = userDetails.getUser();
        String address = userDetails.getAddress();
        String internalAddress = userDetails.getInternalAddress();
        String branch = userDetails.getBranch();
        String origin = projectFileSystem.getRemote();

        try {
            if (name.equals(PROJECT_NEW_COMMAND)) {
                GitHelpers.disableSslCertificateChecks();

                PersonIdent personIdent = userDetails.createPersonIdent();

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
                        InitCommand initCommand = Git.init();
                        initCommand.setDirectory(basedir);
                        Git git = initCommand.call();
                        LOG.info("Initialised an empty git configuration repo at {}", basedir.getAbsolutePath());

                        // lets create the repository
                        GitRepoClient repoClient = userDetails.createRepoClient();
                        CreateRepositoryDTO createRepository = new CreateRepositoryDTO();
                        createRepository.setName(named);

                        String fullName = null;
                        RepositoryDTO repository = repoClient.createRepository(createRepository);
                        if (repository != null) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Got repository: " + toJson(repository));
                            }
                            fullName = repository.getFullName();
                        }
                        if (Strings.isNullOrEmpty(fullName)) {
                            fullName = user + "/" + named;
                        }
                        String htmlUrl = address + user + "/" + named;
                        String remoteUrl = internalAddress + user + "/" + named + ".git";
                        //results.appendOut("Created git repository " + fullName + " at: " + htmlUrl);
                        String cloneUrl =  htmlUrl + ".git";

                        results.setOutputProperty("fullName", fullName);
                        results.setOutputProperty("cloneUrl", remoteUrl);
                        results.setOutputProperty("htmlUrl", htmlUrl);

                        // now lets import the code and publish
                        LOG.info("Using remoteUrl: " + remoteUrl + " and remote name " + origin);
                        GitHelpers.configureBranch(git, branch, origin, remoteUrl);

                        addDummyFileToEmptyFolders(basedir);
                        String message = ExecutionRequest.createCommitMessage(name, executionRequest);
                        LOG.info("Commiting and pushing to: " + remoteUrl + " and remote name " + origin);
                        GitHelpers.doAddCommitAndPushFiles(git, userDetails, personIdent, branch, origin, message, isPushOnCommit());

                        String namespace = firstNotBlank(context.getProjectName(), executionRequest.getNamespace());
                        String projectName = firstNotBlank(named, context.getProjectName(), executionRequest.getProjectName());
                        results.setProjectName(projectName);
                        createBuildConfig(context, namespace, projectName, cloneUrl);

                        LOG.info("Creating any pending webhooks");
                        registerWebHooks(context);

                        LOG.info("Done creating webhooks!");
                    }
                }
            } else {
                registerWebHooks(context);
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    public static String firstNotBlank(String... texts) {
        for (String text : texts) {
            if (!Strings.isNullOrEmpty(text)) {
                return text;
            }
        }
        return null;
    }

    protected void createBuildConfig(RestUIContext context, String namespace, String projectName, String cloneUrl) throws Exception {
        LOG.info("Creating a BuildConfig for namespace: " + namespace + " project: " + projectName);
        DevOpsConnector connector = new DevOpsConnector();
        connector.setGitUrl(cloneUrl);
        connector.setNamespace(namespace);
        connector.setProjectName(projectName);
        connector.execute();
    }

    /**
     * Git tends to ignore empty directories so lets add a dummy file to empty folders to keep them in git
     */
    protected void addDummyFileToEmptyFolders(File dir) {
        if (dir != null && dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children == null || children.length == 0) {
                File dummyFile = new File(dir, ".gitkeep");
                try {
                    IOHelpers.writeFully(dummyFile, "This file is only here to avoid git removing empty folders\nOnce there are files in this folder feel free to delete this file!");
                } catch (IOException e) {
                    LOG.warn("Failed to write file " + dummyFile + ". " + e, e);
                }
            } else {
                for (File child : children) {
                    if (child.isDirectory()) {
                        addDummyFileToEmptyFolders(child);
                    }
                }
            }
        }
    }

    protected void registerWebHooks(RestUIContext context) {
        Map<Object, Object> attributeMap = context.getAttributeMap();
        Object registerWebHooksValue = attributeMap.get("registerWebHooks");
        if (registerWebHooksValue instanceof Runnable) {
            Runnable runnable = (Runnable) registerWebHooksValue;
            projectFileSystem.invokeLater(runnable, 1000L);
        }
    }

    protected boolean isPushOnCommit() {
        return true;
    }

    protected void handleException(Throwable e) {
        LOG.warn("Caught: " + e, e);
        throw new RuntimeException(e);
    }
}
