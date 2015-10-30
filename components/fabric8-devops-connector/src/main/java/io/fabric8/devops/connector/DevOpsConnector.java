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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Challenge;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import io.fabric8.devops.ProjectConfig;
import io.fabric8.devops.ProjectConfigs;
import io.fabric8.devops.ProjectRepositories;
import io.fabric8.gerrit.CreateRepositoryDTO;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.kubernetes.api.builders.ListEnvVarBuilder;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.letschat.LetsChatClient;
import io.fabric8.letschat.LetsChatKubernetes;
import io.fabric8.letschat.RoomDTO;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigSpec;
import io.fabric8.openshift.api.model.BuildSource;
import io.fabric8.openshift.api.model.BuildStrategy;
import io.fabric8.openshift.api.model.BuildTriggerPolicy;
import io.fabric8.openshift.api.model.BuildTriggerPolicyBuilder;
import io.fabric8.openshift.api.model.CustomBuildStrategy;
import io.fabric8.openshift.api.model.GitBuildSource;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.GitRepoKubernetes;
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
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static io.fabric8.kubernetes.client.utils.Utils.isNotNullOrEmpty;

import static io.fabric8.kubernetes.api.KubernetesHelper.getOrCreateMetadata;

/**
 * Updates a project's connections to its various DevOps resources like issue tracking, chat and jenkins builds
 */
public class DevOpsConnector {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private transient Logger log = LoggerFactory.getLogger(DevOpsConnector.class);

    private static final String JSON_MAGIC = ")]}'";
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
    private String gerritUser = Systems.getEnvVar("GERRIT_ADMIN_USER", "admin");
    private String gerritPwd = Systems.getEnvVar("GERRIT_ADMIN_PWD", "secret");
    private String gerritGitInitialCommit = Systems.getEnvVar("GERRIT_INITIAL_COMMIT", "false");
    private String gerritGitRepoDesription = Systems.getEnvVar("GERRIT_REPO_DESCRIPTION", "Description of the gerrit git repo");

