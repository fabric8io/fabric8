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

import io.fabric8.forge.addon.utils.completer.PackageNameCompleter;
import io.fabric8.forge.camel.commands.project.completer.XmlResourcesCamelEndpointsVisitor;
import io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper;
import io.fabric8.forge.addon.utils.validator.ClassNameValidator;
import io.fabric8.forge.addon.utils.validator.PackageNameValidator;

import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.ClassLoaderFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
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
import org.jboss.forge.roaster.model.util.Strings;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@FacetConstraint({JavaSourceFacet.class, ResourcesFacet.class, ClassLoaderFacet.class})
public class CamelNewComponentInstanceCDICommand extends AbstractCamelProjectCommand implements UIWizard {

    @Inject
    @WithAttributes(label = "Filter", required = false, description = "To filter components")
    private UISelectOne<String> componentNameFilter;

    @Inject
    @WithAttributes(label = "Component Name", required = true, description = "Name of component type to add")
    private UISelectOne<String> componentName;

    @Inject
    @WithAttributes(label = "Instance Name", required = true, description = "Name of component instance to add")
    private UISelectOne<String> instanceName;

    @Inject
    @WithAttributes(label = "Package Name", required = false, description = "The package name where this type will be created")
    private UIInput<String> targetPackage;

    @Inject
    @WithAttributes(label = "Class Name", required = true, description = "Name of @Producer class")
    private UIInput<String> className;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    private DependencyResolver dependencyResolver;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelNewComponentInstanceCDICommand.class).name(
                "Camel: New Component CDI").category(Categories.create(CATEGORY))
                .description("Creates a new Camel component instance configuration using CDI");
    }

    @Override
    public boolean isEnabled(UIContext context) {
        boolean enabled = super.isEnabled(context);
        if (enabled) {
            return CamelCommandsHelper.isCdiProject(getSelectedProject(context));
        }
        return false;
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        Project project = getSelectedProject(builder.getUIContext());
        final JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);

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
                ResourceVisitor visitor = new XmlResourcesCamelEndpointsVisitor((ResourcesFacet) facet, endpoints);
                ((ResourcesFacet) facet).visitResources(visitor);
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

        targetPackage.setCompleter(new PackageNameCompleter(facet));
        targetPackage.addValidator(new PackageNameValidator());
        targetPackage.setDefaultValue("org.apache.camel.cdi.producers");

        className.addValidator(new ClassNameValidator(false));
        className.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return getDefaultProducerClassName();
            }
        });

        builder.add(componentNameFilter).add(componentName).add(instanceName).add(targetPackage).add(className);
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
        attributeMap.put("componentName", componentName.getValue());
        attributeMap.put("instanceName", instanceName.getValue());
        attributeMap.put("targetPackage", targetPackage.getValue());
        attributeMap.put("className", className.getValue());
        attributeMap.put("kind", "cdi");
        return Results.navigateTo(ConfigureComponentPropertiesStep.class);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return Results.success();
    }

    protected String getDefaultProducerClassName() {
        String name = instanceName.getValue();
        if (!Strings.isBlank(name)) {
            return Strings.capitalize(name) + "ComponentProducer";
        }
        return null;
    }

}
