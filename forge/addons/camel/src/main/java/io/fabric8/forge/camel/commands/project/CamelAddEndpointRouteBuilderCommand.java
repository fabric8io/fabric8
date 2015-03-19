/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
import java.util.concurrent.Callable;
import javax.inject.Inject;

import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.ClassLoaderFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
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

@FacetConstraint({JavaSourceFacet.class, ResourcesFacet.class, ClassLoaderFacet.class})
public class CamelAddEndpointRouteBuilderCommand extends AbstractCamelProjectCommand implements UIWizard {

    @Inject
    @WithAttributes(label = "componentNameFilter", required = false, description = "To filter components")
    private UISelectOne<String> componentNameFilter;

    @Inject
    @WithAttributes(label = "componentName", required = true, description = "Name of component type to add")
    private UISelectOne<String> componentName;

    @Inject
    @WithAttributes(label = "instanceName", required = true, description = "Name of endpoint instance to add")
    private UISelectOne<String> instanceName;

    @Inject
    @WithAttributes(label = "routeBuilder", required = true, description = "The RouteBuilder class to use")
    private UIInput<String> routeBuilder;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    private DependencyResolver dependencyResolver;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelAddEndpointRouteBuilderCommand.class).name(
                "Camel: New Endpoint RouteBuilder").category(Categories.create(CATEGORY))
                .description("Adds a Camel endpoint to an existing RouteBuilder");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        Project project = getSelectedProject(builder.getUIContext());
        JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);

        componentNameFilter.setValueChoices(CamelCommands.createComponentNameValues(project));
        componentNameFilter.setDefaultValue("<all>");
        componentName.setValueChoices(CamelCommands.createComponentNameValues(project, componentNameFilter, false));

        instanceName.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String value = componentName.getValue();
                // TODO if we already have a component instance of the same name
                // then lets return null
                return value;
            }
        });

        // TODO: a completer for RouteBuilder classes
        routeBuilder.setCompleter(null);

        builder.add(componentNameFilter).add(componentName).add(instanceName).add(routeBuilder);
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
        attributeMap.put("componentName", componentName.getValue());
        attributeMap.put("instanceName", instanceName.getValue());
        attributeMap.put("routeBuilder", routeBuilder.getValue());
        attributeMap.put("kind", "cdi");
        return Results.navigateTo(ConfigureEndpointPropertiesStep.class);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return Results.success();
    }

}
