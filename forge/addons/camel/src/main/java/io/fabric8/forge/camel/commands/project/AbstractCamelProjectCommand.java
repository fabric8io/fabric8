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

import java.io.PrintStream;
import java.util.Set;
import javax.inject.Inject;

import io.fabric8.forge.addon.utils.CamelProjectHelper;
import org.apache.camel.catalog.CamelCatalog;
import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.Projects;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;

public abstract class AbstractCamelProjectCommand extends AbstractProjectCommand {

    public static String CATEGORY = "Camel";

    @Inject
    protected ProjectFactory projectFactory;

    @Inject
    protected ConverterFactory converterFactory;

    @Inject
    private CamelCatalog camelCatalog;

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    public boolean isEnabled(UIContext context) {
        boolean enabled = super.isEnabled(context);
        if (!enabled) {
            return false;
        }
        if (requiresCamelSetup()) {
            // requires camel is already setup
            Project project = getSelectedProjectOrNull(context);
            if (project != null) {
                return findCamelCoreDependency(project) != null;
            }
        }
        return false;
    }

    protected Project getSelectedProjectOrNull(UIContext context) {
        return Projects.getSelectedProject(this.getProjectFactory(), context);
    }

    protected boolean requiresCamelSetup() {
        return true;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    protected ConverterFactory getConverterFactory() {
        return converterFactory;
    }

    protected CamelCatalog getCamelCatalog() {
        return camelCatalog;
    }

    protected PrintStream getOutput(UIExecutionContext context) {
        return context.getUIContext().getProvider().getOutput().out();
    }

    protected Dependency findCamelCoreDependency(Project project) {
        return CamelProjectHelper.findCamelCoreDependency(project);
    }

    protected Set<Dependency> findCamelArtifacts(Project project) {
        return CamelProjectHelper.findCamelArtifacts(project);
    }

    protected Coordinate createCoordinate(String groupId, String artifactId, String version) {
        CoordinateBuilder builder = CoordinateBuilder.create()
                .setGroupId(groupId)
                .setArtifactId(artifactId);
        if (version != null) {
            builder = builder.setVersion(version);
        }

        return builder;
    }

    protected Coordinate createCamelCoordinate(String artifactId, String version) {
        return createCoordinate("org.apache.camel", artifactId, version);
    }
}
