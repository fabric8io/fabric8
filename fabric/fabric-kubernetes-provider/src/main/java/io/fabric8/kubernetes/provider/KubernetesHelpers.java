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
package io.fabric8.kubernetes.provider;

import io.fabric8.common.util.Filter;
import io.fabric8.common.util.Filters;
import io.fabric8.common.util.Strings;
import io.fabric8.kubernetes.api.model.PodSchema;

import java.util.Map;
import java.util.Set;

/**
 */
public class KubernetesHelpers {
    /**
     * Returns the pod id for the given container id
     */
    public static String containerNameToPodId(String containerName) {
        // TODO use prefix?
        return containerName;
    }

    /**
     * Returns a string for the labels using "," to separate values
     */
    public static String toLabelsString(Map<String, String> labelMap) {
        StringBuilder buffer = new StringBuilder();
        Set<Map.Entry<String, String>> entries = labelMap.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (buffer.length() > 0) {
                buffer.append(",");
            }
            buffer.append(entry.getKey());
            buffer.append("=");
            buffer.append(entry.getValue());
        }
        return buffer.toString();
    }

    /**
     * Creates a filter on a pod using the given text string
     */
    public static Filter<PodSchema> createFilter(final String textFilter) {
        if (Strings.isNullOrBlank(textFilter)) {
            return Filters.<PodSchema>trueFilter();
        } else {
            return new Filter<PodSchema>() {
                public String toString() {
                    return "PodFilter(" + textFilter + ")";
                }
                public boolean matches(PodSchema pod) {
                    String text = toLabelsString(pod.getLabels());
                    return text.contains(textFilter) || pod.getId().contains(textFilter);
                }
            };
        }
    }
}
