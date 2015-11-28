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

import javax.inject.Inject;

import io.fabric8.forge.addon.utils.completer.PackageNameCompleter;
import io.fabric8.forge.addon.utils.validator.ClassNameValidator;
import io.fabric8.forge.addon.utils.validator.PackageNameValidator;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.facets.HintsFacet;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;

@FacetConstraint({JavaSourceFacet.class, ResourcesFacet.class})
public class CamelNewRouteBuilderCommand extends AbstractCamelProjectCommand {

    @Inject
    @WithAttributes(label = "Target Package", required = false, description = "The package name where this type will be created")
    private UIInput<String> targetPackage;

    @Inject
    @WithAttributes(label = "Class Name", required = true, description = "Name of RouteBuilder class")
    private UIInput<String> name;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelNewRouteBuilderCommand.class).name(
                "Camel: New RouteBuilder").category(Categories.create(CATEGORY))
                .description("Creates a new Camel RouteBuilder class");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        Project project = getSelectedProject(builder.getUIContext());
        JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);

        targetPackage.setCompleter(new PackageNameCompleter(facet));
        targetPackage.addValidator(new PackageNameValidator());
        targetPackage.getFacet(HintsFacet.class).setInputType(InputType.JAVA_PACKAGE_PICKER);
        name.addValidator(new ClassNameValidator(false));
        name.getFacet(HintsFacet.class).setInputType(InputType.JAVA_CLASS_PICKER);

        builder.add(targetPackage).add(name);
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

        // do we already have a class with the name
        String fqn = targetPackage.getValue() != null ? targetPackage.getValue() + "." + name.getValue() : name.getValue();

        JavaResource existing = facet.getJavaResource(fqn);
        if (existing != null && existing.exists()) {
            return Results.fail("A class with name " + fqn + " already exists");
        }

        // need to parse to be able to extends another class
        final JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
        javaClass.setName(name.getValue());
        if (targetPackage.getValue() != null) {
            javaClass.setPackage(targetPackage.getValue());
        }
        javaClass.setSuperType("RouteBuilder");
        javaClass.addImport("org.apache.camel.builder.RouteBuilder");

        javaClass.addMethod()
                .setPublic()
                .setReturnTypeVoid()
                .setName("configure")
                .addThrows(Exception.class);

        facet.saveJavaSource(javaClass);

        return Results.success("Created new RouteBuilder class " + name.getValue());
    }
}
