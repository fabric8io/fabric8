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
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.model.Pod;

import java.util.List;

/**
 * assertThat(kubernetesResource) entry points for testing Kubernetes
 */
public class Assertions extends io.fabric8.kubernetes.assertions.internal.Assertions {

    public static KubernetesAssert assertThat(KubernetesClient kubernetesClient) {
        return new KubernetesAssert(kubernetesClient);
    }

    public static PodsAssert assertThat(List<Pod> pods) {
        return new PodsAssert(pods);
    }
}
