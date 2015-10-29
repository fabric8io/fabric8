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
package io.fabric8.forge.camel.commands.project;

import java.util.Map;
import javax.inject.Inject;

import io.fabric8.forge.camel.commands.project.completer.XmlEndpointsCompleter;
import io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;

public class CamelShowEndpointOptionsXmlCommand extends AbstractCamelProjectCommand implements UIWizard {

    @Inject
    @WithAttributes(label = "endpoints", required = true, description = "The endpoints from the project")
    private UISelectOne<String> endpoints;

    private XmlEndpointsCompleter completer;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelShowEndpointOptionsXmlCommand.class).name(
                "Camel: Show options Endpoint XML").category(Categories.create(CATEGORY))
                .description("Show Camel endpoint options from an existing XML file");
    }

    @Override
    public boolean isEnabled(UIContext context) {
        boolean enabled = super.isEnabled(context);
        if (enabled) {
            // must be spring or blueprint project for editing xml files
            boolean spring = CamelCommandsHelper.isSpringProject(getSelectedProject(context));
            boolean blueprint = CamelCommandsHelper.isBlueprintProject(getSelectedProject(context));
            return spring || blueprint;
        }
        return false;
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        Project project = getSelectedProject(builder.getUIContext());
        ResourcesFacet resourcesFacet = project.getFacet(ResourcesFacet.class);
        WebResourcesFacet webResourcesFacet = project.getFacet(WebResourcesFacet.class);

        // use value choices instead of completer as that works better in web console
        completer = new XmlEndpointsCompleter(resourcesFacet, webResourcesFacet);
        endpoints.setValueChoices(completer.getEndpointUris());
        builder.add(endpoints);
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();

        String selectedUri = endpoints.getValue();
        CamelEndpointDetails detail = completer.getEndpointDetail(selectedUri);
        if (detail == null) {
            return null;
        }

        attributeMap.put("componentName", detail.getEndpointComponentName());
        attributeMap.put("instanceName", detail.getEndpointInstance());
        attributeMap.put("endpointUri", detail.getEndpointUri());
        attributeMap.put("lineNumber", detail.getLineNumber());
        attributeMap.put("xml", detail.getFileName());
        attributeMap.put("kind", "xml");
        return Results.navigateTo(ShowEndpointPropertiesStep.class);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        boolean empty = !endpoints.getValueChoices().iterator().hasNext();
        if (empty) {
            return Results.fail("No Camel endpoints found");
        } else {
            return Results.success();
        }
    }

}
