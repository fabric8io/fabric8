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
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.input.ValueChangeListener;
import org.jboss.forge.addon.ui.input.events.ValueChangeEvent;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizard;

public class DockerStepCommand extends AbstractDockerProjectCommand implements UIWizard {

    private String[] jarImages = new String[]{"fabric8/java"};
    private String[] bundleImages = new String[]{"fabric8/karaf-2.4"};
    private String[] warImages = new String[]{"fabric8/tomcat-8.0", "jboss/wildfly"};

    @Inject
    @WithAttributes(label = "from", required = true, description = "The docker image to use as base line")
    private UISelectOne<String> from;

    @Inject
    @WithAttributes(label = "main", required = false, description = "Main class to use for Java standalone")
    private UIInput<String> main;

    @Override
    public boolean isEnabled(UIContext context) {
        // this is a step in a wizard, you cannot run this standalone
        return false;
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        // should we also include jube?
        String platform = (String) context.getUIContext().getAttributeMap().get("platform");
        if ("Docker-and-Jube".equals(platform)) {
            return Results.navigateTo(JubeStepCommand.class);
        } else {
            return Results.navigateTo(FabricStepCommand.class);
        }
    }

    @Override
    protected boolean isProjectRequired() {
        return super.isProjectRequired();
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

        // is it possible to pre select a choice?
        String defaultChoice = DockerSetupHelper.defaultDockerImage(getSelectedProject(builder));
        if (defaultChoice != null && choices.contains(defaultChoice)) {
            from.setDefaultValue(defaultChoice);
        }

        from.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChanged(ValueChangeEvent event) {
                // use a listener so the jube step knows what we selected as it want to reuse
                builder.getUIContext().getAttributeMap().put("docker.from", event.getNewValue());
            }
        });

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
        main.setDefaultValue(DockerSetupHelper.defaultMainClass(getSelectedProject(builder)));
        main.addValidator(new ClassNameValidator(true));
        main.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChanged(ValueChangeEvent event) {
                // use a listener so the jube step knows what we selected as it want to reuse
                builder.getUIContext().getAttributeMap().put("docker.main", event.getNewValue());
            }
        });

        builder.add(from).add(main);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        DockerSetupHelper.setupDocker(getSelectedProject(context), from.getValue(), main.getValue());
        return Results.success("Adding Docker using image " + from.getValue());
    }

    private static String getProjectPackaging(Project project) {
        if (project != null) {
            MavenFacet maven = project.getFacet(MavenFacet.class);
            return maven.getModel().getPackaging();
        }
        return null;
    }

}
