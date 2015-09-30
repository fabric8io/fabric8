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
package io.fabric8.devops.connector;

import io.fabric8.devops.ProjectConfig;
import io.fabric8.devops.ProjectConfigs;
import io.fabric8.devops.ProjectRepositories;
import io.fabric8.gerrit.ProjectInfoDTO;
import io.fabric8.gerrit.CreateRepositoryDTO;
import io.fabric8.gerrit.GitApi;
import io.fabric8.gerrit.RepositoryDTO;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.kubernetes.api.builders.ListEnvVarBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.letschat.LetsChatClient;
import io.fabric8.letschat.LetsChatKubernetes;
import io.fabric8.letschat.RoomDTO;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildConfigFluent;
import io.fabric8.repo.git.*;
import io.fabric8.taiga.ModuleDTO;
import io.fabric8.taiga.ProjectDTO;
import io.fabric8.taiga.TaigaClient;
import io.fabric8.taiga.TaigaKubernetes;
import io.fabric8.taiga.TaigaModule;
import io.fabric8.utils.DomHelper;
import io.fabric8.utils.GitHelpers;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import io.fabric8.utils.URLUtils;
import io.fabric8.utils.cxf.WebClients;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fabric8.utils.cxf.JsonHelper.toJson;
import static io.fabric8.utils.cxf.WebClients.configureUserAndPassword;
import static io.fabric8.utils.cxf.WebClients.disableSslChecks;
import static io.fabric8.utils.cxf.WebClients.enableDigestAuthenticaionType;

/**
 * Updates a project's connections to its various DevOps resources like issue tracking, chat and jenkins builds
 */
public class DevOpsConnector {
    private transient Logger log = LoggerFactory.getLogger(DevOpsConnector.class);

    private static final String JSON_MAGIC = ")]}'";

    private File basedir;
    private ProjectConfig projectConfig;

    private String username;
    private String password;
    private String branch;
    private String repoName;
    private String fullName;

    private String gitUrl;
    private String secret = "secret101";
    private String buildImageStream = "triggerJenkins";
    private String buildImageTag = "latest";
    private String s2iCustomBuilderImage = "fabric8/openshift-s2i-jenkins-trigger";
    private String jenkinsJob;
    private boolean triggerJenkinsJob = true;

    private String jenkinsMonitorView;
    private String jenkinsPipelineView;
    private String taigaProjectName;
    private String taigaProjectSlug;
    private String taigaProjectLinkPage = "backlog";
    private String taigaProjectLinkLabel = "Backlog";

    private String issueTrackerUrl;
    private String issueTrackerLabel = "Issues";

    private String teamUrl;
    private String teamLabel = "Team";

    private String releasesUrl;
    private String releasesLabel = "Releases";

    private String repositoryBrowseLink;
    private String repositoryBrowseLabel = "Repository";

    private String taigaTeamLinkPage = "team";
    private String taigaTeamLinkLabel = "Team";
    private boolean taigaAutoCreate = true;
    private boolean taigaEnabled = true;

    private boolean letschatEnabled = true;
    private String letschatRoomLinkLabel = "Room";
    private String letschatRoomExpression = "fabric8_${namespace}";

    private String flowGitUrl = Systems.getEnvVar("JENKINS_WORKFLOW_GIT_REPOSITORY", "https://github.com/fabric8io/jenkins-workflow-library.git");
    private String gerritUser = Systems.getEnvVar("GERRIT_ADMIN_USER","admin");
    private String gerritPwd = Systems.getEnvVar("GERRIT_ADMIN_PWD","secret");
    private String gerritGitInitialCommit = Systems.getEnvVar("GERRIT_INITIAL_COMMIT","false");
    private String gerritGitRepoDesription = Systems.getEnvVar("GERRIT_REPO_DESCRIPTION","Description of the gerrit git repo");

    private boolean recreateMode;
    private String namespace = KubernetesHelper.defaultNamespace();
    private String fabric8ConsoleNamespace = KubernetesHelper.defaultNamespace();
    private String jenkinsNamespace = KubernetesHelper.defaultNamespace();

    private boolean tryLoadConfigFileFromRemoteGit = true;
    private boolean modifiedConfig;
    private boolean registerWebHooks;

    private GitRepoClient gitRepoClient;
    private KubernetesClient kubernetes;
    private String jenkinsJobUrl;
    private ProjectDTO taigaProject;
    private TaigaClient taiga;


    @Override
    public String toString() {
        return "DevOpsConnector{" +
                "gitUrl='" + gitUrl + '\'' +
                ", basedir=" + basedir +
                ", username='" + username + '\'' +
                ", branch='" + branch + '\'' +
                ", repoName='" + repoName + '\'' +
                '}';
    }


