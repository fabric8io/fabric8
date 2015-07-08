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
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerState;

import java.util.Map;

/**
 */
public class AppReplicationControllerSummaryDTO {
    private final String id;
    private final String namespace;
    private final Map<String, String> labels;
    private Integer replicas;
    private Map<String, String> replicaSelector;

    public AppReplicationControllerSummaryDTO(ReplicationController controller) {
        this.id = KubernetesHelper.getName(controller);
        this.namespace = controller.getNamespace();
        this.labels = controller.getLabels();
        ReplicationControllerState desiredState = controller.getDesiredState();
        if (desiredState != null) {
            this.replicas = desiredState.getReplicas();
            this.replicaSelector = desiredState.getReplicaSelector();
        }
    }

    @Override
    public String toString() {
        return "AppReplicationControllerSummaryDTO{" +
                "id='" + id + '\'' +
                ", namespace='" + namespace + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getNamespace() {
        return namespace;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public Map<String, String> getReplicaSelector() {
        return replicaSelector;
    }
}
