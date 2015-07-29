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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.inject.Inject;

import io.fabric8.forge.addon.utils.MavenHelpers;
import io.fabric8.forge.addon.utils.VersionHelper;
import io.fabric8.forge.addon.utils.validator.ClassNameValidator;
import org.apache.maven.model.Model;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.maven.plugins.ExecutionBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.templates.TemplateFactory;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.input.ValueChangeListener;
import org.jboss.forge.addon.ui.input.events.ValueChangeEvent;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;

import static io.fabric8.forge.devops.setup.DockerSetupHelper.hasSpringBootMavenPlugin;

@FacetConstraint({MavenFacet.class, MavenPluginFacet.class, ResourcesFacet.class})
public class Fabric8SetupStep extends AbstractDockerProjectCommand implements UIWizardStep {

    private String[] jarImages = new String[]{"fabric8/java"};
    private String[] bundleImages = new String[]{"fabric8/karaf-2.4"};
    private String[] warImages = new String[]{"fabric8/tomcat-8.0", "jboss/wildfly"};

    @Inject
    @WithAttributes(label = "from", required = true, description = "The docker image to use as base line")
    private UISelectOne<String> from;

    @Inject
    @WithAttributes(label = "main", required = false, description = "Main class to use for Java standalone")
    private UIInput<String> main;

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

    @Inject
    private TemplateFactory factory;

    @Inject
    ResourceFactory resourceFactory;

    @Inject
    FacetFactory facetFactory;

    @Override
    public boolean isEnabled(UIContext context) {
        // this is a step in a wizard, you cannot run this standalone
        return false;
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        Project project = getCurrentProject(context.getUIContext());
        System.out.println("====== initializeUI() and we have a project: " + project);
        // no more steps
        return null;
    }

    @Override
    public void validate(UIValidationContext validator) {
        super.validate(validator);

        Project project = getCurrentProject(validator.getUIContext());
        System.out.println("====== validate() and we have a project: " + project);
    }

    @Override
    public void initializeUI(final UIBuilder builder) throws Exception {
        Project project = getCurrentProject(builder.getUIContext());
        System.out.println("====== initializeUI() and we have a project: " + project);

        String packaging = getProjectPackaging(project);

        boolean springBoot = hasSpringBootMavenPlugin(project);

        // limit the choices depending on the project packaging
        List<String> choices = new ArrayList<String>();
        if (packaging == null || springBoot || "jar".equals(packaging)) {
            choices.add(jarImages[0]);
        }
        if (packaging == null || "bundle".equals(packaging)) {
            choices.add(bundleImages[0]);
        }
        if (!springBoot && (packaging == null || "war".equals(packaging))) {
            choices.add(warImages[0]);
            choices.add(warImages[1]);
        }
        from.setValueChoices(choices);

        // is it possible to pre select a choice?
        String defaultChoice = DockerSetupHelper.defaultDockerImage(project);
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
        builder.add(from);

        if (packaging == null || (!packaging.equals("war") && packaging.equals("ear"))) {
            main.setRequired(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return DockerSetupHelper.isJarImage(from.getValue());
                }
            });
            // only enable main if its required
            main.setEnabled(main.isRequired());
            if (project != null) {
                main.setDefaultValue(DockerSetupHelper.defaultMainClass(project));
            }
            main.addValidator(new ClassNameValidator(true));
            main.addValueChangeListener(new ValueChangeListener() {
                @Override
                public void valueChanged(ValueChangeEvent event) {
                    // use a listener so the jube step knows what we selected as it want to reuse
                    builder.getUIContext().getAttributeMap().put("docker.main", event.getNewValue());
                }
            });
        }
        builder.add(main);


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
        Project project = getCurrentProject(context.getUIContext());


        System.out.println("====== execute() and we have a project: " + project);

        DockerSetupHelper.setupDocker(project, from.getValue(), main.getValue());

        // make sure we have resources as we need it later
        facetFactory.install(project, ResourcesFacet.class);

        // install fabric8 bom
        Dependency bom = DependencyBuilder.create()
                .setCoordinate(MavenHelpers.createCoordinate("io.fabric8", "fabric8-project", VersionHelper.fabric8Version(), "pom"))
                .setScopeType("import");
        dependencyInstaller.installManaged(project, bom);

        // include test dependencies?
        if (test.getValue() != null && test.getValue()) {
            Dependency dependency = DependencyBuilder.create()
                    .setCoordinate(MavenHelpers.createCoordinate("io.fabric8", "arquillian-fabric8", null))
                    .setScopeType("test");
            dependencyInstaller.installManaged(project, dependency);

            dependency = DependencyBuilder.create()
                    .setCoordinate(MavenHelpers.createCoordinate("org.jboss.arquillian.junit", "arquillian-junit-container", null))
                    .setScopeType("test");
            dependencyInstaller.installManaged(project, dependency);
        }

        // add fabric8 plugin
        MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
        MavenPlugin plugin = MavenPluginBuilder.create()
                .setCoordinate(MavenHelpers.createCoordinate("io.fabric8", "fabric8-maven-plugin", VersionHelper.fabric8Version()))
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
        return Results.success("Adding Fabric8 maven support with base Docker image: " + from.getValue());
    }

    /**
     * Returns the current project either from the context or added to the attribute map
     * if the project got created during a new-project wizard
     */
    public Project getCurrentProject(UIContext context) {
        Project project = getSelectedProject(context);
        if (project == null) {
            Map<Object, Object> attributeMap = context.getAttributeMap();
            Object object = attributeMap.get(Project.class);
            if (object instanceof Project) {
                project = (Project) object;
            }
        }
        return project;
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

    private static String getProjectPackaging(Project project) {
        if (project != null) {
            MavenFacet maven = project.getFacet(MavenFacet.class);
            return maven.getModel().getPackaging();
        }
        return null;
    }
}