    private boolean recreateMode;
    private String namespace = KubernetesHelper.defaultNamespace();
    private String projectName;

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
    private String jenkinsJobName;
    private String gitSourceSecretName;


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
     *
     * @throws Exception
     */
    public void execute() throws Exception {
        loadConfigFile();
        KubernetesClient kubernetes = getKubernetes();

        String name = projectName;
        if (Strings.isNullOrBlank(name)) {
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
        }
        if (Strings.isNullOrBlank(projectName)) {
            projectName = name;
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
        if (projectConfig != null && projectConfig.hasCodeReview()) {
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
        Controller controller = createController();
        OpenShiftClient openShiftClient = getKubernetes().adapt(OpenShiftClient.class);
        BuildConfig buildConfig = null;
        try {
            buildConfig = openShiftClient.inNamespace(namespace).buildConfigs().withName(projectName).get();
        } catch (Exception e) {
            log.error("Failed to load build config for " + namespace + "/" + projectName + ". " + e, e);
        }
        log.info("Loaded build config for " + namespace + "/" + projectName  + " " + buildConfig);

        // if we have loaded a build config then lets assume its correct!
        boolean foundExistingGitUrl = false;
        if (buildConfig != null) {
            BuildConfigSpec spec = buildConfig.getSpec();
            if (spec != null) {
                BuildSource source = spec.getSource();
                if (source != null) {
                    GitBuildSource git = source.getGit();
                    if (git != null) {
                        gitUrl = git.getUri();
                        log.info("Loaded existing BuildConfig git url: " + gitUrl);
                        foundExistingGitUrl = true;
                    }
                    LocalObjectReference sourceSecret = source.getSourceSecret();
                    if (sourceSecret != null) {
                        gitSourceSecretName = sourceSecret.getName();
                    }
                }
            }
            if (!foundExistingGitUrl) {
                log.warn("Could not find a git url in the loaded BuildConfig: " + buildConfig);
            }
            log.info("Loaded gitSourceSecretName: " + gitSourceSecretName);
        }





        if (buildConfig == null) {
            buildConfig = new BuildConfig();
        }
        ObjectMeta metadata = getOrCreateMetadata(buildConfig);
        metadata.setName(projectName);
        metadata.setAnnotations(annotations);
        metadata.setLabels(labels);
        BuildConfigSpec spec = buildConfig.getSpec();
        if (spec == null) {
            spec = new BuildConfigSpec();
            buildConfig.setSpec(spec);
        }

        log.info("gitUrl is: " + gitUrl);
        if (!foundExistingGitUrl && Strings.isNotBlank(gitUrl)) {
            BuildSource source = spec.getSource();
            if (source == null) {
                source = new BuildSource();
                spec.setSource(source);
            }
            source.setType("Git");
            GitBuildSource git = source.getGit();
            if (git == null) {
                git = new GitBuildSource();
                source.setGit(git);
            }
            git.setUri(gitUrl);
        }

        if (Strings.isNotBlank(buildImageStream) && Strings.isNotBlank(buildImageTag)) {
            BuildStrategy strategy = spec.getStrategy();
            if (strategy == null) {
                strategy = new BuildStrategy();
                spec.setStrategy(strategy);
            }

            // TODO only do this if we are using Jenkins?
            strategy.setType("Custom");
            CustomBuildStrategy customStrategy = strategy.getCustomStrategy();
            if (customStrategy == null) {
                customStrategy = new CustomBuildStrategy();
                strategy.setCustomStrategy(customStrategy);
            }

            ListEnvVarBuilder envBuilder = new ListEnvVarBuilder();
            envBuilder.withEnvVar("BASE_URI", jenkinsUrl);
            envBuilder.withEnvVar("JOB_NAME", name);
            customStrategy.setEnv(envBuilder.build());
            customStrategy.setFrom(new ObjectReferenceBuilder().withKind("DockerImage").withName(s2iCustomBuilderImage).build());
        }
        List<BuildTriggerPolicy> triggers = spec.getTriggers();
        if (triggers.isEmpty()) {
            triggers.add(new BuildTriggerPolicyBuilder().withType("GitHub").withNewGithub().withSecret(secret).endGithub().build());
            triggers.add(new BuildTriggerPolicyBuilder().withType("Generic").withNewGeneric().withSecret(secret).endGeneric().build());
            spec.setTriggers(triggers);
        }

        try {
            getLog().info("About to apply build config: " + new JSONObject(KubernetesHelper.toJson(buildConfig)).toString(4));
            controller.applyBuildConfig(buildConfig, "maven");

            getLog().info("Created build configuration for " + name + " in namespace: " + controller.getNamespace() + " at " + kubernetes.getMasterUrl());
        } catch (Exception e) {
            getLog().error("Failed to create BuildConfig for " + KubernetesHelper.toJson(buildConfig) + ". " + e, e);
        }
        this.jenkinsJobName = name;
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
        String[] defaults = {KubernetesHelper.defaultNamespace(), "default"};
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
        if (Strings.isNotBlank(jenkinsJobName)) {
            createJenkinsJob(jenkinsJobName, jenkinsJobUrl);
            getLog().info("created jenkins job");
        }
        if (Strings.isNotBlank(jenkinsJobUrl) && Strings.isNotBlank(jenkinsJobName)) {
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
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
            String flow = projectConfig.getPipeline();
            String flowGitUrlValue = null;
            boolean localFlow = false;
            String projectGitUrl = convertGitUrlToHttpFromSsh(this.gitUrl);
            if (Strings.isNotBlank(flow)) {
                flowGitUrlValue = this.flowGitUrl;
            } else if (projectConfig.isUseLocalFlow()) {
                flow = ProjectConfigs.LOCAL_FLOW_FILE_NAME;
                flowGitUrlValue = projectGitUrl;
                localFlow = true;
            } else {
                getLog().info("Not creating Jenkins job as no pipeline defined for project configuration!");
            }
            String versionPrefix = Systems.getSystemPropertyOrEnvVar("VERSION_PREFIX", "VERSION_PREFIX", "1.0");
            if (Strings.isNotBlank(flow) && Strings.isNotBlank(projectGitUrl) && Strings.isNotBlank(flowGitUrlValue)) {
                String template = loadJenkinsBuildTemplate(getLog());
                if (Strings.isNotBlank(template)) {
                    if (Strings.isNotBlank(gitSourceSecretName)) {
                        template = addBuildParameter(getLog(), template, "SOURCE_SECRET", gitSourceSecretName, "Name of the Kubernetes Secret required to clone the git repository");
                    }
                    template = template.replace("${FLOW_PATH}", flow);
                    template = template.replace("${FLOW_GIT_URL}", flowGitUrlValue);
                    template = template.replace("${GIT_URL}", projectGitUrl);
                    template = template.replace("${VERSION_PREFIX}", versionPrefix);
                    if (localFlow) {
                        // lets remove the GIT_URL parameter
                        template = removeBuildParameter(getLog(), template, "GIT_URL");
                    }
                    postJenkinsBuild(buildName, template);
                }
            }
        }
    }

    /**
     * Jenkins can't clone yet git URLs using openshift secrets so lets switch to https for now for CI builds
     */
    protected String convertGitUrlToHttpFromSsh(String gitUrl) {
        if (Strings.isNotBlank(gitUrl)) {
            String prefix = "git@";
            if (gitUrl.startsWith(prefix)) {
                String remaining = gitUrl.substring(prefix.length());
                remaining = remaining.replace(":", "/");
                return  "https://" + remaining;
            }
        }
        return gitUrl;
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

    public static String addBuildParameter(Logger log, String template, String parameterName, String parameterValue, String description) {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = documentBuilder.parse(new InputSource(new StringReader(template)));
            Element rootElement = doc.getDocumentElement();
            NodeList parameterDefs = rootElement.getElementsByTagName("parameterDefinitions");
            if (parameterDefs != null && parameterDefs.getLength() > 0) {
                Node paramDefNode = parameterDefs.item(0);
                Element stringParamDef = DomHelper.addChildElement(paramDefNode, "hudson.model.StringParameterDefinition");

                DomHelper.addChildElement(stringParamDef, "name", parameterName);
                DomHelper.addChildElement(stringParamDef, "defaultValue", parameterValue);

                if (Strings.isNotBlank(description)) {
                    DomHelper.addChildElement(stringParamDef, "description", description);
                }
            } else {
                log.warn("Could not find the <parameterDefinitions> to add the build parameter name " + parameterName + " with value: " + parameterValue);
            }
        } catch (Exception e) {
            log.error("Failed to add the build parameter from the Jenkins XML. " + e, e);
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
                    getLog().error("Failed to register job " + jobName + " on " + jobUrl + ". Status: " + status + " message: " + message);
                }
            } catch (Exception e) {
                getLog().error("Failed to register jenkins on " + jobUrl + ". " + e, e);
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
            Map<String, String> buildParameters = getBuildParameters();
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
                triggerJenkinsWebHook(jenkinsJobUrl, jenkinsWebHook, this.secret);
            }
        }
    }

