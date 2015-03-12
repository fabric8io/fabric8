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

import java.io.PrintStream;
import javax.inject.Inject;

import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.Projects;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;

public abstract class AbstractDockerProjectCommand extends AbstractProjectCommand {

    public static String CATEGORY = "Docker";

    @Inject
    protected ProjectFactory projectFactory;

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    protected Project getSelectedProjectOrNull(UIContext context) {
        return Projects.getSelectedProject(this.getProjectFactory(), context);
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    protected PrintStream getOutput(UIExecutionContext context) {
        return context.getUIContext().getProvider().getOutput().out();
    }

    protected Coordinate createCoordinate(String groupId, String artifactId, String version) {
        return createCoordinate(groupId, artifactId, version, null);
    }

    protected Coordinate createCoordinate(String groupId, String artifactId, String version, String packaging) {
        CoordinateBuilder builder = CoordinateBuilder.create()
                .setGroupId(groupId)
                .setArtifactId(artifactId);
        if (version != null) {
            builder = builder.setVersion(version);
        }
        if (packaging != null) {
            builder = builder.setPackaging(packaging);
        }

        return builder;
    }

}
