/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.kubernetes.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerManifest;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodState;
import io.fabric8.kubernetes.api.model.PodTemplate;
import io.fabric8.kubernetes.api.model.Port;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.ReplicationControllerState;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.utils.Files;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Filters;
import io.fabric8.utils.Maps;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fabric8.utils.Lists.notNullList;
import static io.fabric8.utils.Strings.isNullOrBlank;

/*
TODO Config
import io.fabric8.kubernetes.api.model.Item;
import io.fabric8.kubernetes.api.model.Template;
*/

/**
 */
public class KubernetesHelper {
    public static final String DEFAULT_DOCKER_HOST = "tcp://localhost:2375";
    private static final transient Logger LOG = LoggerFactory.getLogger(KubernetesHelper.class);
    private static ObjectMapper objectMapper = KubernetesFactory.createObjectMapper();


    public static String getId(Pod entity) {
        if (entity != null) {
            // TODO no additional properties!
            return Strings.firstNonBlank(entity.getName(), entity.getUid(), getAdditionalProperty(null, "id"));
        } else {
            return null;
        }
    }

    public static String getId(ReplicationController entity) {
        if (entity != null) {
            // TODO no additional properties!
            return Strings.firstNonBlank(entity.getName(), entity.getUid(), getAdditionalProperty(null, "id"));
        } else {
            return null;
        }
    }

    public static String getId(Service entity) {
        if (entity != null) {
            // TODO no additional properties!
            return Strings.firstNonBlank(entity.getName(), entity.getUid(), getAdditionalProperty(null, "id"));
        } else {
            return null;
        }
    }

    public static String getPortalIP(Service entity) {
        if (entity != null) {
            ServiceSpec spec = entity.getSpec();
            if (spec != null) {
                return spec.getPortalIP();
            }
        }
        return null;
    }

    public static Map<String, String> getSelector(Service entity) {
        if (entity != null) {
            ServiceSpec spec = entity.getSpec();
            if (spec != null) {
                return spec.getSelector();
            }
        }
        return Collections.EMPTY_MAP;
    }

    public static Integer getPort(Service entity) {
        if (entity != null) {
            ServiceSpec spec = entity.getSpec();
            if (spec != null) {
                return spec.getPort();
            }
        }
        return null;
    }

    private static String getAdditionalProperty(Map<String, String> additionalProperties, String name) {
        if (additionalProperties != null) {
            return additionalProperties.get(name);
        } else {
            return null;
        }
    }

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

    public static String toJson(Object dto) throws JsonProcessingException {
        Class<?> clazz = dto.getClass();
        return objectMapper.writerWithType(clazz).writeValueAsString(dto);
    }

    /**
     * Returns the given json data as a DTO such as
     * {@link Pod}, {@link ReplicationController} or
     * {@link io.fabric8.kubernetes.api.model.Service}
     * from the Kubernetes REST API or
     * {@link JsonNode} if it cannot be recognised.
     */
    public static Object loadJson(File file) throws IOException {
        byte[] data = Files.readBytes(file);
        return loadJson(data);
    }

