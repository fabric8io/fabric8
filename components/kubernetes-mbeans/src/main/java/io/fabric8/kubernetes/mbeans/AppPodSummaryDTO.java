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
package io.fabric8.kubernetes.mbeans;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.PodStatusType;
import io.fabric8.kubernetes.api.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public class AppPodSummaryDTO {
    private final String id;
    private final String namespace;
    private final PodStatusType status;
    private final Map<String, String> labels;
    private final Set<Integer> containerPorts = new HashSet<>();
    private final String creationTimestamp;
    private String podIP;
    private String host;

    public AppPodSummaryDTO(Pod pod) {
        this.id = KubernetesHelper.getName(pod);
        this.namespace = pod.getNamespace();
        this.status = KubernetesHelper.getPodStatus(pod);
        this.labels = pod.getLabels();
        this.creationTimestamp = pod.getCreationTimestamp();

        PodState currentState = pod.getCurrentState();
        PodState desiredState = pod.getDesiredState();
        if (currentState != null) {
            this.podIP = currentState.getPodIP();
            this.host = currentState.getHost();
        }
        if (desiredState != null) {
            ContainerManifest manifest = desiredState.getManifest();
            if (manifest != null) {
                List<Container> containers = manifest.getContainers();
                if (containers != null) {
                    for (Container container : containers) {
                        List<ContainerPort> ports = container.getPorts();
                        if (ports != null) {
                            for (ContainerPort port : ports) {
                                Integer containerPort = port.getContainerPort();
                                if (containerPort != null) {
                                    containerPorts.add(containerPort);
                                }
                            }
                        }
                    }
                }
            }
        }
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

    public PodStatusType getStatus() {
        return status;
    }

    public String getNamespace() {
        return namespace;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public Set<Integer> getContainerPorts() {
        return containerPorts;
    }

    public String getPodIP() {
        return podIP;
    }

    public String getHost() {
        return host;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }
}
