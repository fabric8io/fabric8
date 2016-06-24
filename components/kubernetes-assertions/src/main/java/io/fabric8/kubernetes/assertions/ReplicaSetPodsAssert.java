/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpec;
import io.fabric8.kubernetes.api.model.extensions.LabelSelector;
import io.fabric8.kubernetes.api.model.extensions.LabelSelectorRequirement;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSetAssert;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.List;
import java.util.Map;

/**
 */
public class ReplicaSetPodsAssert extends ReplicaSetAssert implements HasPodSelectionAssert {
    private final KubernetesClient client;

    public ReplicaSetPodsAssert(KubernetesClient client, ReplicaSet replicasSet) {
        super(replicasSet);
        this.client = client;
    }


    @Override
    public PodSelectionAssert pods() {
        spec().isNotNull().selector().isNotNull();
        ReplicaSetSpec spec = this.actual.getSpec();
        Integer replicas = spec.getReplicas();
        LabelSelector selector = spec.getSelector();
        Map<String, String> matchLabels = selector.getMatchLabels();
        List<LabelSelectorRequirement> matchExpressions = selector.getMatchExpressions();
        return new PodSelectionAssert(client, replicas, matchLabels, matchExpressions, "ReplicaSet " + KubernetesHelper.getName(actual));
    }
}
