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
package io.fabric8.forge.devops;

import io.fabric8.forge.addon.utils.MavenHelpers;
import io.fabric8.forge.addon.utils.completer.TestPackageNameCompleter;
import io.fabric8.forge.addon.utils.validator.ClassNameValidator;
import io.fabric8.forge.addon.utils.validator.PackageNameValidator;
import io.fabric8.utils.Strings;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.maven.plugins.ConfigurationBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.profiles.ProfileImpl;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.Callable;

import static io.fabric8.forge.addon.utils.MavenHelpers.createCoordinate;

/**
 * Creates a new kubernetes integration test class for the current project
 */
public class NewIntegrationTestClassCommand extends AbstractDevOpsCommand {
    private static final transient Logger LOG = LoggerFactory.getLogger(NewIntegrationTestClassCommand.class);

    @Inject
    @WithAttributes(label = "targetPackage", required = false,
            description = "The package name where the new test class will be created")
    private UIInput<String> targetPackage;

    @Inject
    @WithAttributes(label = "className", required = true,
            description = "Name of the JUnit test class to generate")
    private UIInput<String> className;

    @Inject
    @WithAttributes(label = "profile", required = true,
            description = "The maven profile name used to run the kubernetes integration test",
            defaultValue = "kit")
    private UIInput<String> profile;

    @Inject
    @WithAttributes(label = "integrationTestWildcard", required = true,
            description = "The wildcard used to find the integration test classes in the generated integration test profile",
            defaultValue = "**/*KT.*")
    private UIInput<String> integrationTestWildcard;

