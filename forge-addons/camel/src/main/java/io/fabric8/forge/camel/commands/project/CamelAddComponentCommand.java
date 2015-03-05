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

import javax.inject.Inject;

import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

import static io.fabric8.forge.camel.commands.project.CamelCatalogHelper.findComponentArchetype;

public class CamelAddComponentCommand extends AbstractCamelProjectCommand {

    @Inject
    @WithAttributes(label = "filter", required = false, description = "To filter components")
    private UISelectOne<String> filter;

    @Inject
    @WithAttributes(label = "name", required = true, description = "Name of component to add.")
    private UISelectOne<String> name;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelAddComponentCommand.class).name(
                "project-camel-component-add").category(Categories.create(CATEGORY))
                .description("Adds a Camel component to your project");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        final Project project = getSelectedProject(builder);

        filter.setValueChoices(CamelCommands.createComponentNameValues(project));
        filter.setDefaultValue("<all>");

        name.setValueChoices(CamelCommands.createComponentNameValues(project, filter, true));

        builder.add(filter);
        builder.add(name);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);

        // does the project already have camel?
        Dependency core = findCamelCoreDependency(project);
        if (core == null) {
            return Results.fail("The project does not include camel-core");
        }

        // name -> artifactId
        String artifactId = findComponentArchetype(name.getValue());
        if (artifactId == null) {
            return Results.fail("Camel component " + name.getValue() + " is unknown.");
        }

        DependencyBuilder component = DependencyBuilder.create().setGroupId("org.apache.camel")
                .setArtifactId(artifactId).setVersion(core.getCoordinate().getVersion());

        // install the component
        dependencyInstaller.install(project, component);

        return Results.success("Added Camel component " + name.getValue() + " (" + artifactId + ") to the project");
    }
}
