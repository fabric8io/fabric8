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

import io.fabric8.kubernetes.api.model.ControllerDesiredState;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;

import java.util.Map;

/**
 */
public class AppReplicationControllerSummaryDTO {
    private final String id;
    private final String namespace;
    private final Map<String, String> labels;
    private Integer replicas;
    private Map<String, String> replicaSelector;

    public AppReplicationControllerSummaryDTO(ReplicationControllerSchema controller) {
        this.id = controller.getId();
        this.namespace = controller.getNamespace();
        this.labels = controller.getLabels();
        ControllerDesiredState desiredState = controller.getDesiredState();
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
