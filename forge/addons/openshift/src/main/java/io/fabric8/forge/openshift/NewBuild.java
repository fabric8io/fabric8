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

import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.builds.Builds;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.ImageRepository;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import io.fabric8.utils.cxf.JsonHelper;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
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

/**
 * Creates a new build in OpenShift for the current project
 */
public class NewBuild extends AbstractOpenShiftCommand {
    @Inject
    @WithAttributes(name = "buildName", label = "Build name",
            description = "The build configuration name to generate.",
            required = false)
    UIInput<String> buildName;

    @Inject
    @WithAttributes(name = "imageTag", label = "Image Tag",
            description = "The image tag to use.",
            required = false, defaultValue = "test")
    UIInput<String> imageTag;

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

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.from(super.getMetadata(context), getClass())
                .category(Categories.create(CATEGORY))
                .name(CATEGORY + ": NewBuild")
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
        builder.add(imageTag);
        builder.add(gitUri);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        String buildConfigName = buildName.getValue();
        Objects.assertNotNull(buildConfigName, "buildName");
        Map<String, String> labels = BuildConfigs.createBuildLabels(buildConfigName);
        String imageTagText = imageTag.getValue();
        String gitUrlText = gitUri.getValue();
        Model mavenModel = getMavenModel(context);
        if (Strings.isNullOrBlank(gitUrlText)) {
            if (mavenModel != null) {
                Scm scm = mavenModel.getScm();
                if (scm != null) {
                    String connection = scm.getConnection();
                    if (Strings.isNotBlank(connection)) {
                        gitUrlText = connection;
                    }
                }
            }
        }
        if (Strings.isNullOrBlank(gitUrlText)) {
            throw new IllegalArgumentException("Could not find git URL");
        }
        String imageText = outputImage.getValue();
        if (Strings.isNullOrBlank(imageText) && mavenModel != null) {
            imageText = mavenModel.getProperties().getProperty("docker.image");
        }

        BuildConfig buildConfig = BuildConfigs.createBuildConfig(buildConfigName, labels, gitUrlText, imageTagText, imageText);

        ImageRepository imageRepository = BuildConfigs.imageRepository(buildConfigName, labels);

        Controller controller = createController();
        controller.applyImageRepository(imageRepository, "generated ImageRepository: " + JsonHelper.toJson(imageRepository));
        controller.applyBuildConfig(buildConfig, "generated BuildConfig: " + JsonHelper.toJson(buildConfig));
        return Results.success("Added BuildConfig: " + Builds.getName(buildConfig) + " to OpenShift at master: " + getKubernetes().getAddress());
    }

}

