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
package io.fabric8.arquillian.kubernetes.await;

import io.fabric8.arquillian.kubernetes.Session;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.PodStatusType;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Objects;

import java.util.List;
import java.util.concurrent.Callable;

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
        List<Pod> pods = kubernetesClient.pods().inNamespace(session.getNamespace()).list().getItems();

        if (pods.isEmpty()) {
            result = false;
            session.getLogger().warn("No pods are available yet, waiting...");
        }

        for (Pod pod : pods) {
            if (!KubernetesHelper.isPodReady(pod)) {
                result = false;
                PodStatus podStatus = pod.getStatus();
                if (podStatus != null) {
                    List<ContainerStatus> containerStatuses = podStatus.getContainerStatuses();
                    for (ContainerStatus containerStatus : containerStatuses) {
                        ContainerState state = containerStatus.getState();
                        if (state != null) {
                            ContainerStateWaiting waiting = state.getWaiting();
                            String containerName = containerStatus.getName();
                            if (waiting != null) {
                                session.getLogger().warn("Waiting for container:" + containerName + ". Reason:" + waiting.getReason());
                            } else {
                                session.getLogger().warn("Waiting for container:" + containerName + ".");
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

}
