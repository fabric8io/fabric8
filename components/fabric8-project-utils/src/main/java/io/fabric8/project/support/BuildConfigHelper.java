/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.project.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.kubernetes.api.builds.Builds;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.repo.git.CreateRepositoryDTO;
import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.RepositoryDTO;
import io.fabric8.utils.Strings;
import io.fabric8.utils.URLUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static io.fabric8.kubernetes.api.KubernetesHelper.getName;
import static io.fabric8.kubernetes.api.KubernetesHelper.getNamespace;
import static io.fabric8.utils.cxf.JsonHelper.toJson;

/**
 */
public class BuildConfigHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(BuildConfigHelper.class);

    /**
     * Returns the created BuildConfig for the given project name and git repository
     */
    public static BuildConfig createAndApplyBuildConfig(KubernetesClient kubernetesClient, String namespace, String projectName, String cloneUrl) {
        BuildConfig buildConfig = createBuildConfig(kubernetesClient, namespace, projectName, cloneUrl);
        Controller controller = new Controller(kubernetesClient);
        controller.setNamespace(namespace);
        controller.applyBuildConfig(buildConfig, "from project " + projectName);
        return buildConfig;
    }

    public static BuildConfig createBuildConfig(KubernetesClient kubernetesClient, String namespace, String projectName, String cloneUrl) {
        LOG.info("Creating a BuildConfig for namespace: " + namespace + " project: " + projectName);
        String jenkinsUrl = getJenkinsServiceUrl(kubernetesClient, namespace);
        return Builds.createDefaultBuildConfig(projectName, cloneUrl, jenkinsUrl);
    }

    /**
     * Returns the URL to the fabric8 console
     */
    public static String getBuildConfigConsoleURL(KubernetesClient kubernetes, String consoleNamespace, BuildConfig buildConfig) {
        String name = getName(buildConfig);
        String namespace = getNamespace(buildConfig);
        if (Strings.isNullOrBlank(namespace)) {
            namespace = consoleNamespace;
        }
        String consoleURL = getFabric8ConsoleServiceUrl(kubernetes, namespace);
        if (Strings.isNotBlank(consoleURL)) {
            if (Strings.isNotBlank(name)) {
                return URLUtils.pathJoin(consoleURL, "workspaces", namespace, "projects",name);
            }
            return URLUtils.pathJoin(consoleURL, "workspaces", namespace);
        }
        return null;
    }

    private static String getJenkinsServiceUrl(KubernetesClient kubernetes, String namespace) {
        return KubernetesHelper.getServiceURL(kubernetes, ServiceNames.JENKINS, namespace, "http", true);
    }

    private static String getFabric8ConsoleServiceUrl(KubernetesClient kubernetes, String namespace) {
        return KubernetesHelper.getServiceURL(kubernetes, ServiceNames.FABRIC8_CONSOLE, namespace, "http", true);
    }

    public static CreateGitProjectResults importNewGitProject(KubernetesClient kubernetesClient, UserDetails userDetails, File basedir, String namespace, String projectName, String origin, String message, boolean apply) throws GitAPIException, JsonProcessingException {
        GitUtils.disableSslCertificateChecks();

        InitCommand initCommand = Git.init();
        initCommand.setDirectory(basedir);
        Git git = initCommand.call();
        LOG.info("Initialised an empty git configuration repo at {}", basedir.getAbsolutePath());

        PersonIdent personIdent = userDetails.createPersonIdent();

        String user = userDetails.getUser();
        String address = userDetails.getAddress();
        String internalAddress = userDetails.getInternalAddress();
        String branch = userDetails.getBranch();

        // lets create the repository
        GitRepoClient repoClient = userDetails.createRepoClient();
        CreateRepositoryDTO createRepository = new CreateRepositoryDTO();
        createRepository.setName(projectName);

        String fullName = null;
        RepositoryDTO repository = repoClient.createRepository(createRepository);
        if (repository != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Got repository: " + toJson(repository));
            }
            fullName = repository.getFullName();
        }
        if (Strings.isNullOrBlank(fullName)) {
            fullName = user + "/" + projectName;
        }

        String htmlUrl = URLUtils.pathJoin(address, user, projectName);
        String remoteUrl = URLUtils.pathJoin(internalAddress,  user, projectName + ".git");
        String cloneUrl = htmlUrl + ".git";

        // now lets import the code and publish
        LOG.info("Using remoteUrl: " + remoteUrl + " and remote name " + origin);
        GitUtils.configureBranch(git, branch, origin, remoteUrl);

        GitUtils.addDummyFileToEmptyFolders(basedir);
        LOG.info("About to git commit and push to: " + remoteUrl + " and remote name " + origin);
        GitUtils.doAddCommitAndPushFiles(git, userDetails, personIdent, branch, origin, message, true);

        BuildConfig buildConfig;
        if (apply) {
            buildConfig = createAndApplyBuildConfig(kubernetesClient, namespace, projectName, cloneUrl);
        } else {
            buildConfig = createBuildConfig(kubernetesClient, namespace, projectName, cloneUrl);
        }
        return new CreateGitProjectResults(buildConfig, fullName, htmlUrl, remoteUrl, cloneUrl);
    }

    public static class CreateGitProjectResults {
        private final BuildConfig buildConfig;
        private final String fullName;
        private final String htmlUrl;
        private final String remoteUrl;
        private final String cloneUrl;

        public CreateGitProjectResults(BuildConfig buildConfig, String fullName, String htmlUrl, String remoteUrl, String cloneUrl) {
            this.buildConfig = buildConfig;
            this.fullName = fullName;
            this.htmlUrl = htmlUrl;
            this.remoteUrl = remoteUrl;
            this.cloneUrl = cloneUrl;
        }

        @Override
        public String toString() {
            return "CreateGitProjectResults{" +
                    "fullName='" + fullName + '\'' +
                    ", htmlUrl='" + htmlUrl + '\'' +
                    ", remoteUrl='" + remoteUrl + '\'' +
                    ", cloneUrl='" + cloneUrl + '\'' +
                    '}';
        }

        public BuildConfig getBuildConfig() {
            return buildConfig;
        }

        public String getFullName() {
            return fullName;
        }

        public String getHtmlUrl() {
            return htmlUrl;
        }

        public String getRemoteUrl() {
            return remoteUrl;
        }

        public String getCloneUrl() {
            return cloneUrl;
        }
    }
}
