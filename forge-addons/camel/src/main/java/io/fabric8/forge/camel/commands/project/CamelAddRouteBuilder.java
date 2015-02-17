/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.camel.commands.project;

import javax.inject.Inject;

import io.fabric8.forge.camel.commands.jolokia.ConnectCommand;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
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
public class CamelAddRouteBuilder extends AbstractCamelProjectCommand {

    @Inject
    @WithAttributes(label = "targetPackage", required = false, description = "The package name where this type will be created")
    private UIInput<String> targetPackage;

    @Inject
    @WithAttributes(label = "name", required = true, description = "Name of RouteBuilder class")
    private UIInput<String> name;

    @Inject
    private DependencyInstaller dependencyInstaller;

    private JavaSourceFacet facet;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectCommand.class).name(
                "project-camel-add-route-builder").category(Categories.create(CATEGORY))
                .description("Adds a Camel RouteBuilder class to your project");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(targetPackage).add(name);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);
        facet = project.getFacet(JavaSourceFacet.class);

        // does the project already have camel?
        Dependency core = findCamelCoreDependency(project);
        if (core == null) {
            return Results.fail("The project does not include camel-core");
        }

        // is it a valid class name
        char[] chars = name.getValue().toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (!Character.isJavaIdentifierPart(ch)) {
                return Results.fail("The class name [" + name.getValue() + "] is invalid at position " + (i + 1));
            }
            // first must be upper case alfa
            if (i == 0 && !Character.isUpperCase(ch)) {
                return Results.fail("The class name [" + name.getValue() + "] must start with an upper case alphabetic character");
            }
        }

        // do we already have a class with the name
        String fqn = targetPackage.getValue() != null ? targetPackage.getValue() + "." + name.getValue() : name.getName();

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
                .setBody("from(\"timer:foo\").to(\"log:foo\");")
                .addThrows(Exception.class);

        facet.saveJavaSource(javaClass);

        return Results.success("Added RouteBuilder " + name.getValue() + " to the project");
    }
}
