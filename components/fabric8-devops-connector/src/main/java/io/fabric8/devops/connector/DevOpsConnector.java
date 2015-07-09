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
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.letschat.LetsChatClient;
import io.fabric8.letschat.LetsChatKubernetes;
import io.fabric8.letschat.RoomDTO;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildConfigFluent;
import io.fabric8.repo.git.GitRepoClient;
import io.fabric8.repo.git.GitRepoKubernetes;
import io.fabric8.taiga.ModuleDTO;
import io.fabric8.taiga.ProjectDTO;
import io.fabric8.taiga.TaigaClient;
import io.fabric8.taiga.TaigaKubernetes;
import io.fabric8.taiga.TaigaModule;
import io.fabric8.utils.GitHelpers;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Strings;
import io.fabric8.utils.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Updates a project's connections to its various DevOps resources like issue tracking, chat and jenkins builds
 */
public class DevOpsConnector {
    private transient Logger log = LoggerFactory.getLogger(DevOpsConnector.class);

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
    private String jenkinsJob;

    private String jenkinsMonitorView;
    private String jenkinsPipelineView;
    private String taigaProjectName;
    private String taigaProjectSlug;
    private String taigaProjectLinkPage = "backlog";
    private String taigaProjectLinkLabel = "Backlog";

    private String taigaTeamLinkPage = "team";
    private String taigaTeamLinkLabel = "Team";
    private boolean taigaAutoCreate = true;
    private boolean taigaEnabled = true;

    private boolean letschatEnabled = true;
    private String letschatRoomLinkLabel = "Room";
    private String letschatRoomExpression = "fabric8_${namespace}";

    private String flowGitUrl = System.getenv("JENKINS_WORKFLOW_GIT_REPOSITORY");

