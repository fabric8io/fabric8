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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import io.fabric8.forge.camel.commands.project.completer.XmlEndpointsCompleter;
import io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper;
import io.fabric8.forge.camel.commands.project.model.CamelEndpointDetails;
import io.fabric8.forge.camel.commands.project.model.EndpointOptionByGroup;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.InputComponentFactory;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;

import static io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper.createUIInputsForCamelComponent;

public class CamelEditEndpointXmlCommand extends AbstractCamelProjectCommand implements UIWizard {

    private static final int MAX_OPTIONS = 15;

    @Inject
    @WithAttributes(label = "Endpoints", required = true, description = "The endpoints from the project")
    private UISelectOne<String> endpoints;

    @Inject
    private InputComponentFactory componentFactory;

    @Inject
    private DependencyInstaller dependencyInstaller;

    private XmlEndpointsCompleter completer;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelEditEndpointXmlCommand.class).name(
                "Camel: Edit Endpoint XML").category(Categories.create(CATEGORY))
                .description("Edit Camel endpoint from an existing XML file");
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
        Map<Object, Object> attributeMap = builder.getUIContext().getAttributeMap();
        attributeMap.remove("navigationResult");

        Project project = getSelectedProject(builder.getUIContext());
        ResourcesFacet resourcesFacet = project.getFacet(ResourcesFacet.class);
        WebResourcesFacet webResourcesFacet = null;
        if (project.hasFacet(WebResourcesFacet.class)) {
            webResourcesFacet = project.getFacet(WebResourcesFacet.class);
        }

        // use value choices instead of completer as that works better in web console
        completer = new XmlEndpointsCompleter(resourcesFacet, webResourcesFacet);
        endpoints.setValueChoices(completer.getEndpointUris());

        builder.add(endpoints);
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();

        NavigationResult navigationResult = (NavigationResult) attributeMap.get("navigationResult");
        if (navigationResult != null) {
            return navigationResult;
        }

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
        attributeMap.put("mode", "edit");
        attributeMap.put("kind", "xml");

        // we need to figure out how many options there is so we can as many steps we need
        String camelComponentName = detail.getEndpointComponentName();
        String uri = detail.getEndpointUri();

        CamelCatalog catalog = new DefaultCamelCatalog();
        String json = catalog.componentJSonSchema(camelComponentName);
        if (json == null) {
            throw new IllegalArgumentException("Could not find catalog entry for component name: " + camelComponentName);
        }

        List<EndpointOptionByGroup> groups = createUIInputsForCamelComponent(camelComponentName, uri, componentFactory, converterFactory);
        int size = groups.size();

        // need all inputs in a list as well
        List<InputComponent> allInputs = new ArrayList<>();
        for (EndpointOptionByGroup group : groups) {
            allInputs.addAll(group.getInputs());
        }

        NavigationResultBuilder builder = Results.navigationBuilder();
        // calculate the number of page we need when there is at most MAX_OPTIONS options per page
//        int pages = size % MAX_OPTIONS == 0 ? size / MAX_OPTIONS : size / MAX_OPTIONS + 1;
        int pages = size;
        for (int i = 0; i < pages; i++) {
//            int from = i * MAX_OPTIONS;
//            int delta = Math.min(MAX_OPTIONS, size - from);
//            int to = from + delta;
            boolean last = i == pages - 1;

            EndpointOptionByGroup current = groups.get(i);
            ConfigureEndpointPropertiesStep step = new ConfigureEndpointPropertiesStep(projectFactory, dependencyInstaller,
                    camelComponentName, current.getGroup(), allInputs, current.getInputs(), last, i, pages);
            builder.add(step);
        }

        navigationResult = builder.build();
        attributeMap.put("navigationResult", navigationResult);
        return navigationResult;
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