    /**
     * For a given project this operation will try to update the associated DevOps resources
     * @throws Exception
     */
    public void execute() throws Exception {
        loadConfigFile();
        KubernetesClient kubernetes = getKubernetes();

        String name = null;
        if (projectConfig != null) {
            name = projectConfig.getBuildName();
        }
        if (Strings.isNullOrBlank(name)) {
            name = jenkinsJob;
        }
        if (Strings.isNullOrBlank(name)) {
            name = ProjectRepositories.createBuildName(username, repoName);
            if (projectConfig != null) {
                projectConfig.setBuildName(name);
            }
        }
        Map<String, String> labels = new HashMap<>();
        labels.put("user", username);
        labels.put("repo", repoName);

        getLog().info("build name " + name);

        taiga = null;
        taigaProject = null;
        try {
            taiga = createTaiga();
            taigaProject = createTaigaProject(taiga);
        } catch (Exception e) {
            getLog().error("Failed to load or lazily create the Taiga project: " + e, e);
        }
        getLog().info("taiga " + taiga);

        LetsChatClient letschat = null;
        try {
            letschat = createLetsChat();
        } catch (Exception e) {
            getLog().error("Failed to load or lazily create the LetsChat client: " + e, e);
        }
        getLog().info("letschat " + letschat);
        
        /*
         * Create Gerrit Git to if isGerritReview is enabled
         */
        if (projectConfig.hasCodeReview()) {
            try {
                createGerritRepo(repoName, gerritUser, gerritPwd, gerritGitInitialCommit, gerritGitRepoDesription);
            } catch (Exception e) {
                getLog().error("Failed to create GerritGit repo : " + e, e);
            }
        }

        Map<String, String> annotations = new HashMap<>();
        jenkinsJobUrl = null;
        String jenkinsUrl = null;
        try {
            jenkinsUrl = getJenkinsServiceUrl();

            if (Strings.isNotBlank(jenkinsUrl)) {
                if (Strings.isNotBlank(jenkinsMonitorView)) {
                    String url = URLUtils.pathJoin(jenkinsUrl, "/view", jenkinsMonitorView);
                    annotationLink(annotations, "fabric8.link.jenkins.monitor/", url, "Monitor");
                }
                if (Strings.isNotBlank(jenkinsPipelineView)) {
                    String url = URLUtils.pathJoin(jenkinsUrl, "/view", jenkinsPipelineView);
                    annotationLink(annotations, "fabric8.link.jenkins.pipeline/", url, "Pipeline");
                }
                if (Strings.isNotBlank(name)) {
                    jenkinsJobUrl = URLUtils.pathJoin(jenkinsUrl, "/job", name);
                    annotationLink(annotations, "fabric8.link.jenkins.job/", jenkinsJobUrl, "Job");
                }
            }
        } catch (Exception e) {
            getLog().warn("Could not find the Jenkins URL!: " + e, e);
        }
        getLog().info("jenkins " + jenkinsUrl);


        if (!annotationLink(annotations, "fabric8.link.issues/", issueTrackerUrl, issueTrackerLabel)) {
            String taigaLink = getProjectPageLink(taiga, taigaProject, this.taigaProjectLinkPage);
            annotationLink(annotations, "fabric8.link.taiga/", taigaLink, taigaProjectLinkLabel);
        }
        if (!annotationLink(annotations, "fabric8.link.team/", teamUrl, teamLabel)) {
            String taigaTeamLink = getProjectPageLink(taiga, taigaProject, this.taigaTeamLinkPage);
            annotationLink(annotations, "fabric8.link.taiga.team/", taigaTeamLink, taigaTeamLinkLabel);
        }
        annotationLink(annotations, "fabric8.link.releases/", releasesUrl, releasesLabel);

        String chatRoomLink = getChatRoomLink(letschat);
        annotationLink(annotations, "fabric8.link.letschat.room/", chatRoomLink, letschatRoomLinkLabel);

        annotationLink(annotations, "fabric8.link.repository.browse/", repositoryBrowseLink, repositoryBrowseLabel);

        ProjectConfigs.defaultEnvironments(projectConfig);

        String consoleUrl = getServiceUrl(ServiceNames.FABRIC8_CONSOLE, namespace, fabric8ConsoleNamespace);
        if (Strings.isNotBlank(consoleUrl) && projectConfig != null) {
            Map<String, String> environments = projectConfig.getEnvironments();
            if (environments != null) {
                for (Map.Entry<String, String> entry : environments.entrySet()) {
                    String label = entry.getKey();
                    String value = entry.getValue();
                    String key = value;
                    String environmentLink = URLUtils.pathJoin(consoleUrl, "/kubernetes/pods?namespace=" + value);
                    annotations.put("fabric8.link.environment." + key + "/url", environmentLink);
                    annotations.put("fabric8.link.environment." + key + "/label", label);
                    addLink(label, environmentLink);
                }
            }
        }

        BuildConfigFluent<BuildConfigBuilder>.SpecNested<BuildConfigBuilder> specBuilder = new BuildConfigBuilder().
                withNewMetadata().withName(name).withLabels(labels).withAnnotations(annotations).endMetadata().
                withNewSpec();

        if (Strings.isNotBlank(gitUrl)) {
            specBuilder = specBuilder.withNewSource().
                    withType("Git").withNewGit().withUri(gitUrl).endGit().
                    endSource();
        }
        if (Strings.isNotBlank(buildImageStream) && Strings.isNotBlank(buildImageTag)) {

            ListEnvVarBuilder envBuilder = new ListEnvVarBuilder();
            envBuilder.withEnvVar("BASE_URI", jenkinsUrl);
            envBuilder.withEnvVar("JOB_NAME", name);

            specBuilder = specBuilder.
                    withNewStrategy().
                    withType("Custom").withNewCustomStrategy().withNewFrom().withKind("DockerImage").withName(s2iCustomBuilderImage).endFrom()
                    .withEnv(envBuilder.build()).endCustomStrategy().
                            endStrategy();

        }
        BuildConfig buildConfig = specBuilder.
                addNewTrigger().
                withType("GitHub").withNewGithub().withSecret(secret).endGithub().
                endTrigger().
                addNewTrigger().
                withType("Generic").withNewGeneric().withSecret(secret).endGeneric().
                endTrigger().
                endSpec().
                build();

        Controller controller = createController();
        try {
            getLog().info("About to apply build config: " + new JSONObject(KubernetesHelper.toJson(buildConfig)).toString(4));
            controller.applyBuildConfig(buildConfig, "maven");

            getLog().info("Created build configuration for " + name + " in namespace: " + controller.getNamespace() + " at " + kubernetes.getMasterUrl());
        } catch (Exception e) {
            getLog().error("Failed to create BuildConfig for " + KubernetesHelper.toJson(buildConfig) + ". " + e, e);
        }
        if (Strings.isNotBlank(name)) {
            createJenkinsJob(name, jenkinsJobUrl);
            getLog().info("created jenkins job");
        }
        if (isRegisterWebHooks()) {
            registerWebHooks();
            getLog().info("webhooks done");
        }
        if (modifiedConfig) {
            if (basedir == null) {
                getLog().error("Could not save updated " + ProjectConfigs.FILE_NAME + " due to missing basedir");
            } else {
                try {
                    ProjectConfigs.saveToFolder(basedir, projectConfig, true);
                    getLog().info("Updated " + ProjectConfigs.FILE_NAME);
                } catch (IOException e) {
                    getLog().error("Could not save updated " + ProjectConfigs.FILE_NAME + ": " + e, e);
                }
            }
        }
    }

