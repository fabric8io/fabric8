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
package io.fabric8.kubernetes.api;

import io.fabric8.common.util.Strings;
import io.fabric8.kubernetes.api.model.Env;
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerListSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceListSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fabric8.common.util.Lists.notNullList;

/**
 */
public class KubernetesHelper {

    /**
     * Returns a list of {@link Env} objects from an environment variables map
     */
    public static List<Env> createEnv(Map<String, String> environmentVariables) {
        List<Env> answer = new ArrayList<>();
        Set<Map.Entry<String, String>> entries = environmentVariables.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            Env env = new Env();
            env.setName(entry.getKey());
            env.setValue(entry.getValue());
            answer.add(env);
        }
        return answer;
    }

    /**
     * Returns a map indexed by pod id of the pods
     */
    public static Map<String, PodSchema> toPodMap(PodListSchema podSchema) {
        return toPodMap(podSchema != null ? podSchema.getItems() : null);
    }

    /**
     * Returns a map indexed by pod id of the pods
     */
    public static Map<String, PodSchema> toPodMap(List<PodSchema> pods) {
        List<PodSchema> list = notNullList(pods);
        Map<String, PodSchema> answer = new HashMap<>();
        for (PodSchema podSchema : list) {
            String id = podSchema.getId();
            if (Strings.isNotBlank(id)) {
                answer.put(id, podSchema);
            }
        }
        return answer;
    }

    /**
     * Returns a map indexed by service id of the services
     */
    public static Map<String, ServiceSchema> toServiceMap(ServiceListSchema serviceSchema) {
        return toServiceMap(serviceSchema != null ? serviceSchema.getItems() : null);
    }

    /**
     * Returns a map indexed by service id of the services
     */
    public static Map<String, ServiceSchema> toServiceMap(List<ServiceSchema> services) {
        List<ServiceSchema> list = notNullList(services);
        Map<String, ServiceSchema> answer = new HashMap<>();
        for (ServiceSchema serviceSchema : list) {
            String id = serviceSchema.getId();
            if (Strings.isNotBlank(id)) {
                answer.put(id, serviceSchema);
            }
        }
        return answer;
    }

    /**
     * Returns a map indexed by replicationController id of the replicationControllers
     */
    public static Map<String, ReplicationControllerSchema> toReplicationControllerMap(ReplicationControllerListSchema replicationControllerSchema) {
        return toReplicationControllerMap(replicationControllerSchema != null ? replicationControllerSchema.getItems() : null);
    }

    /**
     * Returns a map indexed by replicationController id of the replicationControllers
     */
    public static Map<String, ReplicationControllerSchema> toReplicationControllerMap(List<ReplicationControllerSchema> replicationControllers) {
        List<ReplicationControllerSchema> list = notNullList(replicationControllers);
        Map<String, ReplicationControllerSchema> answer = new HashMap<>();
        for (ReplicationControllerSchema replicationControllerSchema : list) {
            String id = replicationControllerSchema.getId();
            if (Strings.isNotBlank(id)) {
                answer.put(id, replicationControllerSchema);
            }
        }
        return answer;
    }

    public static Map<String, PodSchema> getPodMap(Kubernetes kubernetes) {
        PodListSchema podSchema = kubernetes.getPods();
        return toPodMap(podSchema);
    }

    public static Map<String, ServiceSchema> getServiceMap(Kubernetes kubernetes) {
        return toServiceMap(kubernetes.getServices());
    }

    public static Map<String, ReplicationControllerSchema> getReplicationControllerMap(Kubernetes kubernetes) {
        return toReplicationControllerMap(kubernetes.getReplicationControllers());
    }
}
