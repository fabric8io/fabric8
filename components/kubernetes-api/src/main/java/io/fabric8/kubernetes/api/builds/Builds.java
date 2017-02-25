/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.kubernetes.api.builds;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.builders.ListEnvVarBuilder;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigSpec;
import io.fabric8.openshift.api.model.BuildSource;
import io.fabric8.openshift.api.model.BuildStrategy;
import io.fabric8.openshift.api.model.BuildTriggerPolicy;
import io.fabric8.openshift.api.model.BuildTriggerPolicyBuilder;
import io.fabric8.openshift.api.model.GitBuildSource;
import io.fabric8.openshift.api.model.JenkinsPipelineBuildStrategy;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import io.fabric8.utils.URLUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.fabric8.kubernetes.api.KubernetesHelper.getOrCreateMetadata;

public class Builds {

    public static final String DEFAULT_SECRET = "secret101";
    public static final String DEFAULT_BUILD_IMAGE_STREAM = "triggerJenkins";
    public static final String DEFAULT_IMAGE_TAG = "latest";
    public static final String DEFAULT_CUSTOM_BUILDER_IMAGE = "fabric8/openshift-s2i-jenkins-trigger";

    public static class Status {
        public static final String COMPLETE = "Complete";
        public static final String FAIL = "Fail";
        public static final String ERROR = "Error";
        public static final String CANCELLED = "Cancelled";
    }

    public static BuildConfig createDefaultBuildConfig(String name, String gitUrl, String jenkinsUrl) {
        BuildConfig buildConfig = new BuildConfig();
        getOrCreateMetadata(buildConfig).setName(name);
        boolean foundExistingGitUrl = false;
        return configureDefaultBuildConfig(buildConfig, name, gitUrl, foundExistingGitUrl, jenkinsUrl);
    }

    public static BuildConfig configureDefaultBuildConfig(BuildConfig buildConfig, String name, String gitUrl, boolean foundExistingGitUrl, String jenkinsUrl) {
        return configureDefaultBuildConfig(buildConfig, name, gitUrl, foundExistingGitUrl, DEFAULT_BUILD_IMAGE_STREAM,  DEFAULT_IMAGE_TAG,  DEFAULT_CUSTOM_BUILDER_IMAGE,  DEFAULT_SECRET,  jenkinsUrl);
    }

    public static BuildConfig configureDefaultBuildConfig(BuildConfig buildConfig, String name, String gitUrl, boolean foundExistingGitUrl, String buildImageStream, String buildImageTag, String s2iCustomBuilderImage, String secret, String jenkinsUrl) {
        BuildConfigSpec spec = buildConfig.getSpec();
        if (spec == null) {
            spec = new BuildConfigSpec();
            buildConfig.setSpec(spec);
        }

        if (!foundExistingGitUrl && Strings.isNotBlank(gitUrl)) {
            BuildSource source = spec.getSource();
            if (source == null) {
                source = new BuildSource();
                spec.setSource(source);
            }
            source.setType("Git");
            GitBuildSource git = source.getGit();
            if (git == null) {
                git = new GitBuildSource();
                source.setGit(git);
            }
            git.setUri(gitUrl);
        }

        if (Strings.isNotBlank(buildImageStream) && Strings.isNotBlank(buildImageTag)) {
            BuildStrategy strategy = spec.getStrategy();
            if (strategy == null) {
                strategy = new BuildStrategy();
                spec.setStrategy(strategy);
            }

            // TODO only do this if we are using Jenkins?
            strategy.setType("JenkinsPipeline");
            JenkinsPipelineBuildStrategy buildStrategy = strategy.getJenkinsPipelineStrategy();
            if (buildStrategy == null) {
                buildStrategy = new JenkinsPipelineBuildStrategy();
                strategy.setJenkinsPipelineStrategy(buildStrategy);
            }

            ListEnvVarBuilder envBuilder = new ListEnvVarBuilder();
            if (Strings.isNotBlank(jenkinsUrl)) {
                envBuilder.withEnvVar("BASE_URI", jenkinsUrl);
            }
            buildStrategy.setJenkinsfilePath("Jenkinsfile");
        }
        List<BuildTriggerPolicy> triggers = spec.getTriggers();
        if (triggers == null) {
            triggers = new ArrayList<>();
            }
        if (triggers.isEmpty()) {
            triggers.add(new BuildTriggerPolicyBuilder().withType("GitHub").withNewGithub().withSecret(secret).endGithub().build());
            triggers.add(new BuildTriggerPolicyBuilder().withType("Generic").withNewGeneric().withSecret(secret).endGeneric().build());
            spec.setTriggers(triggers);
        }
        return buildConfig;
    }

