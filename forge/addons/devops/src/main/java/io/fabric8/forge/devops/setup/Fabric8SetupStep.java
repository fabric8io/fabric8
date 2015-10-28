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

import io.fabric8.forge.addon.utils.MavenHelpers;
import io.fabric8.forge.addon.utils.VersionHelper;
import io.fabric8.forge.addon.utils.validator.ClassNameOrMavenPropertyValidator;
import io.fabric8.forge.devops.AbstractDevOpsCommand;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
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
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.input.ValueChangeListener;
import org.jboss.forge.addon.ui.input.events.ValueChangeEvent;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;

import static io.fabric8.forge.addon.utils.MavenHelpers.ensureMavenDependencyAdded;
import static io.fabric8.forge.devops.setup.DockerSetupHelper.hasSpringBootMavenPlugin;
import static io.fabric8.forge.devops.setup.DockerSetupHelper.setupDocker;

@FacetConstraint({MavenFacet.class, MavenPluginFacet.class, ResourcesFacet.class})
public class Fabric8SetupStep extends AbstractDevOpsCommand implements UIWizardStep {
    private static final transient Logger LOG = LoggerFactory.getLogger(Fabric8SetupStep.class);

    private String[] jarImages = new String[]{DockerSetupHelper.DEFAULT_JAVA_IMAGE, DockerSetupHelper.OTHER_JAVA_IMAGE};
    private String[] bundleImages = new String[]{DockerSetupHelper.DEFAULT_KARAF_IMAGE};
    private String[] warImages = new String[]{DockerSetupHelper.DEFAULT_TOMCAT_IMAGE, DockerSetupHelper.DEFAULT_WILDFLY_IMAGE};

    @Inject
    @WithAttributes(label = "from", required = true, description = "The docker image to use as base line")
    private UIInput<String> from;

    @Inject
    @WithAttributes(label = "main", required = false, description = "Main class to use for Java standalone")
    private UIInput<String> main;

