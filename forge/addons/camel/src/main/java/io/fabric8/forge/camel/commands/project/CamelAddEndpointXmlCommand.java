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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.inject.Inject;

import io.fabric8.forge.camel.commands.project.completer.XmlFileCompleter;
import io.fabric8.forge.camel.commands.project.completer.XmlResourcesCamelEndpointsVisitor;
import io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.visit.ResourceVisitor;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;

public class CamelAddEndpointXmlCommand extends AbstractCamelProjectCommand implements UIWizard {

    @Inject
    @WithAttributes(label = "componentNameFilter", required = false, description = "To filter components")
    private UISelectOne<String> componentNameFilter;

    @Inject
    @WithAttributes(label = "componentName", required = true, description = "Name of component type to add")
    private UISelectOne<String> componentName;

    @Inject
    @WithAttributes(label = "instanceName", required = true, description = "Name of endpoint instance to add")
    private UIInput<String> instanceName;

    @Inject
    @WithAttributes(label = "file", required = true, description = "The XML file to use (either Spring or Blueprint)")
    private UISelectOne<String> xml;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    private DependencyResolver dependencyResolver;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelAddEndpointXmlCommand.class).name(
                "Camel: Add Endpoint XML").category(Categories.create(CATEGORY))
                .description("Adds a Camel endpoint to an existing XML file");
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
        final ResourcesFacet resourcesFacet = project.getFacet(ResourcesFacet.class);
        WebResourcesFacet webResourcesFacet = project.getFacet(WebResourcesFacet.class);

        componentNameFilter.setValueChoices(CamelCommandsHelper.createComponentNameValues(project));
        componentNameFilter.setDefaultValue("<all>");
        componentName.setValueChoices(CamelCommandsHelper.createComponentNameValues(project, componentNameFilter, false));

        instanceName.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String value = componentName.getValue();
                if (value != null) {
                    // the component may have a dash, so remove it
                    value = value.replaceAll("-", "");
                }
                List<CamelEndpointDetails> endpoints = new ArrayList<>();
                ResourceVisitor visitor = new XmlResourcesCamelEndpointsVisitor(resourcesFacet, endpoints);
                resourcesFacet.visitResources(visitor);
                Iterator<CamelEndpointDetails> it = endpoints.iterator();
                while (it.hasNext()) {
                	CamelEndpointDetails det = it.next();
                	if (det.getEndpointInstance() != null) {
                		if (det.getEndpointInstance().equals(instanceName)) {
                			return null;
                		}
                	}
                }
                return value;
            }
        });

        // use value choices instead of completer as that works better in web console
        xml.setValueChoices(new XmlFileCompleter(resourcesFacet, webResourcesFacet).getFiles());
        builder.add(componentNameFilter).add(componentName).add(instanceName).add(xml);
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
        attributeMap.put("componentName", componentName.getValue());
        attributeMap.put("instanceName", instanceName.getValue());
        attributeMap.put("xml", xml.getValue());
        attributeMap.put("kind", "xml");
        return Results.navigateTo(ConfigureEndpointPropertiesStep.class);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return Results.success();
    }

}