    public static boolean isCompleted(String status) {
        return Objects.equal(Status.COMPLETE, status);
    }

    public static boolean isCancelled(String status) {
        return Objects.equal(Status.CANCELLED, status);
    }

    public static boolean isFailed(String status) {
        if (status != null) {
            return status.startsWith(Status.FAIL) || status.startsWith(Status.ERROR);
        }
        return false;
    }

    public static boolean isFinished(String status) {
        return isCompleted(status) || isFailed(status) || isCancelled(status);
    }

    /**
     * Returns a unique UUID for a build
     */
    public static String getUid(Build build) {
        String answer = null;
        if (build != null) {
            answer = build.getMetadata().getUid();
            if (Strings.isNullOrBlank(answer)) {
                Map<String, Object> metadata = getMetadata(build);
                answer = getString(metadata, "uid");
                if (Strings.isNullOrBlank(answer)) {
                    answer = getString(metadata, "id");
                }
                if (Strings.isNullOrBlank(answer)) {
                    answer = getString(metadata, "name");
                }
            }
            if (Strings.isNullOrBlank(answer)) {
                answer = build.getMetadata().getName();
            }
        }
        return answer;
    }

    protected static String getString(Map<String, Object> metadata, String name) {
        Object answer = metadata.get(name);
        if (answer != null) {
            return answer.toString();
        }
        return null;
    }

    public static Map<String, Object> getMetadata(Build build) {
        if (build != null) {
            Map<String, Object> additionalProperties = build.getAdditionalProperties();
            if (additionalProperties != null) {
                Object metadata = additionalProperties.get("metadata");
                if (metadata instanceof Map) {
                    return (Map<String, Object>) metadata;
                }
            }
        }
        return Collections.EMPTY_MAP;

    }

    public static Map<String, Object> getMetadata(BuildConfig build) {
        if (build != null) {
            Map<String, Object> additionalProperties = build.getAdditionalProperties();
            if (additionalProperties != null) {
                Object metadata = additionalProperties.get("metadata");
                if (metadata instanceof Map) {
                    return (Map<String, Object>) metadata;
                }
            }
        }
        return Collections.EMPTY_MAP;

    }

    public static String getName(BuildConfig build) {
        String answer = null;
        if (build != null) {
            Map<String, Object> metadata = getMetadata(build);
            answer = getString(metadata, "name");
            if (Strings.isNullOrBlank(answer))  {
                answer = build.getMetadata().getName();
            }
        }
        return answer;
    }

    public static String getName(Build build) {
        String answer = null;
        if (build != null) {
            Map<String, Object> metadata = getMetadata(build);
            answer = getString(metadata, "name");
            if (Strings.isNullOrBlank(answer))  {
                answer = build.getMetadata().getName();
            }
        }
        return answer;
    }

    public static String getNamespace(Build build) {
        String answer = null;
        if (build != null) {
            Map<String, Object> metadata = getMetadata(build);
            answer = getString(metadata, "namespace");
            if (Strings.isNullOrBlank(answer))  {
                answer = build.getMetadata().getNamespace();
            }
        }
        return answer;
    }


    public static String getCreationTimestamp(Build build) {
        String answer = null;
        if (build != null) {
            Map<String, Object> metadata = getMetadata(build);
            answer = getString(metadata, "creationTimestamp");
            if (Strings.isNullOrBlank(answer))  {
                answer = build.getMetadata().getCreationTimestamp();
            }
        }
        return answer;
    }

    public static Date getCreationTimestampDate(Build build) {
        String text = getCreationTimestamp(build);
        if (Strings.isNullOrBlank(text)) {
            return null;
        } else {
            return KubernetesHelper.parseDate(text);
        }
    }


    public static String getBuildConfigName(Build build) {
        if (build != null) {
            Map<String, Object> metadata = getMetadata(build);
            Object labels = metadata.get("labels");
            if (labels instanceof Map) {
                Map<String,Object> labelMap = (Map<String,Object>) labels;
                return getString(labelMap, "buildconfig");
            }
        }
        return null;
    }

    /**
     * Returns the link to the build page in the console for the given build UUID
     */
    public static String createConsoleBuildLink(String fabricConsoleExternalUrl, String buildName) {
        return URLUtils.pathJoin(fabricConsoleExternalUrl, "kubernetes/builds", buildName);
    }

}