    protected void triggerJenkinsWebHook(String jobUrl, String triggerUrl, String secret) {
        // lets check if this build is already running in which case do nothing
        String lastBuild = URLUtils.pathJoin(jobUrl, "/lastBuild/api/json");
        JsonNode lastBuildJson = parseLastBuildJson(lastBuild);
        JsonNode building = null;
        if (lastBuildJson != null && lastBuildJson.isObject()) {
            building = lastBuildJson.get("building");
            if (building != null && building.isBoolean()) {
                if (building.booleanValue()) {
                    getLog().info("Build is already running so lets not trigger another one!");
                    return;
                }
            }
        }
        getLog().info("Got last build JSON: " + lastBuildJson + " building: " + building);


        getLog().info("Triggering Jenkins webhook: " + triggerUrl);
        String json = "{}";
        HttpURLConnection connection = null;
        try {
            URL url = new URL(triggerUrl);
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
                getLog().error("Failed to trigger job " + triggerUrl + ". Status: " + status + " message: " + message);
            }
        } catch (Exception e) {
            getLog().error("Failed to trigger jenkins on " + triggerUrl + ". " + e, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    protected JsonNode parseLastBuildJson(String urlText) {
        HttpURLConnection connection = null;
        String message = null;
        try {
            URL url = new URL(urlText);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");

            int status = connection.getResponseCode();
            message = connection.getResponseMessage();
            getLog().info("Got response code from URL: " + url +" " + status + " message: " + message);
            if (status != 200 || Strings.isNullOrBlank(message)) {
                getLog().debug("Failed to load URL " + url + ". Status: " + status + " message: " + message);
            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.reader().readTree(message);
            }
        } catch (Exception e) {
            getLog().debug("Failed to load URL " + urlText + ". " + e, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }

    /**
     * If the build is parameterised lets return the build parameters
     */
    protected Map<String, String> getBuildParameters() {
        Map<String, String> answer = new HashMap<>();
        if (projectConfig != null) {
            String flow = projectConfig.getPipeline();
            if (flow != null && Strings.isNotBlank(gitUrl)) {
                answer.put("GIT_URL", gitUrl);
            }
            Map<String, String> parameters = projectConfig.getBuildParameters();
            if (parameters != null) {
                answer.putAll(parameters);
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


    protected void createGerritRepo(String repoName, String gerritUser, String gerritPwd, String gerritGitInitialCommit, String gerritGitRepoDescription) throws Exception {

        // lets add defaults if not env vars
        final String username = Strings.isNullOrBlank(gerritUser) ? "admin" : gerritUser;
        final String password = Strings.isNullOrBlank(gerritPwd) ? "secret" : gerritPwd;

        log.info("A Gerrit git repo will be created for this name : " + repoName);

        String gerritAddress = KubernetesHelper.getServiceURL(kubernetes, ServiceNames.GERRIT, namespace, "http", true);
        log.info("Found gerrit address: " + gerritAddress + " for namespace: " + namespace + " on Kubernetes address: " + kubernetes.getMasterUrl());

        if (Strings.isNullOrBlank(gerritAddress)) {
            throw new Exception("No address for service " + ServiceNames.GERRIT + " in namespace: "
                    + namespace + " on Kubernetes address: " + kubernetes.getMasterUrl());
        }

        String GERRIT_URL = gerritAddress + "/a/projects/" + repoName;

        OkHttpClient client = new OkHttpClient();
        if (isNotNullOrEmpty(gerritUser) && isNotNullOrEmpty(gerritPwd)) {
            client.setAuthenticator(new Authenticator() {
                @Override
                public Request authenticate(Proxy proxy, Response response) throws IOException {
                    List<Challenge> challenges = response.challenges();
                    Request request = response.request();
                    for (int i = 0, size = challenges.size(); i < size; i++) {
                        Challenge challenge = challenges.get(i);
                        if (!"Basic".equalsIgnoreCase(challenge.getScheme())) continue;

                        String credential = Credentials.basic(username, password);
                        return request.newBuilder()
                                .header("Authorization", credential)
                                .build();
                    }
                    return null;
                }

                @Override
                public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
                    return null;
                }
            });
        }

        System.out.println("Requesting : " + GERRIT_URL);

        try {
            CreateRepositoryDTO createRepoDTO = new CreateRepositoryDTO();
            createRepoDTO.setDescription(gerritGitRepoDescription);
            createRepoDTO.setName(repoName);
            createRepoDTO.setCreate_empty_commit(Boolean.valueOf(gerritGitInitialCommit));

            RequestBody body = RequestBody.create(JSON, MAPPER.writeValueAsString(createRepoDTO));
            Request request = new Request.Builder().post(body).url(GERRIT_URL).build();
            Response response = client.newCall(request).execute();
            System.out.println("responseBody : " + response.body().string());

        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            if (client != null && client.getConnectionPool() != null) {
                client.getConnectionPool().evictAll();
            }
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
            s = s.replace(JSON_MAGIC, "");

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
        // TODO we should only register a webhook if either git + other system is on premise or if its all online
        // e.g. we shouldn't try to register webhooks on public github with on premise services
        try {
            GitRepoClient gitRepoClient = getGitRepoClient();
            WebHooks.createGogsWebhook(gitRepoClient, getLog(), username, repoName, url, webhookSecret);
        } catch (Exception e) {
            getLog().error("Failed to create webhook " + url + " on repository " + repoName + ". Reason: " + e, e);
        }
    }

}
