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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.fabric8.common.util.Files;
import io.fabric8.common.util.Filter;
import io.fabric8.common.util.Filters;
import io.fabric8.common.util.Objects;
import io.fabric8.common.util.Strings;
import io.fabric8.kubernetes.api.model.CurrentState;
import io.fabric8.kubernetes.api.model.DesiredState;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.ManifestSchema;
import io.fabric8.kubernetes.api.model.PodContainerManifest;
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerListSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceListSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fabric8.common.util.Lists.notNullList;
import static io.fabric8.common.util.Strings.isNullOrBlank;

/**
 */
public class KubernetesHelper {
    public static final String DEFAULT_DOCKER_HOST = "tcp://localhost:2375";
    private static final transient Logger LOG = LoggerFactory.getLogger(KubernetesHelper.class);
    private static ObjectMapper objectMapper = KubernetesFactory.createObjectMapper();


    public static String getDockerIp() {
        String url = resolveDockerHost();
        int idx = url.indexOf("://");
        if (idx > 0) {
            url = url.substring(idx + 3);
        }
        idx = url.indexOf(":");
        if (idx > 0) {
            url = url.substring(0, idx);
        }
        return url;
    }

    public static String resolveDockerHost() {
        String dockerHost = System.getenv("DOCKER_HOST");
        if (isNullOrBlank(dockerHost)) {
            dockerHost = System.getProperty("docker.host");
        }
        if (isNullOrBlank(dockerHost)) {
            return DEFAULT_DOCKER_HOST;
        } else {
            return dockerHost;
        }
    }

    /**
     * Returns the given json data as a DTO such as
     * {@link PodSchema}, {@link ReplicationControllerSchema} or
     * {@link io.fabric8.kubernetes.api.model.ServiceSchema}
     * from the Kubernetes REST API or
     * {@link JsonNode} if it cannot be recognised.
     */
    public static Object loadJson(File file) throws IOException {
        byte[] data = Files.readBytes(file);
        return loadJson(data);
    }

    /**
     * Returns the given json data as a DTO such as
     * {@link PodSchema}, {@link ReplicationControllerSchema} or
     * {@link io.fabric8.kubernetes.api.model.ServiceSchema}
     * from the Kubernetes REST API or
     * {@link JsonNode} if it cannot be recognised.
     */
    public static Object loadJson(InputStream in) throws IOException {
        byte[] data = Files.readBytes(in);
        return loadJson(data);
    }

    public static Object loadJson(String json) throws IOException {
        byte[] data = json.getBytes();
        return loadJson(data);
    }

    /**
     * Returns the given json data as a DTO such as
     * {@link PodSchema}, {@link ReplicationControllerSchema} or
     * {@link io.fabric8.kubernetes.api.model.ServiceSchema}
     * from the Kubernetes REST API or
     * {@link JsonNode} if it cannot be recognised.
     */
    public static Object loadJson(byte[] json) throws IOException {
        if (json != null && json.length > 0) {
            ObjectReader reader = objectMapper.reader();
            JsonNode tree = reader.readTree(new ByteArrayInputStream(json));
            if (tree != null) {
                JsonNode kindNode = tree.get("kind");
                if (kindNode != null) {
                    String kind = kindNode.asText();
                    if (Objects.equal("Pod", kind)) {
                        return objectMapper.reader(PodSchema.class).readValue(json);
                    } else if (Objects.equal("ReplicationController", kind)) {
                        return objectMapper.reader(ReplicationControllerSchema.class).readValue(json);
                    } else if (Objects.equal("Service", kind)) {
                        return objectMapper.reader(ServiceSchema.class).readValue(json);
                    } else {
                        return tree;
                    }
                } else {
                    LOG.warn("No JSON type for: " + tree);
                }
                return tree;
            }
        }
        return null;
    }


    /**
     * Returns a map indexed by pod id of the pods
     */
    public static Map<String, PodSchema> toPodMap(PodListSchema podSchema) {
        return toPodMap(podSchema, null);
    }

    /**
     * Returns a map indexed by pod id of the pods
     */
    public static Map<String, PodSchema> toPodMap(PodListSchema podSchema, String selector) {
        List<PodSchema> list = podSchema != null ? podSchema.getItems() : null;
        List<PodSchema> filteredList = Filters.filter(list, createPodFilter(selector));
        return toPodMap(filteredList);
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
        return toReplicationControllerMap(replicationControllerSchema, null);
    }


    private static Map<String,ReplicationControllerSchema> toReplicationControllerMap(ReplicationControllerListSchema replicationControllerSchema, String selector) {
        List<ReplicationControllerSchema> list = replicationControllerSchema != null ? replicationControllerSchema.getItems() : null;
        List<ReplicationControllerSchema> filteredList = Filters.filter(list, createReplicationControllerFilter(selector));
        return toReplicationControllerMap(filteredList);
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
        return getPodMap(kubernetes, null);
    }


    public static Map<String, PodSchema> getPodMap(Kubernetes kubernetes, String selector) {
        PodListSchema podSchema = kubernetes.getPods();
        return toPodMap(podSchema, selector);
    }

    public static Map<String, ServiceSchema> getServiceMap(Kubernetes kubernetes) {
        return toServiceMap(kubernetes.getServices());
    }

    public static Map<String, ReplicationControllerSchema> getReplicationControllerMap(Kubernetes kubernetes) {
        return toReplicationControllerMap(kubernetes.getReplicationControllers());
    }

