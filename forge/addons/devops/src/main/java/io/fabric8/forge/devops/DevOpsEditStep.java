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
package io.fabric8.forge.devops;

import io.fabric8.devops.ProjectConfig;
import io.fabric8.devops.ProjectConfigs;
import io.fabric8.devops.connector.DevOpsConnector;
import io.fabric8.forge.addon.utils.CommandHelpers;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.letschat.LetsChatClient;
import io.fabric8.letschat.LetsChatKubernetes;
import io.fabric8.letschat.RoomDTO;
import io.fabric8.taiga.ProjectDTO;
import io.fabric8.taiga.TaigaClient;
import io.fabric8.taiga.TaigaKubernetes;
import io.fabric8.utils.Files;
import io.fabric8.utils.Filter;
import io.fabric8.utils.GitHelpers;
import io.fabric8.utils.IOHelpers;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.util.ResourceUtil;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.ValueChangeListener;
import org.jboss.forge.addon.ui.input.events.ValueChangeEvent;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class DevOpsEditStep extends AbstractDevOpsCommand implements UIWizardStep {
    private static final transient Logger LOG = LoggerFactory.getLogger(DevOpsEditStep.class);

    @Inject
    @WithAttributes(label = "flow", required = false, description = "The URL of the Jenkins workflow groovy script to use for builds")
    private UIInput<String> flow;

    @Inject
    @WithAttributes(label = "copyFlowToProject", required = false, description = "Should we copy the Jenkins Workflow script into the project source code")
    private UIInput<Boolean> copyFlowToProject;

    @Inject
    @WithAttributes(label = "chatRoom", required = false, description = "Name of chat room to use for this project")
    private UIInput<String> chatRoom;

    @Inject
    @WithAttributes(label = "issueProjectName", required = false, description = "Name of the issue tracker project")
    private UIInput<String> issueProjectName;

    @Inject
    @WithAttributes(label = "codeReview", required = false, description = "Enable code review of all commits")
    private UIInput<Boolean> codeReview;

    private String namespace = KubernetesHelper.defaultNamespace();
    private LetsChatClient letsChat;
    private TaigaClient taiga;
    private List<InputComponent> inputComponents;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(getClass())
                .category(Categories.create(AbstractDevOpsCommand.CATEGORY))
                .name(AbstractDevOpsCommand.CATEGORY + ": Configure")
                .description("Configure the DevOps options for the new project");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        final UIContext context = builder.getUIContext();
        flow.setCompleter(new UICompleter<String>() {
            @Override
            public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
                return filterCompletions(getFlowURIs(context), value);
            }
        });
        flow.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChanged(ValueChangeEvent event) {
                String value = event.getNewValue() != null ? event.getNewValue().toString() : null;
                if (value != null) {
                    String description = getDescriptionForFlow(value);
                    flow.setNote(description != null ? description : "");
                } else {
                    flow.setNote("");
                }
                boolean canCopy = Strings.isNotBlank(value);
                copyFlowToProject.setEnabled(canCopy);
            }
        });
        chatRoom.setCompleter(new UICompleter<String>() {
            @Override
            public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
                return filterCompletions(getChatRoomNames(), value);
            }
        });
        chatRoom.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChanged(ValueChangeEvent event) {
                String value = event.getNewValue() != null ? event.getNewValue().toString() : null;
                if (value != null) {
                    String description = getDescriptionForChatRoom(value);
                    chatRoom.setNote(description != null ? description : "");
                } else {
                    chatRoom.setNote("");
                }
            }
        });
        issueProjectName.setCompleter(new UICompleter<String>() {
            @Override
            public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
                return filterCompletions(getIssueProjectNames(), value);
            }
        });
        issueProjectName.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChanged(ValueChangeEvent event) {
                String value = event.getNewValue() != null ? event.getNewValue().toString() : null;
                if (value != null) {
                    String description = getDescriptionForIssueProject(value);
                    issueProjectName.setNote(description != null ? description : "");
                } else {
                    issueProjectName.setNote("");
                }
            }
        });

        // lets initialise the data from the current config if it exists
        ProjectConfig config = null;
        Project project = getSelectedProject(context);
        File configFile = getProjectConfigFile(project);
        if (configFile != null && configFile.exists()) {
            config = ProjectConfigs.parseProjectConfig(configFile);
        }
        if (config != null) {
            CommandHelpers.setInitialComponentValue(flow, config.getFlow());
            CommandHelpers.setInitialComponentValue(chatRoom, config.getChatRoom());
            CommandHelpers.setInitialComponentValue(issueProjectName, config.getIssueProjectName());
            CommandHelpers.setInitialComponentValue(codeReview, config.getCodeReview());
        }

        inputComponents = CommandHelpers.addInputComponents(builder, flow, copyFlowToProject, chatRoom, issueProjectName, codeReview);
    }

    public static Iterable<String> filterCompletions(Iterable<String> values, String inputValue) {
        List<String> answer = new ArrayList<>();
        String lowerInputValue = inputValue.toLowerCase();
        for (String value : values) {
            if (value != null) {
                if (value.toLowerCase().indexOf(lowerInputValue) >= 0) {
                    answer.add(value);
                }
            }
        }
        return answer;
    }


    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        return null;
    }


    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        LOG.info("Creating the fabric8.yml file");


        String fileName = ProjectConfigs.FILE_NAME;
        Project project = getSelectedProject(context);
        File configFile = getProjectConfigFile(project);
        if (configFile == null) {
            return Results.fail("This command requires a project");
        }
        ProjectConfig config = null;
        boolean hasFile = false;
        if (configFile.exists()) {
            config = ProjectConfigs.parseProjectConfig(configFile);
            hasFile = true;
        }
        if (config == null) {
            config = new ProjectConfig();
        }

        CommandHelpers.putComponentValuesInAttributeMap(context, inputComponents);
        updateConfiguration(context, config);
        System.out.println("Result: " + config);

        String message;
        if (config.isEmpty() && !hasFile) {
            message = "No " + fileName + " need be generated as there is no configuration";
            return Results.success(message);
        } else {
            String operation = "Updated";
            if (!configFile.exists()) {
                operation = "Created";
            }
            ProjectConfigs.saveConfig(config, configFile);
            message = operation + " " + fileName;
        }

        // now lets update the devops stuff
        UIContext uiContext = context.getUIContext();
        Map<Object, Object> attributeMap = uiContext.getAttributeMap();

        String gitUrl = null;
        Object object = attributeMap.get(Project.class);
        String user = getStringAttribute(attributeMap, "gitUser");
        String named = null;
        File basedir = CommandHelpers.getBaseDir(project);

        if (object instanceof Project) {
            Project newProject = (Project) object;
            MetadataFacet facet = newProject.getFacet(MetadataFacet.class);
            if (facet != null) {
                named = facet.getProjectName();

                String email = getStringAttribute(attributeMap, "gitAuthorEmail");
                String address = getStringAttribute(attributeMap, "gitAddress");
                String htmlUrl = address + user + "/" + named;
                String fullName = user + "/" + named;
                gitUrl = address + user + "/" + named + ".git";
            } else {
                LOG.error("No MetadataFacet for newly created project " + newProject);
            }
        } else {
            // updating an existing project - so lets try find the git url from the current source code
            gitUrl = GitHelpers.extractGitUrl(basedir);
            if (basedir != null) {
                named = basedir.getName();
            }
        }
        ProjectConfigs.defaultEnvironments(config);


        Boolean copyFlowToProjectValue = copyFlowToProject.getValue();
        if (copyFlowToProjectValue != null && copyFlowToProjectValue.booleanValue()) {
            if (basedir == null && !basedir.isDirectory()) {
                LOG.warn("Cannot copy the flow to the project as no basedir!");
            } else {
                String flow = this.flow.getValue();
                if (Strings.isNullOrBlank(flow)) {
                    LOG.warn("Cannot copy the flow to the project as no flow selected!");
                } else {
                    String flowText = getFlowContent(flow, uiContext);
                    if (Strings.isNullOrBlank(flowText))  {
                        LOG.warn("Cannot copy the flow to the project as no flow text could be loaded!");
                    } else {
                        flowText = Strings.replaceAllWithoutRegex(flowText, "GIT_URL", "'" + gitUrl + "'");
                        File newFile = new File(basedir, ProjectConfigs.LOCAL_FLOW_FILE_NAME);
                        Files.writeToFile(newFile, flowText.getBytes());
                        LOG.info("Written flow to " + newFile);
                        if (config != null) {
                            config.setFlow(null);
                            config.setUseLocalFlow(true);
                        }
                    }
                }
            }
        }

        final DevOpsConnector connector = new DevOpsConnector();
        connector.setProjectConfig(config);
        connector.setTryLoadConfigFileFromRemoteGit(false);
        connector.setUsername(user);
        connector.setPassword(getStringAttribute(attributeMap, "gitPassword"));
        connector.setBranch(getStringAttribute(attributeMap, "gitBranch", "master"));
        connector.setBasedir(basedir);
        connector.setGitUrl(gitUrl);
        connector.setRepoName(named);

        // lets not register webhooks yet - lets trigger the build immediately
        // then later on lets register a webhook for any further commits
        // otherwise we can get a timing issue where the push happens before the
        // webhook handling kicks in
        connector.setRegisterWebHooks(true);
        connector.setTriggerJenkinsJob(true);

        LOG.info("Using connector: " + connector);

