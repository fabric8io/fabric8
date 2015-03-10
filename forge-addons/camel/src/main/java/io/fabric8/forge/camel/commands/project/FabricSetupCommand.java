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

import java.util.Properties;
import javax.inject.Inject;

import io.fabric8.forge.camel.commands.jolokia.ConnectCommand;
import org.apache.maven.model.Model;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.maven.plugins.ExecutionBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
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

@FacetConstraint({MavenFacet.class, MavenPluginFacet.class})
public class FabricSetupCommand extends AbstractFabricProjectCommand {

    @Inject
    @WithAttributes(label = "label", required = false, description = "Label to use for the app")
    private UIInput<String> group;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectCommand.class).name(
                "Fabric: Setup").category(Categories.create(CATEGORY))
                .description("Setup Fabric8 in your project");
    }

    @Override
    public void initializeUI(final UIBuilder builder) throws Exception {
        builder.add(group);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);

        // install fabric8 bom
        Dependency bom = DependencyBuilder.create()
                .setCoordinate(createCoordinate("io.fabric8", "fabric8-project", VersionHelper.fabric8Version(), "pom"))
                .setScopeType("import");
        dependencyInstaller.installManaged(project, bom);

        // add fabric8 plugin
        MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
        MavenPlugin plugin = MavenPluginBuilder.create()
                .setCoordinate(createCoordinate("io.fabric8", "fabric8-maven-plugin", VersionHelper.fabric8Version()))
                .addExecution(ExecutionBuilder.create().setId("json").addGoal("json"));
        pluginFacet.addPlugin(plugin);

        String container = null;
        String packaging = getProjectPackaging(project);
        if ("jar".equals(packaging)) {
            container = "java";
        } else if ("bundle".equals(packaging)) {
            container = "karaf";
        } else if ("war".equals(packaging)) {
            container = "tomcat";
        }

        // update properties section in pom.xml
        MavenFacet maven = project.getFacet(MavenFacet.class);
        Model pom = maven.getModel();
        Properties properties = pom.getProperties();
        boolean updated = false;
        if (container != null) {
            properties.put("fabric8.label.container", container);
            updated = true;
        }
        if (group.getValue() != null) {
            properties.put("fabric8.label.group", group.getValue());
            updated = true;
        }

        // to save then set the model
        if (updated) {
            maven.setModel(pom);
        }

        // TODO: icon?
        // <fabric8.iconRef>icons/camel.svg</fabric8.iconRef>

        return Results.success("Added Fabric8 to the project");
    }

    protected String getProjectPackaging(Project project) {
        if (project != null) {
            MavenFacet maven = project.getFacet(MavenFacet.class);
            return maven.getModel().getPackaging();
        }
        return null;
    }

}
