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
import java.util.Set;

import io.fabric8.forge.camel.commands.jolokia.ConnectCommand;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

import static io.fabric8.forge.camel.commands.project.CamelCatalogHelper.componentsFromArtifact;
import static io.fabric8.forge.camel.commands.project.CamelCatalogHelper.dataFormatsFromArtifact;
import static io.fabric8.forge.camel.commands.project.CamelCatalogHelper.languagesFromArtifact;

public class CamelInfoCommand extends AbstractCamelProjectCommand {

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectCommand.class).name(
                "project-camel-info").category(Categories.create(CATEGORY))
                .description("Displays what the current project includes of Camel components");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        // noop
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);

        // does the project already have camel?
        Dependency core = findCamelCoreDependency(project);
        if (core == null) {
            return Results.fail("The project does not include camel-core");
        }

        PrintStream out = getOutput(context);

        out.println("Camel Project Information");
        out.println("=========================");
        out.println("\n");

        Set<Dependency> artifacts = findCamelArtifacts(project);
        for (Dependency d : artifacts) {
            out.println("      artifact: " + d.getCoordinate().getArtifactId());

            Set<String> components = componentsFromArtifact(d.getCoordinate().getArtifactId());
            if (!components.isEmpty()) {
                out.print("     component: ");
                for (String c : components) {
                    out.print(c);
                    out.print(" ");
                }
                out.println("");
            }

            Set<String> dataFormats = dataFormatsFromArtifact(d.getCoordinate().getArtifactId());
            if (!dataFormats.isEmpty()) {
                out.print("   data format: ");
                for (String df : dataFormats) {
                    out.print(df);
                    out.print(" ");
                }
                out.println("");
            }

            Set<String> languages = languagesFromArtifact(d.getCoordinate().getArtifactId());
            if (!languages.isEmpty()) {
                out.print("      language: ");
                for (String l : languages) {
                    out.print(l);
                    out.print(" ");
                }
            }

            out.println("\n");
        }

        return Results.success();
    }
}