    /**
     * Returns the given json data as a DTO such as
     * {@link Pod}, {@link ReplicationController} or
     * {@link io.fabric8.kubernetes.api.model.Service}
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
     * {@link Pod}, {@link ReplicationController} or
     * {@link io.fabric8.kubernetes.api.model.Service}
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
                    return loadEntity(json, kind, tree);
                } else {
                    LOG.warn("No JSON type for: " + tree);
                }
                return tree;
            }
        }
        return null;
    }

    protected static Object loadEntity(byte[] json, String kind, Object defaultValue) throws IOException {
        if (Objects.equal("Pod", kind)) {
            return objectMapper.reader(Pod.class).readValue(json);
        } else if (Objects.equal("ReplicationController", kind)) {
            return objectMapper.reader(ReplicationController.class).readValue(json);
        } else if (Objects.equal("Service", kind)) {
            return objectMapper.reader(Service.class).readValue(json);
        } else if (Objects.equal("Config", kind)) {
            return loadConfig(json);
/*
        } else if (Objects.equal("Template", kind)) {
            return objectMapper.reader(Template.class).readValue(json);
*/
        } else {
            return defaultValue;
        }
    }

    protected static Config loadConfig(byte[] data) throws IOException {
        Config config = new Config();
        List<Object> itemList = new ArrayList<>();
        config.setItems(itemList);
        JsonNode jsonNode = objectMapper.readTree(data);
        JsonNode items = jsonNode.get("items");
        for (JsonNode item : items) {
            if (item != null) {
                JsonNode kindObject = item.get("kind");
                if (kindObject != null && kindObject.isTextual()) {
                    String kind = kindObject.asText();
                    String json = toJson(item);
                    byte[] bytes = json.getBytes();
                    Object entity = loadEntity(bytes, kind, null);
                    if (entity != null) {
                        itemList.add(entity);
                    }
                }
            }
        }
        return config;
    }

    /**
     * Loads the entity for the given item
     */
    public static Object getEntity(JsonNode item) throws IOException {
        if (item != null) {
            JsonNode kindObject = item.get("kind");
            if (kindObject != null && kindObject.isTextual()) {
                String kind = kindObject.asText();
                if (kind != null) {
                    String json = toJson(item);
                    byte[] bytes = json.getBytes();
                    Object entity = loadEntity(bytes, kind, null);
                    return entity;
                }
            }
        }
        return null;
    }

    /**
     * Returns the items inside the config as a list of {@link Entity} objects
     */
    public static List<Object> getEntities(Config config) throws IOException {
        return config.getItems();
    }


    /**
     * Saves the json object to the given file
     */
    public static void saveJson(File json, Object object) throws IOException {
        objectMapper.writer().writeValue(json, object);
    }

    /**
     * Returns a map indexed by pod id of the pods
     */
    public static Map<String, Pod> toPodMap(PodList podSchema) {
        return toPodMap(podSchema, null);
    }

    /**
     * Returns a map indexed by pod id of the pods
     */
    public static Map<String, Pod> toPodMap(PodList podSchema, String selector) {
        List<Pod> list = podSchema != null ? podSchema.getItems() : null;
        List<Pod> filteredList = Filters.filter(list, createPodFilter(selector));
        return toPodMap(filteredList);
    }

    /**
     * Returns a map indexed by pod id of the pods
     */
    public static Map<String, Pod> toPodMap(List<Pod> pods) {
        List<Pod> list = notNullList(pods);
        Map<String, Pod> answer = new HashMap<>();
        for (Pod pod : list) {
            String id = getId(pod);
            if (Strings.isNotBlank(id)) {
                answer.put(id, pod);
            }
        }
        return answer;
    }

    /**
     * Returns a map indexed by service id of the services
     */
    public static Map<String, Service> toServiceMap(ServiceList serviceSchema) {
        return toServiceMap(serviceSchema != null ? serviceSchema.getItems() : null);
    }

    /**
     * Returns a map indexed by service id of the services
     */
    public static Map<String, Service> toServiceMap(List<Service> services) {
        List<Service> list = notNullList(services);
        Map<String, Service> answer = new HashMap<>();
        for (Service service : list) {
            String id = getId(service);
            if (Strings.isNotBlank(id)) {
                answer.put(id, service);
            }
        }
        return answer;
    }

    /**
     * Returns a map indexed by replicationController id of the replicationControllers
     */
    public static Map<String, ReplicationController> toReplicationControllerMap(ReplicationControllerList replicationControllerSchema) {
        return toReplicationControllerMap(replicationControllerSchema, null);
    }


    private static Map<String, ReplicationController> toReplicationControllerMap(ReplicationControllerList replicationControllerSchema, String selector) {
        List<ReplicationController> list = replicationControllerSchema != null ? replicationControllerSchema.getItems() : null;
        List<ReplicationController> filteredList = Filters.filter(list, createReplicationControllerFilter(selector));
        return toReplicationControllerMap(filteredList);
    }


    /**
     * Returns a map indexed by replicationController id of the replicationControllers
     */
    public static Map<String, ReplicationController> toReplicationControllerMap(List<ReplicationController> replicationControllers) {
        List<ReplicationController> list = notNullList(replicationControllers);
        Map<String, ReplicationController> answer = new HashMap<>();
        for (ReplicationController replicationControllerSchema : list) {
            String id = getId(replicationControllerSchema);
            if (Strings.isNotBlank(id)) {
                answer.put(id, replicationControllerSchema);
            }
        }
        return answer;
    }

    public static Map<String, Pod> getPodMap(Kubernetes kubernetes) {
        return getPodMap(kubernetes, null);
    }


    public static Map<String, Pod> getPodMap(Kubernetes kubernetes, String selector) {
        PodList podSchema = kubernetes.getPods();
        return toPodMap(podSchema, selector);
    }

    public static Map<String, Service> getServiceMap(Kubernetes kubernetes) {
        return toServiceMap(kubernetes.getServices());
    }

    public static Map<String, ReplicationController> getReplicationControllerMap(Kubernetes kubernetes) {
        return toReplicationControllerMap(kubernetes.getReplicationControllers());
    }

    public static Map<String, ReplicationController> getReplicationControllerMap(Kubernetes kubernetes, String selector) {
        return toReplicationControllerMap(kubernetes.getReplicationControllers(), selector);
    }

    /**
     * Removes empty pods returned by Kubernetes
     */
    public static void removeEmptyPods(PodList podSchema) {
        List<Pod> list = notNullList(podSchema.getItems());

        List<Pod> removeItems = new ArrayList<Pod>();

        for (Pod pod : list) {
            if (StringUtils.isEmpty(getId(pod))) {
                removeItems.add(pod);

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

    public static Map<String, String> toLabelsMap(String labels) {
        Map<String, String> map = new HashMap<>();
        if (labels != null && !labels.isEmpty()) {
            String[] elements = labels.split(",");
            if (elements.length > 0) {
                for (String str : elements) {
                    String[] keyValue = str.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = keyValue[1];
                        if (key != null && value != null) {
                            map.put(key.trim(), value.trim());
                        }
                    }
                }
            }
        }
        return map;
    }

    /**
     * Creates a filter on a pod using the given text string
     */
    public static Filter<Pod> createPodFilter(final String textFilter) {
        if (isNullOrBlank(textFilter)) {
            return Filters.<Pod>trueFilter();
        } else {
            return new Filter<Pod>() {
                public String toString() {
                    return "PodFilter(" + textFilter + ")";
                }

                public boolean matches(Pod entity) {
                    return filterMatchesIdOrLabels(textFilter, getId(entity), entity.getLabels());
                }
            };
        }
    }

    /**
     * Creates a filter on a pod using the given set of labels
     */
    public static Filter<Pod> createPodFilter(final Map<String, String> labelSelector) {
        if (labelSelector == null || labelSelector.isEmpty()) {
            return Filters.<Pod>trueFilter();
        } else {
            return new Filter<Pod>() {
                public String toString() {
                    return "PodFilter(" + labelSelector + ")";
                }

                public boolean matches(Pod entity) {
                    return filterLabels(labelSelector, entity.getLabels());
                }
            };
        }
    }

    /**
     * Creates a filter on a pod annotations using the given set of attribute values
     */
    public static Filter<Pod> createPodAnnotationFilter(final Map<String, String> annotationSelector) {
        if (annotationSelector == null || annotationSelector.isEmpty()) {
            return Filters.<Pod>trueFilter();
        } else {
            return new Filter<Pod>() {
                public String toString() {
                    return "PodAnnotationFilter(" + annotationSelector + ")";
                }

                public boolean matches(Pod entity) {
                    return filterLabels(annotationSelector, entity.getAnnotations());
                }
            };
        }
    }

    /**
     * Creates a filter on a service using the given text string
     */
    public static Filter<Service> createServiceFilter(final String textFilter) {
        if (isNullOrBlank(textFilter)) {
            return Filters.<Service>trueFilter();
        } else {
            return new Filter<Service>() {
                public String toString() {
                    return "ServiceFilter(" + textFilter + ")";
                }

                public boolean matches(Service entity) {
                    return filterMatchesIdOrLabels(textFilter, getId(entity), entity.getLabels());
                }
            };
        }
    }

    /**
     * Creates a filter on a service using the given text string
     */
    public static Filter<Service> createServiceFilter(final Map<String, String> labelSelector) {
        if (labelSelector == null || labelSelector.isEmpty()) {
            return Filters.<Service>trueFilter();
        } else {
            return new Filter<Service>() {
                public String toString() {
                    return "ServiceFilter(" + labelSelector + ")";
                }

                public boolean matches(Service entity) {
                    return filterLabels(labelSelector, entity.getLabels());
                }
            };
        }
    }

    /**
     * Creates a filter on a replicationController using the given text string
     */
    public static Filter<ReplicationController> createReplicationControllerFilter(final String textFilter) {
        if (isNullOrBlank(textFilter)) {
            return Filters.<ReplicationController>trueFilter();
        } else {
            return new Filter<ReplicationController>() {
                public String toString() {
                    return "ReplicationControllerFilter(" + textFilter + ")";
                }

                public boolean matches(ReplicationController entity) {
                    return filterMatchesIdOrLabels(textFilter, getId(entity), entity.getLabels());
                }
            };
        }
    }

    /**
     * Creates a filter on a replicationController using the given text string
     */
    public static Filter<ReplicationController> createReplicationControllerFilter(final Map<String, String> labelSelector) {
        if (labelSelector == null || labelSelector.isEmpty()) {
            return Filters.<ReplicationController>trueFilter();
        } else {
            return new Filter<ReplicationController>() {
                public String toString() {
                    return "ReplicationControllerFilter(" + labelSelector + ")";
                }

                public boolean matches(ReplicationController entity) {
                    return filterLabels(labelSelector, entity.getLabels());
                }
            };
        }
    }

    /**
     * Returns true if the given textFilter matches either the id or the labels
     */
    public static boolean filterMatchesIdOrLabels(String textFilter, String id, Map<String, String> labels) {
        String text = toLabelsString(labels);
        boolean result = (text != null && text.contains(textFilter)) || (id != null && id.contains(textFilter));
        if (!result) {
            //labels can be in different order to selector
            Map<String, String> selectorMap = toLabelsMap(textFilter);

            if (!selectorMap.isEmpty()) {
                result = true;
                for (Map.Entry<String, String> entry : selectorMap.entrySet()) {
                    String value = labels.get(entry.getKey());
                    if (value == null || !value.matches(entry.getValue())) {
                        result = false;
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns true if the given filterLabels matches the actual labels
     */
    public static boolean filterLabels(Map<String, String> filterLabels, Map<String, String> labels) {
        if (labels == null) {
            return false;
        }
        Set<Map.Entry<String, String>> entries = filterLabels.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String expectedValue = entry.getValue();
            String actualValue = labels.get(key);
            if (!Objects.equal(expectedValue, actualValue)) {
                return false;
            }
        }
        return true;
    }


    /**
     * For positive non-zero values return the text of the number or return blank
     */
    public static String toPositiveNonZeroText(Integer port) {
        if (port != null) {
            int value = port;
            if (value > 0) {
                return "" + value;
            }
        }
        return "";
    }

    /**
     * Returns all the containers from the given pod
     */
    public static List<Container> getContainers(Pod pod) {
        if (pod != null) {
            PodState desiredState = pod.getDesiredState();
            return getContainers(desiredState);

        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns all the containers from the given Replication Controller
     */
    public static List<Container> getContainers(ReplicationController replicationController) {
        if (replicationController != null) {
            ReplicationControllerState desiredState = replicationController.getDesiredState();
            return getContainers(desiredState);
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns all the containers from the given Replication Controller's desiredState
     */
    public static List<Container> getContainers(ReplicationControllerState desiredState) {
        if (desiredState != null) {
            PodTemplate podTemplate = desiredState.getPodTemplate();
            return getContainers(podTemplate);
        }
        return Collections.EMPTY_LIST;
    }

    public static List<Container> getContainers(PodTemplate podTemplate) {
        if (podTemplate != null) {
            PodState desiredState = podTemplate.getDesiredState();
            return getContainers(desiredState);
        }
        return Collections.EMPTY_LIST;
    }

    public static List<Container> getContainers(ContainerManifest manifest) {
        if (manifest != null) {
            List<Container> containers = manifest.getContainers();
            if (containers != null) {
                return containers;
            }
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns all the containers from the given Replication Controller
     */
    public static List<Container> getCurrentContainers(ReplicationController replicationController) {
        if (replicationController != null) {
            // TODO
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns all the manifest containers from the given desiredState
     */
    public static List<Container> getContainers(PodState desiredState) {
        if (desiredState != null) {
            ContainerManifest manifest = desiredState.getManifest();
            if (manifest != null) {
                List<Container> containers = manifest.getContainers();
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
    public static Map<String, ContainerStatus> getCurrentContainers(Pod pod) {
        if (pod != null) {
            PodState currentState = pod.getCurrentState();
            return getCurrentContainers(currentState);

        }
        return Collections.EMPTY_MAP;
    }

    /**
     * Returns all the current containers from the given currentState
     */
    public static Map<String, ContainerStatus> getCurrentContainers(PodState currentState) {
        if (currentState != null) {
            Map<String, ContainerStatus> info = currentState.getInfo();
            if (info != null) {
                return info;
            }
        }
        return Collections.EMPTY_MAP;
    }

    /**
     * Returns the host of the pod
     */
    public static String getHost(Pod pod) {
        if (pod != null) {
            PodState currentState = pod.getCurrentState();
            if (currentState != null) {
                return currentState.getHost();
            }
        }
        return null;
    }

    /**
     * Returns the container port number for the given service
     */
    public static int getContainerPort(Service service) {
        int answer = -1;
        String id = getId(service);
        ServiceSpec spec = service.getSpec();
        if (spec != null) {
            io.fabric8.kubernetes.api.model.util.IntOrString containerPort = spec.getContainerPort();
            Objects.notNull(containerPort, "containerPort for service " + id);
            Integer intValue = containerPort.getIntVal();
            if (intValue != null) {
                answer = intValue;
            } else {
                String containerPortText = containerPort.getStrVal();
                if (Strings.isNullOrBlank(containerPortText)) {
                    throw new IllegalArgumentException("No containerPort for service " + id);
                }
                try {
                    answer = Integer.parseInt(containerPortText);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("Invalid containerPort expression " + containerPortText + " for service " + id + ". " + e, e);
                }
            }
        }
        if (answer <= 0) {
            throw new IllegalArgumentException("Invalid port number for service " + id + ". " + answer);
        }
        return answer;
    }

    /**
     * Combines the JSON objects into a config object
     */
    public static JsonNode combineJson(Object... objects) throws IOException {
        JsonNode config = findOrCreateConfig(objects);
        JsonNode items = config.get("items");
        ArrayNode itemArray;
        if (items instanceof ArrayNode) {
            itemArray = (ArrayNode) items;
        } else {
            itemArray = new ArrayNode(createNodeFactory());
            if (config instanceof ObjectNode) {
                ObjectNode objectNode = (ObjectNode) config;
                objectNode.set("items", itemArray);
            } else {
                throw new IllegalArgumentException("config " + config + " is not a ObjectNode");
            }
        }
        for (Object object : objects) {
            if (object != config) {
                addObjectsToItemArray(itemArray, object);
            }
        }
        return config;
    }

    protected static void addObjectsToItemArray(ArrayNode itemArray, Object object) throws IOException {
        JsonNode node = toJsonNode(object);
        JsonNode items = node.get("items");
        if (items != null && items.isArray()) {
            Iterator<JsonNode> iter = items.iterator();
            for (JsonNode item : items) {
                itemArray.add(item);
            }
        } else {
            itemArray.add(node);
        }
    }

    protected static JsonNodeFactory createNodeFactory() {
        return new JsonNodeFactory(false);
    }

    protected static JsonNode findOrCreateConfig(Object[] objects) {
        for (Object object : objects) {
            if (object instanceof JsonNode) {
                JsonNode jsonNode = (JsonNode) object;
                JsonNode items = jsonNode.get("items");
                if (items != null && items.isArray()) {
                    return jsonNode;
                }
            }
        }
        // lets create a new config
        JsonNodeFactory factory = createNodeFactory();
        ObjectNode config = factory.objectNode();
        config.set("apiVersion", factory.textNode("v1beta1"));
        config.set("kind", factory.textNode("Config"));
        config.set("items", factory.arrayNode());
        return config;
    }

    /**
     * Converts the DTO to a JsonNode
     */
    public static JsonNode toJsonNode(Object object) throws IOException {
        if (object instanceof JsonNode) {
            return (JsonNode) object;
        } else if (object == null) {
            return null;
        } else {
            String json = toJson(object);
            return objectMapper.reader().readTree(json);
        }
    }

    /**
     * Returns the URL to access the service; using the service portalIP and port
     */
    public static String getServiceURL(Service service) {
        if (service != null) {
            ServiceSpec spec = service.getSpec();
            if (spec != null) {
                String portalIP = spec.getPortalIP();
                if (portalIP != null) {
                    // TODO should we use the proxyPort?
                    Integer port = spec.getPort();
                    if (port != null && port > 0) {
                        portalIP += ":" + port;
                    }
                }
                // TODO use metadata on a service to decide if its HTTP or HTTPS?
                String protocol = "http://";
                return protocol + portalIP;
            }
        }
        return null;
    }

    /**
     * Returns the port for the given port number on the pod
     */
    public static Port findContainerPort(Pod pod, Integer portNumber) {
        List<Container> containers = KubernetesHelper.getContainers(pod);
        for (Container container : containers) {
            List<Port> ports = container.getPorts();
            for (Port port : ports) {
                if (Objects.equal(portNumber, port.getContainerPort())) {
                    return port;
                }
            }
        }
        return null;
    }

    /**
     * Returns the port for the given port name
     */
    public static Port findContainerPortByName(Pod pod, String name) {
        List<Container> containers = KubernetesHelper.getContainers(pod);
        for (Container container : containers) {
            List<Port> ports = container.getPorts();
            for (Port port : ports) {
                if (Objects.equal(name, port.getName())) {
                    return port;
                }
            }
        }
        return null;
    }


    /**
     * Returns the port for the given port number or name
     */
    public static Port findContainerPortByNumberOrName(Pod pod, String numberOrName) {
        Integer portNumber = toOptionalNumber(numberOrName);
        if (portNumber != null) {
            return findContainerPort(pod, portNumber);
        } else {
            return findContainerPortByName(pod, numberOrName);
        }
    }


    /**
     * Returns the number if it can be parsed or null
     */
    protected static Integer toOptionalNumber(String text) {
        if (Strings.isNotBlank(text)) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                // ignore parse errors
            }
        }
        return null;
    }

    public static PodState getPodState(ReplicationController replicationController) {
        if (replicationController != null) {
            return getPodState(replicationController.getDesiredState());
        }
        return null;
    }

    public static PodState getPodState(ReplicationControllerState desiredState) {
        PodTemplate podTemplate;
        PodState podTemplatePodState = null;
        if (desiredState != null) {
            podTemplate = desiredState.getPodTemplate();
            if (podTemplate != null) {
                podTemplatePodState = podTemplate.getDesiredState();
            }
        }
        return podTemplatePodState;
    }

    public static PodStatus getPodStatus(Pod pod) {
        String text = getPodStatusText(pod);
        if (Strings.isNotBlank(text)) {
            text = text.toLowerCase();
            if (text.startsWith("run")) {
                return PodStatus.OK;
            } else if (text.startsWith("wait")) {
                return PodStatus.WAIT;
            } else {
                return PodStatus.ERROR;
            }
        }
        return PodStatus.WAIT;
    }

    public static String getPodStatusText(Pod pod) {
        if (pod != null) {
            PodState currentState = pod.getCurrentState();
            if (currentState != null) {
                return currentState.getStatus();
            }
        }
        return null;
    }

    /**
     * Returns the pods for the given replication controller
     */
    public static List<Pod> getPodsForReplicationController(ReplicationController replicationController, Iterable<Pod> pods) {
        PodState podTemplatePodState = getPodState(replicationController);
        if (podTemplatePodState == null) {
            LOG.warn("Cannot instantiate replication controller: " + getId(replicationController) + " due to missing PodTemplate.PodState!");
        } else {
            ReplicationControllerState desiredState = replicationController.getDesiredState();
            if (desiredState != null) {
                Map<String, String> replicaSelector = desiredState.getReplicaSelector();
                Filter<Pod> podFilter = KubernetesHelper.createPodFilter(replicaSelector);
                return Filters.filter(pods, podFilter);
            }
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns the pods for the given service
     */
    public static List<Pod> getPodsForService(Service service, Iterable<Pod> pods) {
        Map<String, String> selector = getSelector(service);
        Filter<Pod> podFilter = KubernetesHelper.createPodFilter(selector);
        return Filters.filter(pods, podFilter);
    }
}
