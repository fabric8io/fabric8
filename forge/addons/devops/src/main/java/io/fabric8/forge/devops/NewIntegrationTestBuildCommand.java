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

import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.builders.ListEnvVarBuilder;
import io.fabric8.kubernetes.api.builds.Builds;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.utils.Objects;
import org.apache.maven.model.Model;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.fabric8.utils.cxf.JsonHelper.toJson;

/**
 * Creates a new integration test build in OpenShift for the current project
 */
public class NewIntegrationTestBuildCommand extends AbstractDevOpsCommand {
    private static final transient Logger LOG = LoggerFactory.getLogger(NewIntegrationTestBuildCommand.class);

    @Inject
    @WithAttributes(name = "buildName", label = "Build name",
            description = "The build configuration name to generate.",
            required = false)
    UIInput<String> buildName;

    @Inject
    @WithAttributes(name = "mavenCommand", label = "Maven command",
            description = "The docker image name to generate.",
            required = true,
                        defaultValue = "mvn failsafe:integration-test -Pkit")
    UIInput<String> mavenCommand;

    @Inject
    @WithAttributes(name = "image", label = "Docker image",
            description = "The docker image to use to run the maven build.",
            required = true,
            defaultValue = "fabric8/maven")
    UIInput<String> image;

    @Inject
    @WithAttributes(name = "gitUri", label = "Git Uri",
            description = "If the git URI is not specified in the pom.xml then this allows you to specify one to be used.",
            required = false)
    UIInput<String> gitUri;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.from(super.getMetadata(context), getClass())
                .category(Categories.create(CATEGORY))
                .name(CATEGORY + ": New Integration Test Build")
                .description("Create a new integration test build configuration");
    }

    @Override
    public void initializeUI(final UIBuilder builder) throws Exception {
        super.initializeUI(builder);

        buildName.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Model mavenModel = getMavenModel(builder);
                if (mavenModel != null) {
                    return mavenModel.getArtifactId() + "-int-test";
                }
                return null;
            }
        });
        builder.add(buildName);
        builder.add(mavenCommand);
        builder.add(image);
        builder.add(gitUri);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        String buildConfigName = buildName.getValue();
        Objects.assertNotNull(buildConfigName, "buildName");
        Map<String, String> labels = BuildConfigs.createBuildLabels(buildConfigName);
        String gitUrlText = getOrFindGitUrl(context, gitUri.getValue());
        String imageText = image.getValue();
        List<EnvVar> envVars = createEnvVars(buildConfigName, gitUrlText, mavenCommand.getValue());
        BuildConfig buildConfig = BuildConfigs.createIntegrationTestBuildConfig(buildConfigName, labels, gitUrlText, imageText, envVars);

        LOG.info("Generated BuildConfig: " + toJson(buildConfig));

        ImageStream imageRepository = BuildConfigs.imageRepository(buildConfigName, labels);

        Controller controller = createController();
        controller.applyImageStream(imageRepository, "generated ImageStream: " + toJson(imageRepository));
        controller.applyBuildConfig(buildConfig, "generated BuildConfig: " + toJson(buildConfig));
        return Results.success("Added BuildConfig: " + Builds.getName(buildConfig) + " to OpenShift at master: " + getKubernetes().getMasterUrl());
    }

    private List<EnvVar> createEnvVars(String buildConfigName, String gitUrlText, String mavenCommand) {
        ListEnvVarBuilder builder = new ListEnvVarBuilder();
        builder.withEnvVar("BUILD_NAME", buildConfigName);
        builder.withEnvVar("SOURCE_URI", gitUrlText);
        builder.withEnvVar("MAVEN_COMMAND", mavenCommand);
        return builder.build();
    }

}