    public static Map<String, ReplicationControllerSchema> getReplicationControllerMap(Kubernetes kubernetes, String selector) {
        return toReplicationControllerMap(kubernetes.getReplicationControllers(), selector);
    }

    /**
     * Removes empty pods returned by Kubernetes
     */
    public static void removeEmptyPods(PodListSchema podSchema) {
        List<PodSchema> list = notNullList(podSchema.getItems());

        List<PodSchema> removeItems = new ArrayList<PodSchema>();

        for (PodSchema serviceSchema : list) {
            if (StringUtils.isEmpty(serviceSchema.getId())) {
                removeItems.add(serviceSchema);

            }
        }
        list.removeAll(removeItems);
    }

    /**
     * Returns the pod id for the given container id
     */
    public static String containerNameToPodId(String containerName) {
        // TODO use prefix?
        return containerName;
    }

    /**
     * Returns a string for the labels using "," to separate values
     */
    public static String toLabelsString(Map<String, String> labelMap) {
        StringBuilder buffer = new StringBuilder();
        if (labelMap != null) {
            Set<Map.Entry<String, String>> entries = labelMap.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                if (buffer.length() > 0) {
                    buffer.append(",");
                }
                buffer.append(entry.getKey());
                buffer.append("=");
                buffer.append(entry.getValue());
            }
        }
        return buffer.toString();
    }

    /**
     * Creates a filter on a pod using the given text string
     */
    public static Filter<PodSchema> createPodFilter(final String textFilter) {
        if (isNullOrBlank(textFilter)) {
            return Filters.<PodSchema>trueFilter();
        } else {
            return new Filter<PodSchema>() {
                public String toString() {
                    return "PodFilter(" + textFilter + ")";
                }

                public boolean matches(PodSchema entity) {
                    return filterMatchesIdOrLabels(textFilter, entity.getId(), entity.getLabels());
                }
            };
        }
    }

    /**
     * Creates a filter on a service using the given text string
     */
    public static Filter<ServiceSchema> createServiceFilter(final String textFilter) {
        if (isNullOrBlank(textFilter)) {
            return Filters.<ServiceSchema>trueFilter();
        } else {
            return new Filter<ServiceSchema>() {
                public String toString() {
                    return "ServiceFilter(" + textFilter + ")";
                }

                public boolean matches(ServiceSchema entity) {
                    return filterMatchesIdOrLabels(textFilter, entity.getId(), entity.getLabels());
                }
            };
        }
    }

    /**
     * Creates a filter on a replicationController using the given text string
     */
    public static Filter<ReplicationControllerSchema> createReplicationControllerFilter(final String textFilter) {
        if (isNullOrBlank(textFilter)) {
            return Filters.<ReplicationControllerSchema>trueFilter();
        } else {
            return new Filter<ReplicationControllerSchema>() {
                public String toString() {
                    return "ReplicationControllerFilter(" + textFilter + ")";
                }

                public boolean matches(ReplicationControllerSchema entity) {
                    return filterMatchesIdOrLabels(textFilter, entity.getId(), entity.getLabels());
                }
            };
        }
    }

    /**
     * Returns true if the given textFilter matches either the id or the labels
     */
    public static boolean filterMatchesIdOrLabels(String textFilter, String id, Map<String, String> labels) {
        String text = toLabelsString(labels);
        return (text != null && text.contains(textFilter)) || (id != null && id.contains(textFilter));
    }


    /**
     * For positive non-zero values return the text of the number or return blank
     */
    public static String toPositiveNonZeroText(Integer port) {
        if (port != null) {
            int value = port.intValue();
            if (value > 0) {
                return "" + value;
            }
        }
        return "";
    }

    /**
     * Returns all the containers from the given pod
     */
    public static List<ManifestContainer> getContainers(PodSchema pod) {
        if (pod != null) {
            DesiredState desiredState = pod.getDesiredState();
            return getContainers(desiredState);

        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns all the containers from the given Replication Controller
     */
    public static List<ManifestContainer> getContainers(ReplicationControllerSchema replicationController) {
        if (replicationController != null) {
            // TODO
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns all the containers from the given Replication Controller
     */
    public static List<ManifestContainer> getCurrentContainers(ReplicationControllerSchema replicationController) {
        if (replicationController != null) {
            // TODO
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns all the manifest containers from the given desiredState
     */
    public static List<ManifestContainer> getContainers(DesiredState desiredState) {
        if (desiredState != null) {
            ManifestSchema manifest = desiredState.getManifest();
            if (manifest != null) {
                List<ManifestContainer> containers = manifest.getContainers();
                if (containers != null) {
                    return containers;
                }
            }
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns all the current containers from the given currentState
     */
    public static List<ManifestContainer> getCurrentContainers(PodSchema pod) {
        if (pod != null) {
            CurrentState currentState = pod.getCurrentState();
            return getCurrentContainers(currentState);

        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns all the current containers from the given currentState
     */
    public static List<ManifestContainer> getCurrentContainers(CurrentState currentState) {
        if (currentState != null) {
            PodContainerManifest manifest = currentState.getManifest();
            if (manifest != null) {
/*
                // TODO...
                List<ManifestContainer> containers = manifest.getContainers();
                if (containers != null) {
                    return containers;
                }
*/
            }
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns the host of the pod
     */
    public static String getHost(PodSchema pod) {
        if (pod != null) {
            CurrentState currentState = pod.getCurrentState();
            if (currentState != null) {
                return currentState.getHost();
            }
        }
        return null;
    }

}
