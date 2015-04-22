/**
 * Copyright 2005-2014 Red Hat, Inc.
 * <p/>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.fabric8.forge.openshift;

import io.fabric8.forge.addon.utils.completer.TestPackageNameCompleter;
import io.fabric8.forge.addon.utils.validator.ClassNameValidator;
import io.fabric8.forge.addon.utils.validator.PackageNameValidator;
import io.fabric8.utils.Strings;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
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
import org.jboss.forge.roaster.model.source.Import;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Creates a new kubernetes integration test class for the current project
 */
public class NewIntegrationTestClass extends AbstractOpenShiftCommand {

    @Inject
    @WithAttributes(label = "targetPackage", required = false, description = "The package name where the new test case will be created")
    private UIInput<String> targetPackage;

    @Inject
    @WithAttributes(label = "className", required = true, description = "Name of @Producer class")
    private UIInput<String> className;

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

        Project project = getSelectedProject(builder.getUIContext());
        JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);

        targetPackage.setCompleter(new TestPackageNameCompleter(facet));
        targetPackage.addValidator(new PackageNameValidator());
        targetPackage.setDefaultValue("io.fabric8.itests");

        className.addValidator(new ClassNameValidator(false));
        className.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "KubernetesIT";
            }
        });

        builder.add(targetPackage).add(className);

    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);

        // lets ensure the dependencies are added...
        ensureMavenDependencyAdded(project, dependencyInstaller, "io.fabric8", "arquillian-fabric8", "test");
        ensureMavenDependencyAdded(project, dependencyInstaller, "org.jboss.arquillian.junit", "arquillian-junit-container", "test");
        ensureMavenDependencyAdded(project, dependencyInstaller, "org.jboss.shrinkwrap.resolver", "shrinkwrap-resolver-impl-maven", "test");

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

        javaClass.getJavaDoc().setText("Tests that the Kubernetes resources (Services, Replication Controllers and Pods)\n" +
                "can be provisioned and start up correctly.\n\n" +
                "This test creates a new Kubernetes Namespace for the duration of the test case");

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

    /**
     * Returns true if the dependency was added or false if its already there
     */
    public static boolean ensureMavenDependencyAdded(Project project, DependencyInstaller dependencyInstaller, String groupId, String artifactId, String scope) {
        List<Dependency> dependencies = project.getFacet(DependencyFacet.class).getEffectiveDependencies();
        for (Dependency d : dependencies) {
            if (groupId.equals(d.getCoordinate().getGroupId()) && artifactId.equals(d.getCoordinate().getArtifactId())) {
                return false;
            }
        }

        // install the component
        DependencyBuilder component = DependencyBuilder.create().setGroupId(groupId)
                .setArtifactId(artifactId).setScopeType(scope); //.setVersion(core.getCoordinate().getVersion());

        dependencyInstaller.install(project, component);
        return true;
    }

}

