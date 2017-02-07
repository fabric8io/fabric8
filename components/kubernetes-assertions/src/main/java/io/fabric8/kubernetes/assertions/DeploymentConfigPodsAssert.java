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
import io.fabric8.kubernetes.api.model.LabelSelectorRequirement;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigAssert;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adds assertions for asserting that a Deployment starts up correctly etc
 */
public class DeploymentConfigPodsAssert extends DeploymentConfigAssert implements HasPodSelectionAssert {
    private final KubernetesClient client;

    public DeploymentConfigPodsAssert(KubernetesClient client, DeploymentConfig deployment) {
        super(deployment);
        this.client = client;
    }

    public PodSelectionAssert pods() {
        spec().isNotNull().selector().isNotNull();
        DeploymentConfigSpec spec = this.actual.getSpec();
        Integer replicas = spec.getReplicas();
        Map<String, String> matchLabels = spec.getSelector();
        List<LabelSelectorRequirement> matchExpressions = new ArrayList<>();
        return new PodSelectionAssert(client, replicas, matchLabels, matchExpressions, "DeploymentConfig " + KubernetesHelper.getName(actual));
    }
}
