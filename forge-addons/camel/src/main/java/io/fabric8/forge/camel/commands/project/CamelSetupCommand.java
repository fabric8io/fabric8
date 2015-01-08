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
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
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

public class CamelSetupCommand extends AbstractCamelProjectCommand {

    @Inject
    @WithAttributes(label = "version", required = false, description = "Camel version to use. If none provided then the latest version will be used.")
    private UIInput<String> version;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectCommand.class).name(
                "project-camel-setup").category(Categories.create(CATEGORY))
                .description("Setup Apache Camel in your project");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(version);
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

        // add camel-maven-plugin
        MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
        MavenPluginBuilder plugin = MavenPluginBuilder.create()
                .setCoordinate(createCamelCoordinate("camel-maven-plugin", camelVersion));
        pluginFacet.addPlugin(plugin);

        // TODO: figure out latest hawtio version

        // add hawtio-maven-plugin using latest version
        plugin = MavenPluginBuilder.create()
                .setCoordinate(createCoordinate("io.hawt", "hawtio-maven-plugin", null));
        pluginFacet.addPlugin(plugin);

        return Results.success("Added Apache Camel to the project");
    }

}