    protected String getJenkinsServiceUrl() {
        return getServiceUrl(ServiceNames.JENKINS, namespace, jenkinsNamespace);
    }


    /**
     * Looks in the given namespaces for the given service or returns null if it could not be found
     */
    protected String getServiceUrl(String serviceName, String... namespaces) {
        return getServiceUrl(serviceName, true, namespaces);
    }

    private String getServiceUrl(String serviceName, boolean serviceExternal, String... namespaces) {
        List<String> namespaceList = new ArrayList<>(Arrays.asList(namespaces));
        String[] defaults = { KubernetesHelper.defaultNamespace(), "default" };
        for (String defaultNamespace : defaults) {
            if (namespaceList.contains(defaultNamespace)) {
                namespaceList.add(defaultNamespace);
            }
        }
        for (String namespace : namespaceList) {
            try {
                return KubernetesHelper.getServiceURL(getKubernetes(), serviceName, namespace, "http", serviceExternal);
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }

    protected boolean annotationLink(Map<String, String> annotations, String annotationPrefix, String issueTrackerUrl, String issueTrackerLabel) {
        if (Strings.isNotBlank(issueTrackerUrl)) {
            annotations.put(annotationPrefix + "url", issueTrackerUrl);
            annotations.put(annotationPrefix + "label", issueTrackerLabel);
            addLink(issueTrackerLabel, issueTrackerUrl);
            return true;
        } else {
            return false;
        }
    }

    public void registerWebHooks() {
        if (Strings.isNotBlank(jenkinsJobUrl)) {
            createJenkinsWebhook(jenkinsJobUrl);
        }
        if (taiga != null && taigaProject != null) {
            createTaigaWebhook(taiga, taigaProject);
        }
    }

    public void addLink(String label, String url) {
        if (projectConfig == null) {
            projectConfig = new ProjectConfig();
        }
        projectConfig.addLink(label, url);
        modifiedConfig = true;
    }

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public KubernetesClient getKubernetes() {
        if (kubernetes == null) {
            kubernetes = new DefaultKubernetesClient();
        }
        return kubernetes;
    }

    public GitRepoClient getGitRepoClient() {
        if (gitRepoClient == null) {
            gitRepoClient = GitRepoKubernetes.createGitRepoClient(getKubernetes(), username, password);
            if (gitRepoClient != null) {
                if (Strings.isNullOrBlank(username)) {
                    username = gitRepoClient.getUsername();
                }
                if (Strings.isNullOrBlank(password)) {
                    password = gitRepoClient.getPassword();
                }
            }
        }
        return gitRepoClient;
    }



    // Properties
    //-------------------------------------------------------------------------

    public File getBasedir() {
        return basedir;
    }

    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getBuildImageStream() {
        return buildImageStream;
    }

    public void setBuildImageStream(String buildImageStream) {
        this.buildImageStream = buildImageStream;
    }

    public String getBuildImageTag() {
        return buildImageTag;
    }

    public void setBuildImageTag(String buildImageTag) {
        this.buildImageTag = buildImageTag;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setGitRepoClient(GitRepoClient gitRepoClient) {
        this.gitRepoClient = gitRepoClient;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public String getJenkinsJob() {
        return jenkinsJob;
    }

    public void setJenkinsJob(String jenkinsJob) {
        this.jenkinsJob = jenkinsJob;
    }

    public String getJenkinsMonitorView() {
        return jenkinsMonitorView;
    }

    public void setJenkinsMonitorView(String jenkinsMonitorView) {
        this.jenkinsMonitorView = jenkinsMonitorView;
    }

    public String getJenkinsPipelineView() {
        return jenkinsPipelineView;
    }

    public void setJenkinsPipelineView(String jenkinsPipelineView) {
        this.jenkinsPipelineView = jenkinsPipelineView;
    }

    public void setKubernetes(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;
    }

    public boolean isTriggerJenkinsJob() {
        return triggerJenkinsJob;
    }

    public void setTriggerJenkinsJob(boolean triggerJenkinsJob) {
        this.triggerJenkinsJob = triggerJenkinsJob;
    }

    public boolean isLetschatEnabled() {
        return letschatEnabled;
    }

    public void setLetschatEnabled(boolean letschatEnabled) {
        this.letschatEnabled = letschatEnabled;
    }

    public String getLetschatRoomExpression() {
        return letschatRoomExpression;
    }

    public void setLetschatRoomExpression(String letschatRoomExpression) {
        this.letschatRoomExpression = letschatRoomExpression;
    }

    public String getLetschatRoomLinkLabel() {
        return letschatRoomLinkLabel;
    }

    public void setLetschatRoomLinkLabel(String letschatRoomLinkLabel) {
        this.letschatRoomLinkLabel = letschatRoomLinkLabel;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getFabric8ConsoleNamespace() {
        return fabric8ConsoleNamespace;
    }

    public void setFabric8ConsoleNamespace(String fabric8ConsoleNamespace) {
        this.fabric8ConsoleNamespace = fabric8ConsoleNamespace;
    }

    public String getJenkinsNamespace() {
        return jenkinsNamespace;
    }

    public void setJenkinsNamespace(String jenkinsNamespace) {
        this.jenkinsNamespace = jenkinsNamespace;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRecreateMode() {
        return recreateMode;
    }

    public void setRecreateMode(boolean recreateMode) {
        this.recreateMode = recreateMode;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public boolean isTaigaAutoCreate() {
        return taigaAutoCreate;
    }

    public void setTaigaAutoCreate(boolean taigaAutoCreate) {
        this.taigaAutoCreate = taigaAutoCreate;
    }

    public boolean isTaigaEnabled() {
        return taigaEnabled;
    }

    public void setTaigaEnabled(boolean taigaEnabled) {
        this.taigaEnabled = taigaEnabled;
    }

    public String getTaigaProjectLinkLabel() {
        return taigaProjectLinkLabel;
    }

    public void setTaigaProjectLinkLabel(String taigaProjectLinkLabel) {
        this.taigaProjectLinkLabel = taigaProjectLinkLabel;
    }

    public String getTaigaProjectLinkPage() {
        return taigaProjectLinkPage;
    }

    public void setTaigaProjectLinkPage(String taigaProjectLinkPage) {
        this.taigaProjectLinkPage = taigaProjectLinkPage;
    }

    public String getTaigaProjectName() {
        return taigaProjectName;
    }

    public void setTaigaProjectName(String taigaProjectName) {
        this.taigaProjectName = taigaProjectName;
    }

    public String getTaigaProjectSlug() {
        return taigaProjectSlug;
    }

    public void setTaigaProjectSlug(String taigaProjectSlug) {
        this.taigaProjectSlug = taigaProjectSlug;
    }

    public String getTaigaTeamLinkLabel() {
        return taigaTeamLinkLabel;
    }

    public void setTaigaTeamLinkLabel(String taigaTeamLinkLabel) {
        this.taigaTeamLinkLabel = taigaTeamLinkLabel;
    }

    public String getTaigaTeamLinkPage() {
        return taigaTeamLinkPage;
    }

    public void setTaigaTeamLinkPage(String taigaTeamLinkPage) {
        this.taigaTeamLinkPage = taigaTeamLinkPage;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isTryLoadConfigFileFromRemoteGit() {
        return tryLoadConfigFileFromRemoteGit;
    }

    public void setTryLoadConfigFileFromRemoteGit(boolean tryLoadConfigFileFromRemoteGit) {
        this.tryLoadConfigFileFromRemoteGit = tryLoadConfigFileFromRemoteGit;
    }

    public ProjectConfig getProjectConfig() {
        return projectConfig;
    }

    public void setProjectConfig(ProjectConfig projectConfig) {
        this.projectConfig = projectConfig;
    }

    public void setRegisterWebHooks(boolean registerWebHooks) {
        this.registerWebHooks = registerWebHooks;
    }

    public boolean isRegisterWebHooks() {
        return registerWebHooks;
    }

    public String getIssueTrackerLabel() {
        return issueTrackerLabel;
    }

    public void setIssueTrackerLabel(String issueTrackerLabel) {
        this.issueTrackerLabel = issueTrackerLabel;
    }

    public String getIssueTrackerUrl() {
        return issueTrackerUrl;
    }

    public void setIssueTrackerUrl(String issueTrackerUrl) {
        this.issueTrackerUrl = issueTrackerUrl;
    }

    public String getTeamUrl() {
        return teamUrl;
    }

    public void setTeamUrl(String teamUrl) {
        this.teamUrl = teamUrl;
    }

    public String getTeamLabel() {
        return teamLabel;
    }

    public void setTeamLabel(String teamLabel) {
        this.teamLabel = teamLabel;
    }

    public String getReleasesUrl() {
        return releasesUrl;
    }

    public void setReleasesUrl(String releasesUrl) {
        this.releasesUrl = releasesUrl;
    }

    public String getReleasesLabel() {
        return releasesLabel;
    }

    public void setReleasesLabel(String releasesLabel) {
        this.releasesLabel = releasesLabel;
    }

    public String getRepositoryBrowseLabel() {
        return repositoryBrowseLabel;
    }

    public void setRepositoryBrowseLabel(String repositoryBrowseLabel) {
        this.repositoryBrowseLabel = repositoryBrowseLabel;
    }

    public String getRepositoryBrowseLink() {
        return repositoryBrowseLink;
    }

    public void setRepositoryBrowseLink(String repositoryBrowseLink) {
        this.repositoryBrowseLink = repositoryBrowseLink;
    }


    // Implementation methods
    //-------------------------------------------------------------------------

    protected Controller createController() {
        Controller controller = new Controller(getKubernetes());
        controller.setNamespace(namespace);
        controller.setThrowExceptionOnError(true);
        controller.setRecreateMode(recreateMode);
        return controller;
    }


    protected void loadConfigFile() {
        if (projectConfig == null) {
            GitRepoClient gitRepo = getGitRepoClient();
            boolean hasLocalConfig = false;
            if (basedir != null && basedir.isDirectory()) {
                projectConfig = ProjectConfigs.loadFromFolder(basedir);
                if (!projectConfig.isEmpty() || ProjectConfigs.hasConfigFile(basedir)) {
                    hasLocalConfig = true;
                }
            }
            if (!hasLocalConfig && tryLoadConfigFileFromRemoteGit && Strings.isNotBlank(repoName) && gitRepo != null) {
                try {
                    InputStream input = gitRepo.getRawFile(username, repoName, branch, ProjectConfigs.FILE_NAME);
                    if (input != null) {
                        try {
                            getLog().info("Parsing " + ProjectConfigs.FILE_NAME + " from the git repo " + repoName + " user " + username + " in branch " + branch);
                            projectConfig = ProjectConfigs.parseProjectConfig(input);
                        } catch (IOException e) {
                            getLog().warn("Failed to parse " + ProjectConfigs.FILE_NAME + " from the repo " + repoName + " for user " + username + " branch: " + branch + ". " + e, e);
                        }
                    }
                } catch (Exception e) {
                    getLog().warn("Failed to load " + ProjectConfigs.FILE_NAME + " from the repo " + repoName + " for user " + username + " branch: " + branch + ". " + e, e);
                }
            }
        }
        if (projectConfig != null) {
            String chatRoom = projectConfig.getChatRoom();
            if (Strings.isNotBlank(chatRoom)) {
                getLog().info("Found chat room: " + chatRoom);
                letschatRoomExpression = chatRoom;
            }
            String issueProjectName = projectConfig.getIssueProjectName();
            if (Strings.isNotBlank(issueProjectName)) {
                taigaProjectName = issueProjectName;
            }
        } else {
            getLog().info("No fabric8.yml file found for " + basedir);
        }
        if (Strings.isNullOrBlank(gitUrl)) {
            try {
                gitUrl = GitHelpers.extractGitUrl(basedir);
            } catch (IOException e) {
                getLog().warn("Could not load git URL from directory: " + e, e);
            }
        }
        if (Strings.isNullOrBlank(taigaProjectName)) {
            taigaProjectName = repoName;
        }
        if (Strings.isNullOrBlank(taigaProjectSlug)) {
            // TODO should we upper case it or anything?
            taigaProjectSlug = taigaProjectName;
        }
    }

    protected String getChatRoomLink(LetsChatClient letschat) {
        if (letschat != null) {
            try {
                String url = letschat.getAddress();
                String slug = evaluateRoomExpression(letschatRoomExpression);
                if (Strings.isNotBlank(url) && Strings.isNotBlank(slug)) {
                    RoomDTO room = letschat.getOrCreateRoom(slug);
                    if (room != null) {
                        String roomId = room.getId();
                        if (Strings.isNotBlank(roomId)) {
                            return URLUtils.pathJoin(url, "/#!/room/" + roomId);
                        }
                    }
                }
            } catch (Exception e) {
                getLog().error("Failed to get the link to the chat room: " + e, e);
            }
        }
        return null;
    }

    protected String evaluateRoomExpression(String roomExpresion) {
        if (Strings.isNotBlank(roomExpresion)) {
            String namespace = KubernetesHelper.defaultNamespace();
            String answer = roomExpresion;
            answer = replaceExpression(answer, "namespace", namespace);
            answer = replaceExpression(answer, "repoName", repoName);
            answer = replaceExpression(answer, "username", username);
            return answer;
        } else {
            return null;
        }
    }

    protected String replaceExpression(String text, String key, String value) {
        if (Strings.isNotBlank(key) && Strings.isNotBlank(value)) {
            String replace = "${" + key + "}";
            return text.replace(replace, value);
        } else {
            return text;
        }
    }

    protected LetsChatClient createLetsChat() {
        if (!letschatEnabled) {
            return null;
        }
        KubernetesClient kubernetes = getKubernetes();
        LetsChatClient letsChat = LetsChatKubernetes.createLetsChat(kubernetes);
        if (letsChat == null) {
            getLog().warn("No letschat service availble n kubernetes " + namespace + " on address: " + kubernetes.getMasterUrl());
            return null;
        }
        if (!letsChat.isValid()) {
            getLog().warn("No $" + LetsChatKubernetes.LETSCHAT_HUBOT_TOKEN + " environment variable defined so LetsChat support is disabled");
            return null;
        }
        return letsChat;
    }

    protected TaigaClient createTaiga() {
        if (!taigaEnabled) {
            return null;
        }
        TaigaClient taiga = TaigaKubernetes.createTaiga(getKubernetes(), namespace);
        if (taiga != null) {
            taiga.setAutoCreateProjects(taigaAutoCreate);
        }
        return taiga;
    }

    protected String getProjectPageLink(TaigaClient taiga, ProjectDTO taigaProject, String projectRelativePage) {
        if (taiga != null && taigaProject != null) {
            try {
                String url = taiga.getAddress();
                String slug = taigaProject.getSlug();
                if (Strings.isNullOrBlank(slug)) {
                    slug = taigaProjectSlug;
                }
                String userName = taiga.getUsername();
                if (Strings.isNullOrBlank(slug)) {
                    slug = userName + "-" + taigaProjectName;
                }
                if (Strings.isNotBlank(url) && Strings.isNotBlank(slug) && Strings.isNotBlank(projectRelativePage)) {
                    return URLUtils.pathJoin(url, "/project/", slug + "/", projectRelativePage);
                }
            } catch (Exception e) {
                getLog().error("Failed to get project page link for " + projectRelativePage + " : " + e, e);
            }
        }
        return null;
    }


    protected void createJenkinsJob(String buildName, String jenkinsJobUrl) {
        if (projectConfig != null) {
            String flow = projectConfig.getFlow();
            String flowGitUrlValue = null;
            boolean localFlow = false;
            if (Strings.isNotBlank(flow)) {
                flowGitUrlValue = this.flowGitUrl;
            } else if (projectConfig.isUseLocalFlow()) {
                flow = ProjectConfigs.LOCAL_FLOW_FILE_NAME;
                flowGitUrlValue = this.gitUrl;
                localFlow = true;
            }
            if (Strings.isNotBlank(flow) && Strings.isNotBlank(gitUrl) && Strings.isNotBlank(flowGitUrlValue)) {
                String template = loadJenkinsBuildTemplate(getLog());
                if (Strings.isNotBlank(template)) {
                    template = template.replace("${FLOW_PATH}", flow);
                    template = template.replace("${FLOW_GIT_URL}", flowGitUrlValue);
                    template = template.replace("${GIT_URL}", gitUrl);
                    if (localFlow) {
                        // lets remove the GIT_URL parameter
                        template = removeBuildParameter(getLog(), template, "GIT_URL");
                    }
                    postJenkinsBuild(buildName, template);
                }
            }
        }
    }

    public static String loadJenkinsBuildTemplate(Logger log) {
        String template = null;
        String templateName = "jenkinsBuildConfig.xml";
        URL url = DevOpsConnector.class.getResource(templateName);
        if (url == null) {
            log.error("Could not load " + templateName + " on the classpath!");
        } else {
            try {
                template = IOHelpers.loadFully(url);
            } catch (IOException e) {
                log.error("Failed to load template " + templateName + " from " + url + ". " + e, e);
            }
        }
        return template;
    }

    public static String removeBuildParameter(Logger log, String template, String parameterName) {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = documentBuilder.parse(new InputSource(new StringReader(template)));
            Element rootElement = doc.getDocumentElement();
            NodeList stringDefs = rootElement.getElementsByTagName("hudson.model.StringParameterDefinition");
            if (stringDefs != null) {
                for (int i = 0, size = stringDefs.getLength(); i < size; i++) {
                    Node item = stringDefs.item(i);
                    if (item instanceof Element) {
                        Element element = (Element) item;
                        Element name = DomHelper.firstChild(element, "name");
                        if (name != null) {
                            String textContent = name.getTextContent();
                            if (textContent != null) {
                                if (parameterName.equals(textContent.trim())) {
                                    Node parameterDefinitions = item.getParentNode();
                                    Node parametersDefinitionProperty = parameterDefinitions != null ? parameterDefinitions.getParentNode() : null;
                                    DomHelper.detach(item);
                                    if (DomHelper.firstChildElement(parameterDefinitions) == null) {
                                        DomHelper.detach(parameterDefinitions);
                                    }
                                    if (DomHelper.firstChildElement(parametersDefinitionProperty) == null) {
                                        DomHelper.detach(parametersDefinitionProperty);
                                    }
                                    return DomHelper.toXml(doc);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to remove the build parameter from the Jenkins XML. " + e, e);
        }
        return template;
    }

    protected void postJenkinsBuild(String jobName, String xml) {
        String address = getServiceUrl(ServiceNames.JENKINS, false, namespace, jenkinsNamespace);
        if (Strings.isNotBlank(address)) {
            String jobUrl = URLUtils.pathJoin(address, "/createItem") + "?name=" + jobName;

            getLog().info("POSTING the jenkins job to: " + jobUrl);
            getLog().debug("Jenkins XML: " + xml);

            String json = "{}";
            HttpURLConnection connection = null;
            try {
                URL url = new URL(jobUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "text/xml");
                connection.setDoOutput(true);

                OutputStreamWriter out = new OutputStreamWriter(
                        connection.getOutputStream());
                out.write(xml);

                out.close();
                int status = connection.getResponseCode();
                String message = connection.getResponseMessage();
                getLog().info("Got response code from Jenkins: " + status + " message: " + message);
                if (status != 200) {
                    getLog().error("Failed to trigger job " + jobName + " on " + jobUrl + ". Status: " + status + " message: " + message);
                }
            } catch (Exception e) {
                getLog().error("Failed to trigger jenkins on " + jobUrl + ". " + e, e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }


    }

    protected void createJenkinsWebhook(String jenkinsJobUrl) {
        if (Strings.isNotBlank(jenkinsJobUrl)) {
            String jenkinsWebHook = URLUtils.pathJoin(jenkinsJobUrl, "/build");
            Map<String,String> buildParameters = getBuildParameters();
            if (!buildParameters.isEmpty()) {
                String postfix = "";
                for (Map.Entry<String, String> entry : buildParameters.entrySet()) {
                    if (postfix.length() > 0) {
                        postfix += "&";
                    }
                    postfix += entry.getKey() + "=" + entry.getValue();
                }
                jenkinsWebHook += "WithParameters?" + postfix;
            }
            createWebhook(jenkinsWebHook, this.secret);
            
            if (triggerJenkinsJob) {
                triggerJenkinsWebHook(jenkinsWebHook, this.secret);
            }
        }
    }

    protected void triggerJenkinsWebHook(String jobUrl, String secret) {
        getLog().info("Triggering Jenkins webhook: " + jobUrl);
        String json = "{}";
        HttpURLConnection connection = null;
        try {
            URL url = new URL(jobUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            OutputStreamWriter out = new OutputStreamWriter(
                    connection.getOutputStream());
            out.write(json);

            out.close();
            int status = connection.getResponseCode();
            String message = connection.getResponseMessage();
            getLog().info("Got response code from Jenkins: " + status + " message: " + message);
            if (status != 200) {
                getLog().error("Failed to trigger job " + jobUrl + ". Status: " + status + " message: " + message);
            }
        } catch (Exception e) {
            getLog().error("Failed to trigger jenkins on " + jobUrl + ". " + e, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * If the build is parameterised lets return the build parameters
     */
    protected Map<String,String> getBuildParameters() {
        Map<String, String> answer = new HashMap<>();
        if (projectConfig != null) {
            String flow = projectConfig.getFlow();
            if (flow != null && Strings.isNotBlank(gitUrl)) {
                answer.put("GIT_URL", gitUrl);
            }
            Map<String, String> parameters = projectConfig.getBuildParameters();
            if (parameters != null) {
                answer.putAll(parameters);
            }
            Map<String, String> environments = projectConfig.getEnvironments();
            if (environments != null) {
                Set<Map.Entry<String, String>> entries = environments.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    String paramName = key.toUpperCase() + "_NAMESPACE";
                    answer.put(paramName, value);
                }
            }
            if (!answer.containsKey("VERSION_PREFIX")) {
                answer.put("VERSION_PREFIX", "1.0");
            }
        }
        return answer;
    }

    protected ProjectDTO createTaigaProject(TaigaClient taiga) {
        if (taiga != null) {
            if (Strings.isNullOrBlank(taigaProjectName)) {
                getLog().info("Not creating Taiga project as no `fabric8.tagiaProjectName` property specified");
                return null;
            }
            if (Strings.isNullOrBlank(taigaProjectSlug)) {
                getLog().info("Not creating Taiga project as no `fabric8.taigaProjectSlug` property specified");
                return null;
            }
            getLog().info("About to create Taiga project " + taigaProjectName + " with slug: " + taigaProjectSlug);
            return taiga.getOrCreateProject(taigaProjectName, taigaProjectSlug);
        }
        return null;
    }


    protected void createGerritRepo(String repoName, String gerritUser, String gerritPwd, String gerritGitInitialCommit, String gerritGitRepoDesription) throws Exception {

        // lets add defaults if not env vars
        if (Strings.isNullOrBlank(gerritUser)) {
            gerritUser = "admin";
        }
        if (Strings.isNullOrBlank(gerritPwd)) {
            gerritPwd = "secret";
        }

        log.info("A Gerrit git repo will be created for this name : " + repoName);

        String gerritAddress = KubernetesHelper.getServiceURL(kubernetes,ServiceNames.GERRIT, namespace, "http", true);
        log.info("Found gerrit address: " + gerritAddress + " for namespace: " + namespace + " on Kubernetes address: " + kubernetes.getMasterUrl());
        
        if (Strings.isNullOrBlank(gerritAddress)) {
            throw new Exception("No address for service " + ServiceNames.GERRIT + " in namespace: "
                    + namespace + " on Kubernetes address: " + kubernetes.getMasterUrl());
        }
        log.info("Querying Gerrit for namespace: " + namespace + " on Kubernetes address: " + kubernetes.getMasterUrl());

        List<Object> providers = WebClients.createProviders();
        providers.add(new RemovePrefix());
        
        WebClient webClient = WebClient.create(gerritAddress, providers);
        disableSslChecks(webClient);
        configureUserAndPassword(webClient, gerritUser, gerritPwd);
        enableDigestAuthenticaionType(webClient);
        GitApi gitApi = JAXRSClientFactory.fromClient(webClient, GitApi.class);

        // Check first if a Git repo already exists in Gerrit
        ProjectInfoDTO project = null;
        try {
            project = gitApi.getRepository(repoName);
        } catch (WebApplicationException ex) {
            // If we get Response Status = 404, then no repo exists. So we can create it
            if (ex.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                CreateRepositoryDTO createRepoDTO = new CreateRepositoryDTO();
                createRepoDTO.setDescription(gerritGitRepoDesription);
                createRepoDTO.setName(repoName);
                createRepoDTO.setCreate_empty_commit(Boolean.valueOf(gerritGitInitialCommit));

                RepositoryDTO repository = gitApi.createRepository(repoName, createRepoDTO);

                if (log.isDebugEnabled()) {
                    log.debug("Git Repo created : " + toJson(repository));
                }
                log.info("Created git repo for " + repoName + " for namespace: " + namespace + " on gogs URL: " + gerritAddress);
            }
        }

        if ((project != null) && (project.getName().equals(repoName))) {
            throw new Exception("Repository " + repoName + " already exists !");
        }  
    }

    @Priority(value = 1000)
    protected static class RemovePrefix implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext interceptorContext)
                throws IOException, WebApplicationException {
            InputStream in = interceptorContext.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder received = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                received.append(line);
            }

            String s = received.toString();
            s = s.replace(JSON_MAGIC,"");

            System.out.println("Reader Interceptor removing the prefix invoked.");
            System.out.println("Content cleaned : " + s);

            String responseContent = new String(s);
            interceptorContext.setInputStream(new ByteArrayInputStream(
                    responseContent.getBytes()));

            return interceptorContext.proceed();
        }
    }
    
    protected void createTaigaWebhook(TaigaClient taiga, ProjectDTO project) {
        if (taiga != null && project != null) {
            Long projectId = project.getId();
            ModuleDTO module = taiga.moduleForProject(projectId, TaigaModule.GOGS);
            if (module != null) {
                String webhookSecret = module.getSecret();
                String webhook = taiga.getPublicWebhookUrl(module);
                if (Strings.isNotBlank(webhookSecret) && Strings.isNotBlank(webhook)) {
                    createWebhook(webhook, webhookSecret);
                } else {
                    getLog().warn("Could not create webhook for Taiga. Missing module data for url: " + webhook + " secret: " + webhookSecret);
                }
            } else {
                getLog().warn("No module for gogs so cannot create Taiga webhook");
            }
        }
    }

    protected void createWebhook(String url, String webhookSecret) {
        try {
            GitRepoClient gitRepoClient = getGitRepoClient();
            WebHooks.createGogsWebhook(gitRepoClient, getLog(), username, repoName, url, webhookSecret);
        } catch (Exception e) {
            getLog().error("Failed to create webhook " + url + " on repository " + repoName + ". Reason: " + e, e);
        }
    }

}
