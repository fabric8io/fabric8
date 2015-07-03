/**
 * Copyright 2005-2015 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.forge.devops;

import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.letschat.LetsChatClient;
import io.fabric8.letschat.LetsChatKubernetes;
import io.fabric8.letschat.RoomDTO;
import io.fabric8.taiga.ProjectDTO;
import io.fabric8.taiga.TaigaClient;
import io.fabric8.taiga.TaigaKubernetes;
import org.jboss.forge.addon.ui.command.AbstractUICommand;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class ConfigureDevOpsStep extends AbstractUICommand implements UIWizardStep {
    private static final transient Logger LOG = LoggerFactory.getLogger(ConfigureDevOpsStep.class);

    @Inject
    @WithAttributes(label = "flow", required = false, description = "The URL of the Jenkins workflow groovy script to use for builds")
    private UIInput<String> flow;

    @Inject
    @WithAttributes(label = "chatRoom", required = false, description = "Name of chat room to use for this project")
    private UIInput<String> chatRoom;

    @Inject
    @WithAttributes(label = "issueProjectName", required = false, description = "Name of the issue tracker project")
    private UIInput<String> issueProjectName;

    @Inject
    @WithAttributes(label = "codeReview", required = false, description = "Enable code review of all commits")
    private UIInput<Boolean> codeReview;

    private KubernetesClient kubernetes;
    private LetsChatClient letsChat;
    private TaigaClient taiga;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(getClass())
                .category(Categories.create(AbstractOpenShiftCommand.CATEGORY))
                .name(AbstractOpenShiftCommand.CATEGORY + ": Configure DevOps")
                .description("Configure the DevOps options");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        flow.setCompleter(new UICompleter<String>() {
            @Override
            public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
                return getFlowURIs();
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
            }
        });
        chatRoom.setCompleter(new UICompleter<String>() {
            @Override
            public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
                return getChatRoomNames();
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
                return getIssueProjectNames();
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
        builder.add(flow).add(chatRoom).add(issueProjectName).add(codeReview);
    }

    protected String getDescriptionForFlow(String flow) {
        return null;
    }

    protected Iterable<String> getFlowURIs() {
        return Arrays.asList("maven/Deploy.groovy", "maven/DeployAndStage.groovy");
    }

    protected String getDescriptionForIssueProject(String value) {
        return null;
    }

    protected Iterable<String> getIssueProjectNames() {
        TaigaClient letschat = getTaiga();
        Set<String> answer = new TreeSet<>();
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
        return answer;

    }


    protected String getDescriptionForChatRoom(String chatRoom) {
        return null;
    }

    protected Iterable<String> getChatRoomNames() {
        LetsChatClient letschat = getLetsChat();
        Set<String> answer = new TreeSet<>();
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
        return answer;
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        return null;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
        LOG.info("devops tab has attributes:: " + attributeMap);
        try {
            return Results.success("");
        } catch (IllegalArgumentException e) {
            return Results.fail(e.getMessage());
        }
    }

    public KubernetesClient getKubernetes() {
        if (kubernetes == null) {
            kubernetes = new KubernetesClient();
        }
        return kubernetes;
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
            taiga = TaigaKubernetes.createTaiga(getKubernetes());
        }
        return taiga;
    }

    public void setTaiga(TaigaClient taiga) {
        this.taiga = taiga;
    }
}
