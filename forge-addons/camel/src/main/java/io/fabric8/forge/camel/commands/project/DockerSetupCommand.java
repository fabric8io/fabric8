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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.inject.Inject;

import io.fabric8.forge.camel.commands.jolokia.ConnectCommand;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
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

@FacetConstraint({MavenFacet.class, MavenPluginFacet.class})
public class DockerSetupCommand extends AbstractDockerProjectCommand {

    private String[] jarImages = new String[]{"fabric8/java"};
    private String[] bundleImages = new String[]{"fabric8/karaf-2.4"};
    private String[] warImages = new String[]{"fabric8/tomcat-8.0", "jboss/wildfly"};

    @Inject
    @WithAttributes(label = "from", required = true, description = "The docker image to use as base line")
    private UISelectOne<String> from;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectCommand.class).name(
                "Docker: Setup").category(Categories.create(CATEGORY))
                .description("Setup Docker in your project");
    }

    @Override
    public void initializeUI(final UIBuilder builder) throws Exception {
        builder.add(from);

        // the from image values
        from.setValueChoices(new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                Set<String> choices = new LinkedHashSet<String>();
                choices.add(jarImages[0]);
                choices.add(bundleImages[0]);
                choices.add(warImages[0]);
                choices.add(warImages[1]);
                return choices.iterator();
            }
        });

        from.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String answer = null;

                String packaging = getProjectPackaging(getSelectedProject(builder));
                if ("jar".equals(packaging)) {
                    answer = jarImages[0];
                } else if ("bundle".equals(packaging)) {
                    answer = bundleImages[0];
                } else if ("war".equals(packaging)) {
                    answer = warImages[0];
                }
                return answer;
            }
        });
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);

        String fromImage = from != null ? from.getValue() : "fabric8/java";
        DockerSetupHelper.setupDocker(project, fromImage);

        return Results.success("Added Docker to the project");
    }

    protected String getProjectPackaging(Project project) {
        if (project != null) {
            MavenFacet maven = project.getFacet(MavenFacet.class);
            return maven.getModel().getPackaging();
        }
        return null;
    }

}
