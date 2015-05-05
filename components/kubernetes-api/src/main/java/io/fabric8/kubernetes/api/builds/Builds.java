/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api.builds;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import io.fabric8.utils.URLUtils;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 */
public class Builds {

    public static class Status {
        public static final String COMPLETE = "Complete";
        public static final String FAIL = "Fail";
        public static final String ERROR = "Error";
        public static final String CANCELLED = "Cancelled";
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
