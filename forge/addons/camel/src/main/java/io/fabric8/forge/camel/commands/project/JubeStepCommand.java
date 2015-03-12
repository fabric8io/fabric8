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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;

import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizard;

public class JubeStepCommand extends AbstractDockerProjectCommand implements UIWizard {

    private String[] jarImages = new String[]{"fabric8/java"};
    private String[] bundleImages = new String[]{"fabric8/karaf-2.4"};
    private String[] warImages = new String[]{"fabric8/tomcat-8.0", "jboss/wildfly"};

    @Inject
    @WithAttributes(label = "from", required = true, description = "The jube image to use as base line")
    private UISelectOne<String> from;

    @Inject
    @WithAttributes(label = "main", required = false, description = "Main class to use for Java standalone")
    private UIInput<String> main;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        context.getUIContext().getAttributeMap().put("prev", JubeStepCommand.class);
        return Results.navigateTo(FabricStepCommand.class);
    }

    @Override
    public void initializeUI(final UIBuilder builder) throws Exception {
        String packaging = getProjectPackaging(getSelectedProject(builder));

        // limit the choices depending on the project packaging
        List<String> choices = new ArrayList<String>();
        if (packaging == null || "jar".equals(packaging)) {
            choices.add(jarImages[0]);
        }
        if (packaging == null || "bundle".equals(packaging)) {
            choices.add(bundleImages[0]);
        }
        if (packaging == null || "war".equals(packaging)) {
            choices.add(warImages[0]);
            choices.add(warImages[1]);
        }
        from.setValueChoices(choices);

        String existing = (String) builder.getUIContext().getAttributeMap().get("docker.from");
        if (existing == null) {
            // docker was not setup, so select if we only have one choice
            if (choices.size() == 1) {
                from.setDefaultValue(choices.get(0));
            }
        }
        from.setDefaultValue(existing);

        main.setRequired(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // is required for jar images
                for (String jar : jarImages) {
                    if (jar.equals(from.getValue())) {
                        return true;
                    }
                }
                return false;
            }
        });
        // only enable main if its required
        main.setEnabled(main.isRequired());

        existing = (String) builder.getUIContext().getAttributeMap().get("docker.main");
        main.setDefaultValue(existing);
        main.addValidator(new ClassNameValidator(true));

        builder.add(from).add(main);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        JubeSetupHelper.setupJube(dependencyInstaller, getSelectedProject(context), from.getValue());
        return Results.success("Adding Jube using image " + from.getValue());
    }

    private static String getProjectPackaging(Project project) {
        if (project != null) {
            MavenFacet maven = project.getFacet(MavenFacet.class);
            return maven.getModel().getPackaging();
        }
        return null;
    }
}
