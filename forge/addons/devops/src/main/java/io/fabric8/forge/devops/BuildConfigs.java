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

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildConfigSpec;
import io.fabric8.openshift.api.model.BuildConfigSpecBuilder;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.utils.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class BuildConfigs {

    public static Map<String, String> createBuildLabels(String buildName) {
        Map<String, String> answer = new HashMap<>();
        answer.put("name", buildName);
        return answer;
    }

    public static ImageStream imageRepository(String buildName, Map<String, String> labels) {
        return new ImageStreamBuilder().
                withNewMetadata().withLabels(labels).withName(buildName).endMetadata().
                build();
    }

    public static BuildConfigSpec addBuildParameterOutput(BuildConfigSpecBuilder builder, String imageName) {
        return builder.
                withNewOutput().
                withNewTo().withKind("ImageStreamImage").withName(imageName).endTo().
                endOutput().
                build();
    }

    public static BuildConfigSpec addBuildParameterGitSource(BuildConfigSpecBuilder builder, String gitUrl) {
        return builder.
                withNewSource().
                withType("Git").
                withNewGit().withUri(gitUrl).endGit().
                endSource().
                build();
    }

    public static BuildConfigSpecBuilder addBuildConfigSpectiStrategy(BuildConfigSpecBuilder builder, String image) {
        return builder.
                withNewStrategy().
                withType("STI").
                // TODO add builderImage
                withNewSourceStrategy().
                withNewFrom().withName(image).withKind("ImageStreamImage").endFrom().
                endSourceStrategy().
                endStrategy();
    }


    public static BuildConfigSpecBuilder addBuildParameterCustomStrategy(BuildConfigSpecBuilder builder, String image, List<EnvVar> envVars) {
        return builder.
                withNewStrategy().
                withType("Custom").
                withNewCustomStrategy().
                withNewFrom().withName(image).withKind("ImageStreamImage").endFrom().
                withEnv(envVars).
                endCustomStrategy().
                endStrategy();
    }

    public static BuildConfigSpecBuilder addWebHookTriggers(BuildConfigSpecBuilder builder, String secret) {
        return builder.
                addNewTrigger().
                withType("github").
                withNewGithub().withSecret(secret).endGithub().
                endTrigger().

                addNewTrigger().
                withType("generic").
                withNewGeneric().withSecret(secret).endGeneric().
                endTrigger();
    }

    public static BuildConfigBuilder buildConfigBuilder(String buildName, Map<String, String> labels) {
        return new BuildConfigBuilder().
                withNewMetadata().withLabels(labels).withName(buildName).endMetadata();
    }

    public static BuildConfig createBuildConfig(String buildConfigName, Map<String, String> labels, String gitUrlText, String outputImageStreamName, String imageText, String webhookSecret) {
        BuildConfigBuilder buildConfigBuilder = buildConfigBuilder(buildConfigName, labels);
        BuildConfigSpecBuilder specBuilder = new BuildConfigSpecBuilder();

        addBuildParameterGitSource(specBuilder, gitUrlText);
        if (Strings.isNotBlank(outputImageStreamName)) {
            addBuildParameterOutput(specBuilder, outputImageStreamName);
        }
        if (Strings.isNotBlank(imageText)) {
            addBuildConfigSpectiStrategy(specBuilder, imageText);
        }
        if (Strings.isNotBlank(webhookSecret)) {
            addWebHookTriggers(specBuilder, webhookSecret);
        }
        return buildConfigBuilder.withSpec(specBuilder.build()).build();
    }

    public static BuildConfig createIntegrationTestBuildConfig(String buildConfigName, Map<String, String> labels, String gitUrlText, String image, List<EnvVar> envVars) {
        BuildConfigBuilder buildConfigBuilder = buildConfigBuilder(buildConfigName, labels);
        BuildConfigSpecBuilder specBuilder = new BuildConfigSpecBuilder();
        addBuildParameterGitSource(specBuilder, gitUrlText);
        if (Strings.isNotBlank(image)) {
            addBuildParameterCustomStrategy(specBuilder, image, envVars);
        }
        return buildConfigBuilder.withSpec(specBuilder.build()).build();
    }
}
