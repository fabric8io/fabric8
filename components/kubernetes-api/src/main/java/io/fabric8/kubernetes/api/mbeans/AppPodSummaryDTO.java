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
package io.fabric8.kubernetes.api.mbeans;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.PodStatus;
import io.fabric8.kubernetes.api.model.Pod;

import java.util.Map;

/**
 */
public class AppPodSummaryDTO {
    private final String id;
    private final String namespace;
    private final PodStatus status;
    private final Map<String, String> labels;

    public AppPodSummaryDTO(Pod pod) {
        this.id = KubernetesHelper.getId(pod);
        this.namespace = pod.getNamespace();
        this.status = KubernetesHelper.getPodStatus(pod);
        this.labels = pod.getLabels();
    }

    @Override
    public String toString() {
        return "AppPodSummaryDTO{" +
                "id='" + id + '\'' +
                ", namespace='" + namespace + '\'' +
                ", status=" + status +
                '}';
    }

    public String getId() {
        return id;
    }

    public PodStatus getStatus() {
        return status;
    }

    public String getNamespace() {
        return namespace;
    }

    public Map<String, String> getLabels() {
        return labels;
    }
}