    private boolean recreateMode;
    private String namespace;
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
            name = repoName;
            if (Strings.isNotBlank(username)) {
                name = username + "-" + name;
            }
            if (projectConfig != null) {
                projectConfig.setBuildName(name);
            }
        }
        Map<String, String> labels = new HashMap<>();
        labels.put("user", username);
        labels.put("repo", repoName);

        taiga = null;
        taigaProject = null;
        try {
            taiga = createTaiga();
            taigaProject = createTaigaProject(taiga);
        } catch (Exception e) {
            getLog().error("Failed to load or lazily create the Taiga project: " + e, e);
        }

        LetsChatClient letschat = null;
        try {
            letschat = createLetsChat();
        } catch (Exception e) {
            getLog().error("Failed to load or lazily create the LetsChat client: " + e, e);
        }

        Map<String, String> annotations = new HashMap<>();
        jenkinsJobUrl = null;
        try {
            String jenkinsUrl = kubernetes.getServiceURL(ServiceNames.JENKINS, kubernetes.getNamespace(), "http", true);

            if (Strings.isNotBlank(jenkinsUrl)) {
                if (Strings.isNotBlank(jenkinsMonitorView)) {
                    String url = URLUtils.pathJoin(jenkinsUrl, "/view", jenkinsMonitorView);
                    annotations.put("fabric8.link.jenkins.monitor/url", url);
                    String label = "Monitor";
                    annotations.put("fabric8.link.jenkins.monitor/label", label);
                    addLink(label, url);
                }
                if (Strings.isNotBlank(jenkinsPipelineView)) {
                    String url = URLUtils.pathJoin(jenkinsUrl, "/view", jenkinsPipelineView);
                    annotations.put("fabric8.link.jenkins.pipeline/url", url);
                    String label = "Pipeline";
                    annotations.put("fabric8.link.jenkins.pipeline/label", label);
                    addLink(label, url);
                }
                if (Strings.isNotBlank(name)) {
                    jenkinsJobUrl = URLUtils.pathJoin(jenkinsUrl, "/job", name);
                    annotations.put("fabric8.link.jenkins.job/url", jenkinsJobUrl);
                    String label = "Job";
                    annotations.put("fabric8.link.jenkins.job/label", label);
                    addLink(label, jenkinsJobUrl);
                }
            }
        } catch (Exception e) {
            getLog().warn("Could not find the Jenkins URL!: " + e, e);
        }

        String taigaLink = getProjectPageLink(taiga, taigaProject, this.taigaProjectLinkPage);
        if (Strings.isNotBlank(taigaLink)) {
            annotations.put("fabric8.link.taiga/url", taigaLink);
            annotations.put("fabric8.link.taiga/label", taigaProjectLinkLabel);
            addLink(taigaProjectLinkLabel, taigaLink);
        }
        String taigaTeamLink = getProjectPageLink(taiga, taigaProject, this.taigaTeamLinkPage);
        if (Strings.isNotBlank(taigaTeamLink)) {
            annotations.put("fabric8.link.taiga.team/url", taigaTeamLink);
            annotations.put("fabric8.link.taiga.team/label", taigaTeamLinkLabel);
            addLink(taigaTeamLinkLabel, taigaTeamLink);
        }

        String chatRoomLink = getChatRoomLink(letschat);
        if (Strings.isNotBlank(chatRoomLink)) {
            annotations.put("fabric8.link.letschat.room/url", chatRoomLink);
            annotations.put("fabric8.link.letschat.room/label", letschatRoomLinkLabel);
            addLink(letschatRoomLinkLabel, chatRoomLink);
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
            specBuilder = specBuilder.
                    withNewStrategy().
                    withType("Docker").withNewDockerStrategy().withNewFrom().withName(buildImageStream + ":" + buildImageTag).endFrom().endDockerStrategy().
                    endStrategy();
        }
        BuildConfig buildConfig = specBuilder.
                addNewTrigger().
                withType("github").withNewGithub().withSecret(secret).endGithub().
                endTrigger().
                addNewTrigger().
                withType("generic").withNewGeneric().withSecret(secret).endGeneric().
                endTrigger().
                endSpec().
                build();

        Controller controller = createController();
        try {
            controller.applyBuildConfig(buildConfig, "maven");
            getLog().info("Created build configuration for " + name + " in namespace: " + controller.getNamespace() + " at " + kubernetes.getAddress());
        } catch (Exception e) {
            getLog().error("Failed to create BuildConfig for " + KubernetesHelper.toJson(buildConfig) + ". " + e, e);
        }
        if (Strings.isNotBlank(name)) {
            createJenkinsJob(name, jenkinsJobUrl);
        }
        if (isRegisterWebHooks()) {
            registerWebHooks();
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
            kubernetes = new KubernetesClient();
        }
        if (Strings.isNotBlank(namespace)) {
            kubernetes.setNamespace(namespace);
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


    // Implementation methods
    //-------------------------------------------------------------------------

    protected Controller createController() {
        Controller controller = new Controller(getKubernetes());
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
            String namespace = getKubernetes().getNamespace();
            if (namespace == null) {
                namespace = KubernetesClient.defaultNamespace();
            }
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
            getLog().warn("No letschat service availble n kubernetes " + kubernetes.getNamespace() + " on address: " + kubernetes.getAddress());
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
        TaigaClient taiga = TaigaKubernetes.createTaiga(getKubernetes());
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
            if (Strings.isNotBlank(flow) && Strings.isNotBlank(gitUrl) && Strings.isNotBlank(flowGitUrl)) {
                String templateName = "jenkinsBuildConfig.xml";
                URL url = getClass().getResource(templateName);
                if (url == null) {
                    getLog().error("Could not load " + templateName + " on the classpath!");
                } else {
                    String template = null;
                    try {
                        template = IOHelpers.loadFully(url);
                    } catch (IOException e) {
                        getLog().error("Failed to load template " + templateName + " from " + url + ". " + e, e);
                    }
                    if (Strings.isNotBlank(template)) {
                        template = template.replace("${FLOW_PATH}", flow);
                        template = template.replace("${FLOW_GIT_URL}", flowGitUrl);
                        template = template.replace("${GIT_URL}", gitUrl);

                        postJenkinsBuild(buildName, template);
                    }
                }
            }
        }
    }

    protected void postJenkinsBuild(String jobName, String xml) {
        String address = kubernetes.getServiceURL(ServiceNames.JENKINS, kubernetes.getNamespace(), "http", false);
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
            
            triggerJenkinsWebHook(jenkinsWebHook, this.secret);
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
