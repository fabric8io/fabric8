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
import io.fabric8.kubernetes.api.builds.Builds;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
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

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.fabric8.utils.cxf.JsonHelper.toJson;

/**
 * Creates a new build in OpenShift for the current project
 */
public class NewBuildCommand extends AbstractDevOpsCommand {

    @Inject
    @WithAttributes(name = "buildName", label = "Build name",
            description = "The build configuration name to generate.",
            required = false)
    UIInput<String> buildName;

    @Inject
    @WithAttributes(name = "imageName", label = "Output Image Name",
            description = "The output image name to push the docker image to.",
            required = false)
    UIInput<String> imageName;

    @Inject
    @WithAttributes(name = "gitUri", label = "Git Uri",
            description = "If the git URI is not specified in the pom.xml then this allows you to specify one to be used.",
            required = false)
    UIInput<String> gitUri;

    @Inject
    @WithAttributes(name = "outputImage", label = "Output image",
            description = "The docker image name to generate.",
            required = false)
    UIInput<String> outputImage;

    @Inject
    @WithAttributes(name = "webHookSecret", label = "Webhook secret",
            description = "The secret that needs to be passed in by webhooks invoking the generated build.",
            required = false)
    UIInput<String> webHookSecret;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.from(super.getMetadata(context), getClass())
                .category(Categories.create(CATEGORY))
                .name(CATEGORY + ": New Build")
                .description("Create a new build configuration");
    }

    @Override
    public void initializeUI(final UIBuilder builder) throws Exception {
        super.initializeUI(builder);

        buildName.setDefaultValue(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Model mavenModel = getMavenModel(builder);
                if (mavenModel != null) {
                    return mavenModel.getArtifactId();
                }
                return null;
            }
        });
        builder.add(buildName);
        builder.add(imageName);
        builder.add(gitUri);
        builder.add(outputImage);
        builder.add(webHookSecret);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        String buildConfigName = buildName.getValue();
        Objects.assertNotNull(buildConfigName, "buildName");
        Map<String, String> labels = BuildConfigs.createBuildLabels(buildConfigName);
        String ouputImageName = imageName.getValue();
        String gitUrlText = getOrFindGitUrl(context, gitUri.getValue());
        String imageText = outputImage.getValue();
        Model mavenModel = getMavenModel(context);
        if (Strings.isNullOrBlank(imageText) && mavenModel != null) {
            imageText = mavenModel.getProperties().getProperty("docker.image");
        }

        String webhookSecretText = webHookSecret.getValue();
        if (Strings.isNullOrBlank(webhookSecretText)) {
            // TODO generate a really good secret!
            webhookSecretText = "secret101";
        }
        BuildConfig buildConfig = BuildConfigs.createBuildConfig(buildConfigName, labels, gitUrlText, ouputImageName, imageText, webhookSecretText);

        System.out.println("Generated BuildConfig: " + toJson(buildConfig));

        ImageStream imageRepository = BuildConfigs.imageRepository(buildConfigName, labels);

        Controller controller = createController();
        controller.applyImageStream(imageRepository, "generated ImageStream: " + toJson(imageRepository));
        controller.applyBuildConfig(buildConfig, "generated BuildConfig: " + toJson(buildConfig));
        return Results.success("Added BuildConfig: " + Builds.getName(buildConfig) + " to OpenShift at master: " + getKubernetes().getMasterUrl());
    }

}

