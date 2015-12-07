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

import java.util.Arrays;
import java.util.Set;
import javax.inject.Inject;

import io.fabric8.forge.addon.utils.VersionHelper;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
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

public class CamelSetupCommand extends AbstractCamelProjectCommand {

    private String[] choices = new String[]{"camel-core", "camel-blueprint", "camel-cdi", "camel-spring", "camel-spring-boot"};

    @Inject
    @WithAttributes(label = "Project Kind", required = true, description = "Camel project kind.")
    private UISelectOne<String> kind;

    @Inject
    @WithAttributes(label = "Camel Version", required = false, description = "Camel version to use. If none provided then the latest version will be used.")
    private UIInput<String> version;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Override
    public boolean isEnabled(UIContext context) {
        Project project = getSelectedProjectOrNull(context);
        // only enable if we do not have Camel yet
        if (project == null) {
            // must have a project
            return false;
        } else {
            return true;
        }
    }

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelSetupCommand.class).name(
                "Camel: Setup").category(Categories.create(CATEGORY))
                .description("Setup Apache Camel in your project");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        kind.setValueChoices(Arrays.asList(choices));
        builder.add(kind).add(version);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);

        // does the project already have camel?
        Dependency core = findCamelCoreDependency(project);
        if (core != null) {
            return Results.success("Apache Camel is already setup");
        }

        core = DependencyBuilder.create().setCoordinate(createCamelCoordinate("camel-core", version.getValue()));

        // add camel-core
        dependencyInstaller.install(project, core);

        core = findCamelCoreDependency(project);
        String camelVersion = core.getCoordinate().getVersion();

        // add additional dependencies based on kind
        boolean found = false;
        Set<Dependency> existing = findCamelArtifacts(project);
        for (Dependency dependency : existing) {
            if (dependency.getCoordinate().getArtifactId().equals(kind.getValue())) {
                found = true;
                break;
            }
        }
        if (!found) {
            DependencyBuilder component = DependencyBuilder.create().setGroupId("org.apache.camel")
                    .setArtifactId(kind.getValue()).setVersion(core.getCoordinate().getVersion());
            dependencyInstaller.install(project, component);

            if ("camel-core".equals(kind.getValue())) {
                // install test dependency
                DependencyBuilder testComponent = DependencyBuilder.create().setGroupId("org.apache.camel")
                        .setArtifactId("camel-test").setVersion(core.getCoordinate().getVersion())
                        .setScopeType("test");
                dependencyInstaller.install(project, testComponent);
            } else if ("camel-spring".equals(kind.getValue()) || "camel-spring-boot".equals(kind.getValue())) {
                // install test dependency
                DependencyBuilder testComponent = DependencyBuilder.create().setGroupId("org.apache.camel")
                        .setArtifactId("camel-test-spring").setVersion(core.getCoordinate().getVersion())
                        .setScopeType("test");
                dependencyInstaller.install(project, testComponent);
            } else if ("camel-blueprint".equals(kind.getValue())) {
                // install test dependency
                DependencyBuilder testComponent = DependencyBuilder.create().setGroupId("org.apache.camel")
                        .setArtifactId("camel-test-blueprint").setVersion(core.getCoordinate().getVersion())
                        .setScopeType("test");
                dependencyInstaller.install(project, testComponent);
            }
        }

        // add camel-maven-plugin
        MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
        MavenPluginBuilder plugin = MavenPluginBuilder.create()
                .setCoordinate(createCamelCoordinate("camel-maven-plugin", camelVersion));
        pluginFacet.addPlugin(plugin);

        // add hawtio-maven-plugin using latest version
        plugin = MavenPluginBuilder.create()
                .setCoordinate(createCoordinate("io.hawt", "hawtio-maven-plugin", VersionHelper.hawtioVersion()));
        pluginFacet.addPlugin(plugin);

        return Results.success("Added Apache Camel to the project");
    }

}
