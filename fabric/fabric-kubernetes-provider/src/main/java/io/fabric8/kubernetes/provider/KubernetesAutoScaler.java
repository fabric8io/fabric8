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

import io.fabric8.api.AutoScaleRequest;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;

import java.util.List;

/**
 */
public class KubernetesAutoScaler implements ContainerAutoScaler {
    public KubernetesAutoScaler(KubernetesContainerProvider containerProvider) {
    }

    @Override
    public int getWeight() {
        return 200;
    }

    @Override
    public void createContainers(AutoScaleRequest request) throws Exception {
        // TODO update the replicatorController metadata
    }

    @Override
    public void destroyContainers(String profile, int count, List<Container> containers) throws Exception {
        // TODO update the replicatorController metadata
    }
}
