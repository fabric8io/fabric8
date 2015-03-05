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

import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.ClassLoaderFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.util.Strings;

import javax.inject.Inject;
import java.util.concurrent.Callable;

import static io.fabric8.forge.camel.commands.project.CamelCommands.ensureCamelArtifactIdAdded;
import static io.fabric8.forge.camel.commands.project.CamelCommands.loadCamelComponentDetails;

@FacetConstraint({JavaSourceFacet.class, ResourcesFacet.class, ClassLoaderFacet.class})
public class CamelAddComponentInstanceSpringCommand extends AbstractCamelProjectCommand {

    @Inject
    @WithAttributes(label = "componentNameFilter", required = false, description = "To filter components")
    private UISelectOne<String> componentNameFilter;

    @Inject
    @WithAttributes(label = "componentName", required = true, description = "Name of component type to add.")
    private UISelectOne<String> componentName;

    @Inject
    @WithAttributes(label = "instanceName", required = true, description = "Name of component instance to add.")
    private UISelectOne<String> instanceName;

    @Inject
    @WithAttributes(label = "targetPackage", required = false, description = "The package name where this type will be created")
    private UIInput<String> targetPackage;

    @Inject
    @WithAttributes(label = "className", required = true, description = "Name of the Spring Component class to generate")
    private UIInput<String> className;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    private DependencyResolver dependencyResolver;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelAddComponentInstanceSpringCommand.class).name(
                "Camel: New Component Spring").category(Categories.create(CATEGORY))
                .description("Adds a Camel component instance configuration using Spring to your project");
    }

    @Override
    public boolean isEnabled(UIContext context) {
        boolean enabled = super.isEnabled(context);
        if (enabled) {
            return CamelCommands.isSpringProject(getSelectedProject(context));
        }
        return false;
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

        targetPackage.setCompleter(new PackageNameCompleter(facet));
        targetPackage.addValidator(new PackageNameValidator());
        targetPackage.setDefaultValue("org.apache.camel.spring.components");

        className.addValidator(new ClassNameValidator());
        className.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return getDefaultProducerClassName();
            }
        });

        builder.add(componentNameFilter).add(componentName).add(instanceName).add(targetPackage).add(className);
    }

    protected String getDefaultProducerClassName() {
        String name = instanceName.getValue();
        if (!Strings.isBlank(name)) {
            return Strings.capitalize(name) + "ComponentFactory";
        }
        return null;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);
        JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);

        // does the project already have camel?
        Dependency core = findCamelCoreDependency(project);
        if (core == null) {
            return Results.fail("The project does not include camel-core");
        }

        // lets find the camel component class
        String camelComponentName = componentName.getValue();

        CamelComponentDetails details = new CamelComponentDetails();
        Result result = loadCamelComponentDetails(camelComponentName, details);
        if (result != null) {
            return result;
        }
        result = ensureCamelArtifactIdAdded(project, details, dependencyInstaller);
        if (result != null) {
            return result;
        }

        // do we already have a class with the name
        String generatePackageName = targetPackage.getValue();
        String generateClassName = className.getValue();
        String fqn = generatePackageName != null ? generatePackageName + "." + generateClassName : generateClassName;

        JavaResource existing = facet.getJavaResource(fqn);
        if (existing != null && existing.exists()) {
            return Results.fail("A class with name " + fqn + " already exists");
        }

        // need to parse to be able to extends another class
        final JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
        javaClass.setName(generateClassName);
        if (generatePackageName != null) {
            javaClass.setPackage(generatePackageName);
        }
        javaClass.addAnnotation("Component");

        javaClass.addImport("org.springframework.beans.factory.config.BeanDefinition");
        javaClass.addImport("org.springframework.beans.factory.annotation.Qualifier");
        javaClass.addImport("org.springframework.context.annotation.Bean");
        javaClass.addImport("org.springframework.context.annotation.Scope");
        javaClass.addImport("org.springframework.stereotype.Component");


        javaClass.addImport(details.getComponentClassQName());

        String componentClassName = details.getComponentClassName();
        String methodName = "create" + Strings.capitalize(instanceName.getValue()) + "Component";

        String body = componentClassName + " component = new " + componentClassName + "();\n/* TODO configure component here */\nreturn component;";

        MethodSource<JavaClassSource> method = javaClass.addMethod()
                .setPublic()
                .setReturnType(componentClassName)
                .setName(methodName)
                .setBody(body)
                .addThrows(Exception.class);

        method.addAnnotation("Qualifier").setStringValue(camelComponentName);
        method.addAnnotation("Bean");
        method.addAnnotation("Scope").setLiteralValue("BeanDefinition.SCOPE_SINGLETON");

        facet.saveJavaSource(javaClass);

        return Results.success("Added spring factory class " + generateClassName + " to the project");
    }

}