    @Inject
    @WithAttributes(label = "testPlugin", required = true,
            description = "The integration test plugin for running integration tests",
            defaultValue = "FailSafe")
    private UIInput<ITestPlugin> itestPlugin;

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    private DependencyResolver dependencyResolver;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.from(super.getMetadata(context), getClass())
                .category(Categories.create(CATEGORY))
                .name(CATEGORY + ": New Integration Test Class")
                .description("Create a new integration test class and adds any extra required dependencies to the pom.xml");
    }

    @Override
    public void initializeUI(final UIBuilder builder) throws Exception {
        super.initializeUI(builder);

        Project project = getCurrentSelectedProject(builder.getUIContext());
        JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);

        targetPackage.setCompleter(new TestPackageNameCompleter(facet));
        targetPackage.addValidator(new PackageNameValidator());
        targetPackage.setDefaultValue("io.fabric8.itests");

        className.addValidator(new ClassNameValidator(false));
        className.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "IntegrationTestKT";
            }
        });

        builder.add(targetPackage).add(className).add(profile).add(integrationTestWildcard).add(itestPlugin);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);

        // lets ensure the dependencies are added...
        MavenHelpers.ensureMavenDependencyAdded(project, dependencyInstaller, "io.fabric8", "fabric8-arquillian", "test");
        MavenHelpers.ensureMavenDependencyAdded(project, dependencyInstaller, "org.jboss.arquillian.junit", "arquillian-junit-container", "test");
        MavenHelpers.ensureMavenDependencyAdded(project, dependencyInstaller, "org.jboss.shrinkwrap.resolver", "shrinkwrap-resolver-impl-maven", "test");

        // lets create a kubernetes integration test profile if one does not exist...
        String profileId = profile.getValue();
        if (Strings.isNotBlank(profileId)) {
            MavenFacet mavenFacet = getMavenFacet(context);
            Model mavenModel = mavenFacet.getModel();
            Profile kitProfile = MavenHelpers.findProfile(mavenModel, profileId);
            if (kitProfile == null) {
                LOG.info("Creating a new maven profile for id: " + profileId);
                kitProfile = new Profile();
                kitProfile.setId(profileId);
            }

            String itestPluginArtifactId = null;
            ITestPlugin itestPluginValue = itestPlugin.getValue();
            if (itestPluginValue != null) {
                itestPluginArtifactId = itestPluginValue.getArtifactId();
            }
            if (itestPluginArtifactId == null) {
                LOG.warn("Warning - no itestPlugin specified!");
                itestPluginArtifactId = MavenHelpers.failsafeArtifactId;
            }
            String version = MavenHelpers.getVersion(MavenHelpers.mavenPluginsGroupId, itestPluginArtifactId);
            if (version != null) {
                Coordinate coordinate = createCoordinate(MavenHelpers.mavenPluginsGroupId, itestPluginArtifactId, version);

                MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);
                ProfileImpl kitProfileImpl = new ProfileImpl();
                kitProfileImpl.setId(profileId);
                MavenPlugin surefirePlugin = null;
                try {
                    surefirePlugin = pluginFacet.getPlugin(coordinate, kitProfileImpl);
                } catch (Exception e) {
                    LOG.debug("Ignored exception looking up maven plugin for " + coordinate + " for profile " + kitProfileImpl);
                }
                if (surefirePlugin == null) {
                    LOG.info("Creating a new plugin for " + coordinate + " on profile " + kitProfileImpl);
                    String wildcard = integrationTestWildcard.getValue();
                    ConfigurationBuilder configuration = ConfigurationBuilder.create();
                    configuration.createConfigurationElement("includes").createConfigurationElement("include").setText(wildcard);

                    surefirePlugin = MavenPluginBuilder.create().
                            setCoordinate(coordinate).
                            setConfiguration(configuration);
                    pluginFacet.addPlugin(surefirePlugin, kitProfileImpl);
                }
            }
        }

        JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);

        String generatePackageName = targetPackage.getValue();
        String generateClassName = className.getValue();

        JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
        javaClass.setName(generateClassName);
        if (Strings.isNotBlank(generatePackageName)) {
            javaClass.setPackage(generatePackageName);
            generateClassName = generatePackageName + "." + generateClassName;
        }
        javaClass.addImport("io.fabric8.arquillian.kubernetes.Session");
        javaClass.addImport("io.fabric8.kubernetes.api.KubernetesClient");
        javaClass.addImport("io.fabric8.kubernetes.api.model.Pod");
        javaClass.addImport("org.assertj.core.api.Condition");
        javaClass.addImport("org.jboss.arquillian.junit.Arquillian");
        javaClass.addImport("org.jboss.arquillian.test.api.ArquillianResource");
        javaClass.addImport("org.junit.Test");
        javaClass.addImport("org.junit.runner.RunWith");
        javaClass.addImport("io.fabric8.kubernetes.assertions.Assertions.assertThat").setStatic(true);

        javaClass.addAnnotation("RunWith").setLiteralValue("Arquillian.class");

        javaClass.getJavaDoc().setText("Tests that the Kubernetes resources\n" +
                "* (Services, Replication Controllers and Pods)\n" +
                "* can be provisioned and start up correctly.\n" +
                "* \n" +
                "* This test creates a new Kubernetes Namespace for the duration of the test.\n" +
                "* For more information see: http://fabric8.io/guide/testing.html" );

        javaClass.addField().
                setProtected().
                setType("KubernetesClient").
                setName("kubernetes").
                addAnnotation("ArquillianResource");

        javaClass.addField().
                setProtected().
                setType("Session").
                setName("session").
                addAnnotation("ArquillianResource");

        String testBody = "assertThat(kubernetes).pods()\n" +
                "        .runningStatus()\n" +
                "        .filterNamespace(session.getNamespace())\n" +
                "        .haveAtLeast(1, new Condition<Pod>() {\n" +
                "            @Override\n" +
                "            public boolean matches(Pod pod) {\n" +
                "                return true;\n" +
                "            }\n" +
                "        });";

        javaClass.addMethod().
                setPublic().
                setReturnTypeVoid().
                setName("testKubernetesProvisionsAtLeastOnePod").
                setBody(testBody).
                addThrows("Exception").
                addAnnotation("Test");

        facet.saveTestJavaSource(javaClass);

        return Results.success("Created new class " + generateClassName);
    }

}