/*
        attributeMap.put("registerWebHooks", new Runnable() {
            @Override
            public void run() {
                LOG.info("Now registering webhooks!");
                connector.registerWebHooks();
            }
        });
*/

/*
        TODO

        results.setOutputProperty("fullName", fullName);
        results.setOutputProperty("cloneUrl", remoteUrl);
        results.setOutputProperty("htmlUrl", htmlUrl);
*/

        try {
            connector.execute();
        } catch (Exception e) {
            LOG.error("Failed to update DevOps resources: " + e, e);
        }

        return Results.success(message);
    }


    protected String getStringAttribute(Map<Object, Object> attributeMap, String name, String defaultValue) {
        String answer = getStringAttribute(attributeMap, name);
        return Strings.isNullOrBlank(answer) ? defaultValue : answer;
    }

    protected String getStringAttribute(Map<Object, Object> attributeMap, String name) {
        Object value = attributeMap.get(name);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    protected String getDescriptionForFlow(String flow) {
        return null;
    }

    protected String getFlowContent(String flow, UIContext context) {
        File dir = getJenkinsWorkflowFolder(context);
        if (dir != null) {
            File file = new File(dir, flow);
            if (file.isFile() && file.exists()) {
                try {
                    return IOHelpers.readFully(file);
                } catch (IOException e) {
                    LOG.warn("Failed to load local flow " + file + ". " + e, e);
                }
            }
        }
        return null;
    }


    protected Iterable<String> getFlowURIs(UIContext context) {
        File dir = getJenkinsWorkflowFolder(context);
        if (dir != null) {
            Filter<File> filter = new Filter<File>() {
                @Override
                public boolean matches(File file) {
                    String extension = Files.getFileExtension(file);
                    return file.isFile() && Objects.equal("groovy", extension);
                }
            };
            Set<File> files =  Files.findRecursive(dir, filter);
            Set<String> names = new TreeSet<>();
            for (File file : files) {
                try {
                    String relativePath = Files.getRelativePath(dir, file);
                    String name = Strings.stripPrefix(relativePath, "/");
                    names.add(name);
                } catch (IOException e) {
                    LOG.warn("Failed to find relative path for folder " + dir + " and file " + file + ". " + e, e);
                }
            }
            return new ArrayList<>(names);
        } else {
            LOG.warn("No jenkinsWorkflowFolder!");
            return new ArrayList<>();
        }
    }

    protected File getJenkinsWorkflowFolder(UIContext context) {
        File dir = null;
        Object workflowFolder = context.getAttributeMap().get("jenkinsWorkflowFolder");
        if (workflowFolder instanceof File) {
            dir = (File) workflowFolder;
        }
        return dir;
    }

    protected String getDescriptionForIssueProject(String value) {
        return null;
    }

    protected Iterable<String> getIssueProjectNames() {
        Set<String> answer = new TreeSet<>();
        try {
            TaigaClient letschat = getTaiga();
            if (letschat != null) {
                List<ProjectDTO> projects = null;
                try {
                    projects = letschat.getProjects();
                } catch (Exception e) {
                    LOG.warn("Failed to load chat projects! " + e, e);
                }
                if (projects != null) {
                    for (ProjectDTO project : projects) {
                        String name = project.getName();
                        if (name != null) {
                            answer.add(name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to get issue project names: " + e, e);
        }
        return answer;

    }

    protected String getDescriptionForChatRoom(String chatRoom) {
        return null;
    }

    protected Iterable<String> getChatRoomNames() {
        Set<String> answer = new TreeSet<>();
        try {
            LetsChatClient letschat = getLetsChat();
            if (letschat != null) {
                List<RoomDTO> rooms = null;
                try {
                    rooms = letschat.getRooms();
                } catch (Exception e) {
                    LOG.warn("Failed to load chat rooms! " + e, e);
                }
                if (rooms != null) {
                    for (RoomDTO room : rooms) {
                        String name = room.getSlug();
                        if (name != null) {
                            answer.add(name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to find chat room names: " + e, e);
        }
        return answer;
    }

    public LetsChatClient getLetsChat() {
        if (letsChat == null) {
            letsChat = LetsChatKubernetes.createLetsChat(getKubernetes());
        }
        return letsChat;
    }

    public void setLetsChat(LetsChatClient letsChat) {
        this.letsChat = letsChat;
    }

    public TaigaClient getTaiga() {
        if (taiga == null) {
            taiga = TaigaKubernetes.createTaiga(getKubernetes(),namespace);
        }
        return taiga;
    }

    public void setTaiga(TaigaClient taiga) {
        this.taiga = taiga;
    }


    public static File getProjectConfigFile(Project project) {
        if (project == null) {
            return null;
        }
        Resource<?> root = project.getRoot();
        if (root == null) {
            return null;
        }
        Resource<?> configFileResource = root.getChild(ProjectConfigs.FILE_NAME);
        if (configFileResource == null) {
            return null;
        }
        return ResourceUtil.getContextFile(configFileResource);
    }


    protected void updateConfiguration(UIExecutionContext context, ProjectConfig config) {
        Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
        ProjectConfigs.configureProperties(config, attributeMap);
    }
}
