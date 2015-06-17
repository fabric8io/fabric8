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

import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.componentsFromArtifact;
import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.dataFormatsFromArtifact;
import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.languagesFromArtifact;

public class CamelInfoCommand extends AbstractCamelProjectCommand {

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(CamelInfoCommand.class).name(
                "Camel: Info").category(Categories.create(CATEGORY))
                .description("Displays what the current project includes of Camel components");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        // noop
    }

    @Override
    public boolean isEnabled(UIContext context) {
        // we dont want this in GUI as it dont add value there
        return super.isEnabled(context) && !context.getProvider().isGUI();
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

        out.println("");
        out.println("Camel Project Information");
        out.println("=========================");

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
                out.print("    dataformat: ");
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
                out.println("");
            }

            out.println("");
        }

        return Results.success();
    }
}
