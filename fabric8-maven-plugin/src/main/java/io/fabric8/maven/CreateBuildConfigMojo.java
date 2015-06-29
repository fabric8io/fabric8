/*
 * Copyright 2005-2015 Red Hat, Inc.                                    
 *                                                                      
 * Red Hat licenses this file to you under the Apache License, version  
 * 2.0 (the "License"); you may not use this file except in compliance  
 * with the License.  You may obtain a copy of the License at           
 *                                                                      
 *    http://www.apache.org/licenses/LICENSE-2.0                        
 *                                                                      
 * Unless required by applicable law or agreed to in writing, software  
 * distributed under the License is distributed on an "AS IS" BASIS,    
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or      
 * implied.  See the License for the specific language governing        
 * permissions and limitations under the License.
 */
package io.fabric8.maven;

import io.fabric8.devops.ProjectConfig;
import io.fabric8.devops.YamlHelper;
import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.ServiceNames;
import io.fabric8.letschat.LetsChatClient;
import io.fabric8.letschat.LetsChatKubernetes;
import io.fabric8.letschat.RoomDTO;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildConfigFluent;
import io.fabric8.taiga.ModuleDTO;
import io.fabric8.taiga.ProjectDTO;
import io.fabric8.taiga.TaigaClient;
import io.fabric8.taiga.TaigaKubernetes;
import io.fabric8.taiga.TaigaModule;
import io.fabric8.utils.GitHelpers;
import io.fabric8.utils.Strings;
import io.fabric8.utils.URLUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates an OpenShift BuildConfig object for a
 */
@Mojo(name = "create-build-config", requiresProject = false)
public class CreateBuildConfigMojo extends AbstractNamespacedMojo {

    /**
     * the current folder
     */
    @Parameter(defaultValue = "${basedir}")
    protected File basedir;

    /**
     * the optional fabric8.yaml file to override configuration
     */
    @Parameter(property = "fabric8.configFile", defaultValue = "${basedir}/fabric8.yml")
    protected File projectConfigFile;

    /**
     * the gogs user name to use
     */
    @Parameter(property = "fabric8.gogsUsername")
    protected String username;

    /**
     * the gogs password
     */
    @Parameter(property = "fabric8.gogsPassword")
    protected String password;

    /**
     *
     */
    @Parameter(property = "fabric8.repoName", defaultValue = "${project.artifactId}")
    protected String repoName;

    /**
     *
     */
    @Parameter(property = "fabric8.fullName")
    protected String fullName;

    /**
     *
     */
    @Parameter(property = "fabric8.gitUrl")
    protected String gitUrl;

    /**
     * The webhook secret used for generic and github webhooks
     */
    @Parameter(property = "fabric8.webhookSecret", defaultValue = "secret101")
    protected String secret;

    /**
     * the build image stream name
     */
    @Parameter(property = "fabric8.buildImageStream", defaultValue = "triggerJenkins")
    protected String buildImageStream;

    /**
     * the build image stream tag
     */
    @Parameter(property = "fabric8.buildImageTag", defaultValue = "latest")
    protected String buildImageTag;

    /**
     * The name of the jenkins job to link to as the first job in the pipeline
     */
    @Parameter(property = "fabric8.jenkinsJob")
    protected String jenkinsJob;

    /**
     * The name of the jenkins monitor view
     */
    @Parameter(property = "fabric8.jenkinsMonitorView")
    protected String jenkinsMonitorView;

    /**
     * The name of the jenkins pipline view
     */
    @Parameter(property = "fabric8.jenkinsPipelineView")
    protected String jenkinsPipelineView;

    /**
     * The name of the taiga project name to use
     */
    @Parameter(property = "fabric8.tagiaProjectName", defaultValue = "${fabric8.repoName}")
    protected String taigaProjectName;

    /**
     * The slug name of the project in Taiga or will be auto-generated from the user and project name if not configured
     */
    @Parameter(property = "fabric8.taigaProjectSlug")
    protected String taigaProjectSlug;

    /**
     * The project page to link to
     */
    @Parameter(property = "fabric8.taigaProjectLinkPage", defaultValue = "backlog")
    protected String taigaProjectLinkPage;

    /**
     * The label for the issue tracker/kanban/scrum taiga project link
     */
    @Parameter(property = "fabric8.taigaProjectLinkLabel", defaultValue = "Backlog")
    protected String taigaProjectLinkLabel;

    /**
     * The team page to link to
     */
    @Parameter(property = "fabric8.taigaTeamLinkPage", defaultValue = "team")
    protected String taigaTeamLinkPage;

    /**
     * The label for the team page
     */
    @Parameter(property = "fabric8.taigaTeamLinkLabel", defaultValue = "Team")
    protected String taigaTeamLinkLabel;

    /**
     * Should we auto-create projects in taiga if they are missing?
     */
    @Parameter(property = "fabric8.taigaAutoCreate", defaultValue = "true")
    protected boolean taigaAutoCreate;

