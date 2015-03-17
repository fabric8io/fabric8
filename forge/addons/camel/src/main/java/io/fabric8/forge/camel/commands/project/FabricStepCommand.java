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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.maven.plugins.ExecutionBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizard;

public class FabricStepCommand extends AbstractDockerProjectCommand implements UIWizard {

    @Inject
    @WithAttributes(label = "container", required = false, description = "Container label to use for the app")
    private UIInput<String> container;

    @Inject
    @WithAttributes(label = "group", required = false, description = "Group label to use for the app")
    private UIInput<String> group;

    @Inject
    @WithAttributes(label = "icon", required = false, description = "Icon to use for the app")
    private UISelectOne<String> icon;

    @Inject
    @WithAttributes(label = "test", required = false, defaultValue = "true", description = "Include test dependencies")
    private UIInput<Boolean> test;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Override
    public boolean isEnabled(UIContext context) {
        // this is a step in a wizard, you cannot run this standalone
        return false;
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        // no more steps
        return null;
    }

    @Override
    public void initializeUI(final UIBuilder builder) throws Exception {
        container.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String from = (String) builder.getUIContext().getAttributeMap().get("docker.from");
                if (from != null) {
                    return asContainer(from);
                }
                return null;
            }
        });

        // the from image values
        icon.setValueChoices(new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                Set<String> choices = new LinkedHashSet<String>();
                choices.add("activemq");
                choices.add("camel");
                choices.add("java");
                choices.add("jetty");
                choices.add("karaf");
                choices.add("mule");
                choices.add("tomcat");
                choices.add("tomee");
                choices.add("weld");
                choices.add("wildfly");
                return choices.iterator();
            }
        });
        icon.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                if (container.getValue() != null) {
                    for (String choice : icon.getValueChoices()) {
                        if (choice.equals(container.getValue())) {
                            return choice;
                        }
                    }
                }
                return null;
            }
        });

        group.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                // use the project name as default value
                return null;
            }
        });

        builder.add(container).add(group).add(icon).add(test);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        // install fabric8 bom
        Project project = getSelectedProject(context);

        Dependency bom = DependencyBuilder.create()
                .setCoordinate(createCoordinate("io.fabric8", "fabric8-project", VersionHelper.fabric8Version(), "pom"))
                .setScopeType("import");
        dependencyInstaller.installManaged(project, bom);

        // include test dependencies?
        if (test.getValue() != null && test.getValue()) {
            Dependency dependency = DependencyBuilder.create()
                    .setCoordinate(createCoordinate("io.fabric8", "arquillian-fabric8", null))
                    .setScopeType("test");
            dependencyInstaller.installManaged(project, dependency);

            dependency = DependencyBuilder.create()
                    .setCoordinate(createCoordinate("org.jboss.arquillian.junit", "arquillian-junit-container", null))
                    .setScopeType("test");
            dependencyInstaller.installManaged(project, dependency);
        }

        // add fabric8 plugin
        MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
        MavenPlugin plugin = MavenPluginBuilder.create()
                .setCoordinate(createCoordinate("io.fabric8", "fabric8-maven-plugin", VersionHelper.fabric8Version()))
                .addExecution(ExecutionBuilder.create().setId("json").addGoal("json"))
                .addExecution(ExecutionBuilder.create().setId("zip").addGoal("zip"));
        pluginFacet.addPlugin(plugin);

        // update properties section in pom.xml
        MavenFacet maven = project.getFacet(MavenFacet.class);
        Model pom = maven.getModel();
        Properties properties = pom.getProperties();
        boolean updated = false;
        if (container.getValue() != null) {
            properties.put("fabric8.label.container", container.getValue());
            updated = true;
        }
        if (icon.getValue() != null) {
            properties.put("fabric8.iconRef", "icons/" + icon.getValue());
            updated = true;
        }
        if (group.getValue() != null) {
            properties.put("fabric8.label.group", group.getValue());
            updated = true;
        }

        // to save then set the model
        if (updated) {
            maven.setModel(pom);
        }

        return Results.success("Adding Fabric");
    }

    private static String asContainer(String fromImage) {
        int idx = fromImage.indexOf('/');
        if (idx > 0) {
            fromImage = fromImage.substring(idx + 1);
        }
        idx = fromImage.indexOf('-');
        if (idx > 0) {
            fromImage = fromImage.substring(0, idx);
        }
        return fromImage;
    }
}
