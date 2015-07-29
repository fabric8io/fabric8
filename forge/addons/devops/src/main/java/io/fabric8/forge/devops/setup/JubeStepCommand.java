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
package io.fabric8.forge.devops.setup;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import io.fabric8.forge.addon.utils.validator.ClassNameValidator;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
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
    public boolean isEnabled(UIContext context) {
        // this is a step in a wizard, you cannot run this standalone
        return false;
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        context.getUIContext().getAttributeMap().put("prev", JubeStepCommand.class);
        return Results.navigateTo(Fabric8SetupStep.class);
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

        from.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChanged(ValueChangeEvent event) {
                // use a listener so the fabric step knows what we selected as it want to reuse
                builder.getUIContext().getAttributeMap().put("docker.from", event.getNewValue());
                // main is optional required, and need to be updated if we change the from image
                main.setRequired(isMainRequired((String) event.getNewValue()));
            }
        });

        String existing = (String) builder.getUIContext().getAttributeMap().get("docker.from");
        if (existing == null) {
            // docker was not setup, so select if we only have one choice
            if (choices.size() == 1) {
                from.setDefaultValue(choices.get(0));
            }
        } else {
            from.setDefaultValue(existing);
        }

        existing = (String) builder.getUIContext().getAttributeMap().get("docker.main");
        if (existing == null) {
            // if no main was setup from docker (maybe we are jube only) then try to pick based on project dependencies
            existing = DockerSetupHelper.defaultMainClass(getSelectedProject(builder));
        }
        main.setDefaultValue(existing);
        main.setRequired(isMainRequired(from.getValue()));
        main.addValidator(new ClassNameValidator(true));
        main.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChanged(ValueChangeEvent event) {
                // use a listener so the fabric step knows what we selected as it want to reuse
                builder.getUIContext().getAttributeMap().put("docker.main", event.getNewValue());
            }
        });

        builder.add(from).add(main);
    }

    private boolean isMainRequired(String from) {
        for (String jar : jarImages) {
            if (jar.equals(from)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        JubeSetupHelper.setupJube(dependencyInstaller, getSelectedProject(context), from.getValue());
        DockerSetupHelper.setupDockerProperties(getSelectedProject(context), from.getValue());
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