    @Inject
    @WithAttributes(label = "container", required = false, description = "Container name to use for the app")
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
        return null;
    }


    @Override
    public void initializeUI(final UIBuilder builder) throws Exception {
        LOG.info("Getting the current project");
        Project project = getCurrentSelectedProject(builder.getUIContext());

        LOG.info("Got the current project");

        String packaging = getProjectPackaging(project);

        boolean springBoot = hasSpringBootMavenPlugin(project);

        // limit the choices depending on the project packaging
        final List<String> choices = new ArrayList<String>();
        if (packaging == null || springBoot || "jar".equals(packaging)) {
            choices.addAll(Arrays.asList(jarImages));
        }
        if (packaging == null || "bundle".equals(packaging)) {
            choices.add(bundleImages[0]);
        }
        if (!springBoot && (packaging == null || "war".equals(packaging))) {
            choices.addAll(Arrays.asList(warImages));
        }
        from.setCompleter(new UICompleter<String>() {
            @Override
            public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
                return choices;
            }
        });

        // is it possible to pre select a choice?
        if (choices.size() > 0) {
            String defaultChoice = choices.get(0);
            if (defaultChoice != null) {
                from.setDefaultValue(defaultChoice);
            }
        }

        from.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChanged(ValueChangeEvent event) {
                // use a listener so the docker step knows what we selected as it want to reuse
                builder.getUIContext().getAttributeMap().put("docker.from", event.getNewValue());
            }
        });
        builder.add(from);

        if (packaging == null || (!packaging.equals("war") && !packaging.equals("ear"))) {
            boolean jarImage = DockerSetupHelper.isJarImage(from.getValue());
            // TODO until we can detect reliably executable JARS versus mains lets not make this mandatory
/*
            main.setRequired(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return jarImage;
                }
            });
*/
            // only enable main if its required
            // TODO we could disable if we knew this was an executable jar
            main.setEnabled(jarImage);
            if (project != null) {
                main.setDefaultValue(DockerSetupHelper.defaultMainClass(project));
            }
            main.addValidator(new ClassNameOrMavenPropertyValidator(true));
            main.addValueChangeListener(new ValueChangeListener() {
                @Override
                public void valueChanged(ValueChangeEvent event) {
                    // use a listener so the docker step knows what we selected as it want to reuse
                    builder.getUIContext().getAttributeMap().put("docker.main", event.getNewValue());
                }
            });
            builder.add(main);
        }


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

        builder.add(test).add(icon).add(group).add(container);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        LOG.info("starting to setup fabric8 project");
        Project project = getCurrentSelectedProject(context.getUIContext());
        if (project == null) {
            return Results.fail("No pom.xml available so cannot edit the project!");
        }

        // setup docker-maven-plugin and fabric8-maven-plugin
        setupDocker(project, from.getValue(), main.getValue());
        LOG.info("docker-maven-plugin now setup");
        setupFabricMavenPlugin(project);
        LOG.info("fabric8-maven-plugin now setup");

        MavenFacet maven = project.getFacet(MavenFacet.class);
        Model pom = maven.getModel();

        // import fabric8 bom
        LOG.info("importing fabric8 bom");
        importFabricBom(project, pom);

        // make sure we have resources as we need it later
        facetFactory.install(project, ResourcesFacet.class);

        LOG.info("setting up arquillian test");
        setupArguillianTest(project, pom);

        LOG.info("setting up fabric8 properties");
        setupFabricProperties(project, maven, pom);

        return Results.success("Adding Fabric8 Maven support with base Docker image: " + from.getValue());
    }

    private void importFabricBom(Project project, Model pom) {
        if (!MavenHelpers.hasManagedDependency(pom, "io.fabric8", "fabric8-project")) {
            Dependency bom = DependencyBuilder.create()
                    .setCoordinate(MavenHelpers.createCoordinate("io.fabric8", "fabric8-project", VersionHelper.fabric8Version(), "pom"))
                    .setScopeType("import");
            dependencyInstaller.installManaged(project, bom);
        }
    }

    private void setupFabricMavenPlugin(Project project) {
        MavenPluginBuilder pluginBuilder;
        MavenPlugin plugin = MavenHelpers.findPlugin(project, "io.fabric8", "fabric8-maven-plugin");
        if (plugin != null) {
            LOG.info("Found existing fabric8-maven-plugin");
            pluginBuilder = MavenPluginBuilder.create(plugin);
        } else {
            LOG.info("Adding fabric8-maven-plugin");
            // add fabric8 plugin
            pluginBuilder = MavenPluginBuilder.create()
                    .setCoordinate(MavenHelpers.createCoordinate("io.fabric8", "fabric8-maven-plugin", VersionHelper.fabric8Version()))
                    .addExecution(ExecutionBuilder.create().setId("json").setPhase("generate-resources").addGoal("json"))
                    .addExecution(ExecutionBuilder.create().setId("attach").setPhase("package").addGoal("attach"));
        }

        MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
        pluginFacet.addPlugin(pluginBuilder);
    }

    private void setupArguillianTest(Project project, Model pom) {
        if (test.getValue() != null && test.getValue()) {
            ensureMavenDependencyAdded(project, dependencyInstaller, "io.fabric8", "fabric8-arquillian", "test");
            ensureMavenDependencyAdded(project, dependencyInstaller, "org.jboss.arquillian.junit", "arquillian-junit-container", "test");
        }
    }

    private void setupFabricProperties(Project project, MavenFacet maven, Model pom) {
        // update properties section in pom.xml
        boolean updated = false;

        Properties properties = pom.getProperties();
        updated = MavenHelpers.updatePomProperty(properties, "fabric8.label.container", container.getValue(), updated);
        String iconValue = icon.getValue();
        if (Strings.isNotBlank(iconValue)) {
            updated = MavenHelpers.updatePomProperty(properties, "fabric8.iconRef", "icons/" + iconValue, updated);
        }
        updated = MavenHelpers.updatePomProperty(properties, "fabric8.label.group", group.getValue(), updated);
        String servicePort = getDefaultServicePort(project);
        if (servicePort != null) {
            updated = MavenHelpers.updatePomProperty(properties, "fabric8.service.containerPort", servicePort, updated);
            updated = MavenHelpers.updatePomProperty(properties, "fabric8.service.port", "80", updated);
        }

        // to save then set the model
        if (updated) {
            maven.setModel(pom);
            LOG.info("updated pom.xml");
        }
    }

    /**
     * Try to determine the default service port.
     *
     * If this is a WAR or EAR then lets assume 8080.
     *
     * For karaf we can't know its definitely got http inside; so lets punt for now.
     */
    protected String getDefaultServicePort(Project project) {
        String packaging = getProjectPackaging(project);
        if (Strings.isNotBlank(packaging)) {
            if (Objects.equal("war", packaging) || Objects.equal("ear", packaging)) {
                return "8080";
            }
        }
        return null;
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