    /**
     * Should we enable Taiga integration
     */
    @Parameter(property = "fabric8.taigaEnabled", defaultValue = "true")
    protected boolean taigaEnabled;

    /**
     * Should we enable LetsChat integration if the
     * {@link LetsChatKubernetes#LETSCHAT_HUBOT_TOKEN} environment variable is enabled
     */
    @Parameter(property = "fabric8.letschatEnabled", defaultValue = "true")
    protected boolean letschatEnabled;

    /**
     * The label for the chat room page
     */
    @Parameter(property = "fabric8.letschatRoomLinkLabel", defaultValue = "Room")
    protected String letschatRoomLinkLabel;

    /**
     * The expression used to define the room name for this project; using expressions like
     * <code>${namespace}</code> or <code>${repoName}</code> to replace project specific values
     *
     */
    @Parameter(property = "fabric8.letschatRoomLinkLabel", defaultValue = "fabric8_${namespace}")
    protected String letschatRoomExpression;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        loadConfigFile();
        KubernetesClient kubernetes = getKubernetes();

        String name = repoName;
        if (Strings.isNotBlank(username)) {
            name = username + "-" + name;
        }
        Map<String, String> labels = new HashMap<>();
        labels.put("user", username);
        labels.put("repo", repoName);

        TaigaClient taiga = null;
        ProjectDTO taigaProject = null;
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
        String jenkinsJobUrl = null;
        try {
            String jenkinsUrl = kubernetes.getServiceURL(ServiceNames.JENKINS, kubernetes.getNamespace(), "http", true);

            if (Strings.isNotBlank(jenkinsUrl)) {
                if (Strings.isNotBlank(jenkinsMonitorView)) {
                    annotations.put("fabric8.link.jenkins.monitor/url", URLUtils.pathJoin(jenkinsUrl, "/view", jenkinsMonitorView));
                    annotations.put("fabric8.link.jenkins.monitor/label", "Monitor");
                }
                if (Strings.isNotBlank(jenkinsPipelineView)) {
                    annotations.put("fabric8.link.jenkins.pipeline/url", URLUtils.pathJoin(jenkinsUrl, "/view", jenkinsPipelineView));
                    annotations.put("fabric8.link.jenkins.pipeline/label", "Pipeline");
                }
                if (Strings.isNotBlank(jenkinsJob)) {
                    jenkinsJobUrl = URLUtils.pathJoin(jenkinsUrl, "/job", jenkinsJob);
                    annotations.put("fabric8.link.jenkins.job/url", jenkinsJobUrl);
                    annotations.put("fabric8.link.jenkins.job/label", "Job");
                }
            }
        } catch (Exception e) {
            getLog().warn("Could not find the Jenkins URL!: " + e, e);
        }

        String taigaLink = getProjectPageLink(taiga, taigaProject, this.taigaProjectLinkPage);
        if (Strings.isNotBlank(taigaLink)) {
            annotations.put("fabric8.link.taiga/url", taigaLink);
            annotations.put("fabric8.link.taiga/label", taigaProjectLinkLabel);
        }
        String taigaTeamLink = getProjectPageLink(taiga, taigaProject, this.taigaTeamLinkPage);
        if (Strings.isNotBlank(taigaTeamLink)) {
            annotations.put("fabric8.link.taiga.team/url", taigaTeamLink);
            annotations.put("fabric8.link.taiga.team/label", taigaTeamLinkLabel);
        }

        String chatRoomLink = getChatRoomLink(letschat);
        if (Strings.isNotBlank(chatRoomLink)) {
            annotations.put("fabric8.link.letschat.room/url", chatRoomLink);
            annotations.put("fabric8.link.letschat.room/label", letschatRoomLinkLabel);
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
        controller.applyBuildConfig(buildConfig, "maven");
        getLog().info("Created build configuration for " + name + " in namespace: " + controller.getNamespace() + " at " + kubernetes.getAddress());

        createJenkinsWebhook(jenkinsJobUrl);
        createTaigaWebhook(taiga, taigaProject);
    }

    protected void loadConfigFile() {
        if (projectConfigFile != null && projectConfigFile.exists() && projectConfigFile.isFile()) {
            getLog().info("Parsing fabric8 devops project configuration from: " + projectConfigFile.getName());
            ProjectConfig projectConfig = null;
            try {
                projectConfig = YamlHelper.parseProjectConfig(projectConfigFile);
            } catch (IOException e) {
                getLog().warn("Failed to parse " + projectConfigFile);
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
        }
        return null;
    }

    protected void createJenkinsWebhook(String jenkinsJobUrl) {
        if (Strings.isNotBlank(jenkinsJobUrl)) {
            String jenkinsWebHook = URLUtils.pathJoin(jenkinsJobUrl, "/build");
            createWebhook(jenkinsWebHook, this.secret);
        }
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
            CreateGogsWebhook.createGogsWebhook(getKubernetes(), getLog(), username, password, repoName, url, webhookSecret);
        } catch (Exception e) {
            getLog().error("Failed to create webhook " + url + " on repository " + repoName + ". Reason: " + e, e);
        }
    }
}
