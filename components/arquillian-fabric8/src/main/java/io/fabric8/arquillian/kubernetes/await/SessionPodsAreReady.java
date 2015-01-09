/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.arquillian.kubernetes.await;

import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.arquillian.utils.Util;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.PodStatus;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Objects;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.fabric8.arquillian.kubernetes.Constants.ARQ_KEY;

public class SessionPodsAreReady implements Callable<Boolean> {

    private final Session session;
    private final KubernetesClient kubernetesClient;

    public SessionPodsAreReady(KubernetesClient kubernetesClient, Session session) {
        this.session = session;
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public Boolean call() throws Exception {
        boolean result = true;
        Map<String, String> labels = Collections.singletonMap(ARQ_KEY, session.getId());
        Filter<Pod> podFilter = KubernetesHelper.createPodFilter(labels);
        List<Pod> pods = Util.findPods(kubernetesClient, podFilter);

        if (pods.isEmpty()) {
            result = false;
            session.getLogger().warn("No pods are available yet, waiting...");
        }

        for (Pod pod : pods) {
            result = result && Objects.equal(PodStatus.OK, KubernetesHelper.getPodStatus(pod));
            if (!result) {
                if (pod.getCurrentState().getInfo() != null) {
                    for (Map.Entry<String, ContainerStatus> entry : pod.getCurrentState().getInfo().entrySet()) {
                        String containerId = entry.getKey();
                        ContainerStatus status = entry.getValue();
                        if (status.getState().getWaiting() != null) {
                            session.getLogger().warn("Waiting for container:" + containerId + ". Reason:" + status.getState().getWaiting().getReason());
                        }
                    }
                }
            }
        }
        return result;
    }

}
