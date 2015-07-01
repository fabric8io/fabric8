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
package io.fabric8.forge.openshift;

import org.jboss.forge.addon.ui.command.AbstractUICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
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
import org.jboss.forge.furnace.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;


public class ConfigureDevOpsStep extends AbstractUICommand implements UIWizardStep {
    private static final transient Logger LOG = LoggerFactory.getLogger(ConfigureDevOpsStep.class);

    @Inject
    @WithAttributes(label = "flow", required = true, description = "The URL of the Jenkins workflow groovy script to use for builds")
    private UISelectOne<String> flow;

    @Inject
    @WithAttributes(label = "chatRoom", required = false, description = "Name of chat room to use for this project")
    private UISelectOne<String> chatRoom;

    @Inject
    @WithAttributes(label = "issueProjectName", required = false, description = "Name of the issue tracker project")
    private UISelectOne<String> issueProjectName;


    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(getClass())
                .category(Categories.create(AbstractOpenShiftCommand.CATEGORY))
                .name(AbstractOpenShiftCommand.CATEGORY + ": Configure DevOps")
                .description("Configure the DevOps options");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        flow.setValueChoices(getFlowURIs());
        // show note about the chosen value
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
        chatRoom.setValueChoices(getChatRoomNames());
        // show note about the chosen value
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
        issueProjectName.setValueChoices(getIssueProjectNames());
        // show note about the chosen value
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
        builder.add(flow).add(chatRoom).add(issueProjectName);
    }

    @Override
    public void validate(UIValidationContext validator) {
        super.validate(validator);

        String value = flow.getValue();
        if (Strings.isNullOrEmpty(value)) {
            validator.addValidationError(flow, "You must enter a flow");
        }

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
        return Arrays.asList("dummy");
    }


    protected String getDescriptionForChatRoom(String chatRoom) {
        return null;
    }

    protected Iterable<String> getChatRoomNames() {
        return Arrays.asList("fabric8_default");
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

    /**
     * Returns the mandatory String value of the given name
     *
     * @throws IllegalArgumentException if the value is not available in the given attribute map
     */
    public static String mandatoryAttributeValue(Map<Object, Object> attributeMap, String name) {
        Object value = attributeMap.get(name);
        if (value != null) {
            String text = value.toString();
            if (!Strings.isNullOrEmpty(text)) {
                return text;
            }
        }
        throw new IllegalArgumentException("The attribute value '" + name + "' did not get passed on from the previous wizard page");
    }
}
