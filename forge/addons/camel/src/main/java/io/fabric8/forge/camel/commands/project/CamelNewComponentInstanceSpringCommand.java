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
import java.util.concurrent.Callable;
import javax.inject.Inject;

import io.fabric8.forge.addon.utils.completer.PackageNameCompleter;
import io.fabric8.forge.addon.utils.validator.ClassNameValidator;
import io.fabric8.forge.addon.utils.validator.PackageNameValidator;
import io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper;
import io.fabric8.forge.camel.commands.project.helper.CamelCommandsHelper;
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
import org.jboss.forge.addon.ui.facets.HintsFacet;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
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
import org.jboss.forge.addon.ui.wizard.UIWizard;
import org.jboss.forge.roaster.model.util.Strings;

@FacetConstraint({JavaSourceFacet.class, ResourcesFacet.class, ClassLoaderFacet.class})
public class CamelNewComponentInstanceSpringCommand extends AbstractCamelProjectCommand implements UIWizard {

    @Inject
    @WithAttributes(label = "Filter", required = false, description = "To filter components")
    private UISelectOne<String> componentNameFilter;

    @Inject
    @WithAttributes(label = "Component Name", required = true, description = "Name of component type to add")
    private UISelectOne<String> componentName;

    @Inject
    @WithAttributes(label = "Instance Name", required = true, description = "Name of component instance to add")
    private UIInput<String> instanceName;

    @Inject
    @WithAttributes(label = "Target Package", required = false, description = "The package name where this type will be created")
    private UIInput<String> targetPackage;

    @Inject
    @WithAttributes(label = "Class Name", required = true, description = "Name of the Spring Component class to generate")
    private UIInput<String> className;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    private DependencyResolver dependencyResolver;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelNewComponentInstanceSpringCommand.class).name(
                "Camel: New Component Spring").category(Categories.create(CATEGORY))
                .description("Creates a new Camel component instance configuration using Spring");
    }

    @Override
    public boolean isEnabled(UIContext context) {
        boolean enabled = super.isEnabled(context);
        if (enabled) {
            return CamelCommandsHelper.isSpringProject(getSelectedProject(context));
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
        // show note about the chosen component
        componentName.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChanged(ValueChangeEvent event) {
                String component = event.getNewValue() != null ? event.getNewValue().toString() : null;
                if (component != null) {
                    String description = CamelCatalogHelper.getComponentDescription(component);
                    componentName.setNote(description != null ? description : "");
                } else {
                    componentName.setNote("");
                }
            }
        });

        targetPackage.setCompleter(new PackageNameCompleter(facet));
        targetPackage.addValidator(new PackageNameValidator());
        targetPackage.getFacet(HintsFacet.class).setInputType(InputType.JAVA_PACKAGE_PICKER);

        className.addValidator(new ClassNameValidator(false));
        className.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return getDefaultProducerClassName();
            }
        });
        className.getFacet(HintsFacet.class).setInputType(InputType.JAVA_CLASS_PICKER);

        builder.add(componentNameFilter).add(componentName).add(instanceName).add(targetPackage).add(className);
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();
        attributeMap.put("componentName", componentName.getValue());
        attributeMap.put("instanceName", instanceName.getValue());
        attributeMap.put("targetPackage", targetPackage.getValue());
        attributeMap.put("className", className.getValue());
        attributeMap.put("kind", "spring");
        return Results.navigateTo(ConfigureComponentPropertiesStep.class);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return Results.success();
    }

    protected String getDefaultProducerClassName() {
        String name = instanceName.getValue();
        if (!Strings.isBlank(name)) {
            return Strings.capitalize(name) + "ComponentFactory";
        }
        return null;
    }

}
