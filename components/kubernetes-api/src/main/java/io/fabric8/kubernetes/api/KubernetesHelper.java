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
package io.fabric8.kubernetes.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.extensions.Templates;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateRunning;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.RootPaths;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.utils.Utils;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import io.fabric8.openshift.api.model.OAuthClient;
import io.fabric8.openshift.api.model.Parameter;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.api.model.RouteSpec;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.utils.Files;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Filters;
import io.fabric8.utils.KubernetesServices;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import io.fabric8.utils.ssl.TrustEverythingSSLTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLKeyException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.SSLSocketFactory;
import javax.tools.FileObject;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.fabric8.utils.Lists.notNullList;
import static io.fabric8.utils.Strings.isNullOrBlank;

/**
 * Kubernetes utility methods.
 */
public final class KubernetesHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesHelper.class);

    public static final String KUBERNETES_NAMESPACE_SYSTEM_PROPERTY = "kubernetes.namespace";
    public static final String KUBERNETES_NAMESPACE_ENV = "KUBERNETES_NAMESPACE";
    public static final String DEFAULT_NAMESPACE = "default";

    private static final transient Logger LOG = LoggerFactory.getLogger(KubernetesHelper.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final int INTORSTRING_KIND_INT = 0;
    public static final int INTORSTRING_KIND_STRING = 1;


    public static final String DEFAULT_DOCKER_HOST = "tcp://localhost:2375";
    protected static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

    public static final String defaultApiVersion = "v1";
    public static final String defaultOsApiVersion = "v1";

    private static final ConcurrentMap<URL, Boolean> IS_OPENSHIFT = new ConcurrentHashMap<>();
    private static final Config CONFIG = new ConfigBuilder().build();

    public static String defaultNamespace() {
        return CONFIG.getNamespace();
    }

    /**
     * Returns the ID of the given object
     */
    public static String getObjectId(Object object) {
        if (object instanceof HasMetadata) {
            return getName((HasMetadata) object);
        } else {
            return object != null ? object.toString() : null;
        }
    }

    public static ObjectMeta getOrCreateMetadata(HasMetadata entity) {
        ObjectMeta metadata = entity.getMetadata();
        if (metadata == null) {
            metadata = new ObjectMeta();
            entity.setMetadata(metadata);
        }
        return metadata;
    }

    /**
     * Returns the resource version for the entity or null if it does not have one
     */
    public static String getResourceVersion(HasMetadata entity) {
        if (entity != null) {
            ObjectMeta metadata = entity.getMetadata();
            if (metadata != null) {
                String resourceVersion = metadata.getResourceVersion();
                if (Strings.isNotBlank(resourceVersion)) {
                    return resourceVersion;
                }
            }
        }
        return null;
    }

    /**
     * Returns true if this entity has a valid non blank resourceVersion in its metadata
     */
    public static boolean hasResourceVersion(HasMetadata entity) {
        return getResourceVersion(entity) != null;
    }

    public static String getName(ObjectMeta entity) {
        if (entity != null) {
            return Strings.firstNonBlank(entity.getName(),
                    getAdditionalPropertyText(entity.getAdditionalProperties(), "id"),
                    entity.getUid());
        } else {
            return null;
        }
    }


    /**
     * Returns the kind of the entity
     */
    public static String getKind(HasMetadata entity) {
        if (entity != null) {
            // TODO use reflection to find the kind?
            if (entity instanceof KubernetesList) {
                return "List";
            } else {
                return entity.getClass().getSimpleName();
            }
        } else {
            return null;
        }
    }

    public static String getName(HasMetadata entity) {
        if (entity != null) {
            return getName(entity.getMetadata());
        } else {
            return null;
        }
    }

    public static void setName(HasMetadata entity, String name) {
        getOrCreateMetadata(entity).setName(name);
    }

    public static void setName(HasMetadata entity, String namespace, String name) {
        ObjectMeta metadata = getOrCreateMetadata(entity);
        metadata.setNamespace(namespace);
        metadata.setName(name);
    }

    public static void setNamespace(HasMetadata entity, String namespace) {
        getOrCreateMetadata(entity).setNamespace(namespace);
    }

    public static String getNamespace(ObjectMeta entity) {
        if (entity != null) {
            return entity.getNamespace();
        } else {
            return null;
        }
    }

    public static String getNamespace(HasMetadata entity) {
        if (entity != null) {
            return getNamespace(entity.getMetadata());
        } else {
            return null;
        }
    }

    public static Map<String, String> getOrCreateAnnotations(HasMetadata entity) {
        ObjectMeta metadata = getOrCreateMetadata(entity);
        Map<String, String> answer = metadata.getAnnotations();
        if (answer == null) {
            // use linked so the annotations can be in the FIFO order
            answer = new LinkedHashMap<>();
            metadata.setAnnotations(answer);
        }
        return answer;
    }

    public static Map<String, String> getOrCreateLabels(HasMetadata entity) {
        ObjectMeta metadata = getOrCreateMetadata(entity);
        Map<String, String> answer = metadata.getLabels();
        if (answer == null) {
            // use linked so the annotations can be in the FIFO order
            answer = new LinkedHashMap<>();
            metadata.setLabels(answer);
        }
        return answer;
    }

    /**
     * Returns the labels of the given metadata object or an empty map if the metadata or labels are null
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> getLabels(ObjectMeta metadata) {
        if (metadata != null) {
            Map<String, String> labels = metadata.getLabels();
            if (labels != null) {
                return labels;
            }
        }
        return Collections.EMPTY_MAP;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getLabels(HasMetadata entity) {
        if (entity != null) {
            return getLabels(entity.getMetadata());
        }
        return Collections.EMPTY_MAP;
    }

    public static ServiceSpec getOrCreateSpec(Service entity) {
        ServiceSpec spec = entity.getSpec();
        if (spec == null) {
            spec = new ServiceSpec();
            entity.setSpec(spec);
        }
        return spec;
    }

    public static String getPortalIP(Service entity) {
        String answer = null;
        if (entity != null) {
            ServiceSpec spec = getOrCreateSpec(entity);
            return spec.getClusterIP();
        }
        return answer;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getSelector(Service entity) {
        Map<String, String> answer = null;
        if (entity != null) {
            ServiceSpec spec = getOrCreateSpec(entity);
            answer = spec.getSelector();
        }
        return answer != null ? answer : Collections.EMPTY_MAP;
    }

    public static void setSelector(Service entity, Map<String, String> labels) {
        ServiceSpec spec = getOrCreateSpec(entity);
        spec.setSelector(labels);
    }

    public static Set<Integer> getPorts(Service entity) {
        Set<Integer> answer = new HashSet<>();
        if (entity != null) {
            ServiceSpec spec = getOrCreateSpec(entity);
            for (ServicePort port : spec.getPorts()) {
                answer.add(port.getPort());
            }
        }
        return answer;
    }

    protected static String getAdditionalPropertyText(Map<String, Object> additionalProperties, String name) {
        if (additionalProperties != null) {
            Object value = additionalProperties.get(name);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    protected static Map<String, Object> getMetadata(Map<String, Object> additionalProperties, boolean create) {
        Map<String, Object> answer = getAdditionalPropertyMap(additionalProperties, "metadata");
        if (answer == null) {
            answer = new LinkedHashMap<>();
            if (create) {
                additionalProperties.put("metadata", answer);
            }
        }
        return answer;
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> getAdditionalPropertyMap(Map<String, Object> additionalProperties, String name) {
        if (additionalProperties != null) {
            Object value = additionalProperties.get(name);
            if (value instanceof Map) {
                return (Map<String, Object>) value;
            }
        }
        return null;
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
        return OBJECT_MAPPER.writerFor(clazz).writeValueAsString(dto);
    }

    /**
     * Returns the given json data as a DTO such as
     * {@link Pod}, {@link ReplicationController} or
     * {@link io.fabric8.kubernetes.api.model.Service}
     * from the Kubernetes REST API
     */
    public static Object loadJson(File file) throws IOException {
        byte[] data = Files.readBytes(file);
        return loadJson(data);
    }

    /**
     * Returns the given json data as a DTO such as
     * {@link Pod}, {@link ReplicationController} or
     * {@link io.fabric8.kubernetes.api.model.Service}
     * from the Kubernetes REST API
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
     * from the Kubernetes REST API
     */
    public static Object loadJson(byte[] json) throws IOException {
        if (json != null && json.length > 0) {
            return OBJECT_MAPPER.readerFor(KubernetesResource.class).readValue(json);
        }
        return null;
    }

    /**
     * Loads the YAML file for the given DTO class
     */
    public static <T> T loadYaml(InputStream in, Class<T> clazz) throws IOException {
        byte[] data = Files.readBytes(in);
        return loadYaml(data, clazz);
    }

    /**
     * Loads the YAML file for the given DTO class
     */
    public static <T> T loadYaml(File file, Class<T> clazz) throws IOException {
        byte[] data = Files.readBytes(file);
        return loadYaml(data, clazz);
    }

    /**
     * Loads the YAML file for the given DTO class
     */
    public static <T> T loadYaml(byte[] data, Class<T> clazz) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(data, clazz);
    }

    public static void saveYaml(Object data, File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.writeValue(file, data);
    }

    public static void saveYaml(Object data, FileObject fileObject) throws IOException{
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (Writer writer = fileObject.openWriter()) {
            mapper.writeValue(writer, data);
        }
    }

    /**
     * Loads the Kubernetes JSON and converts it to a list of entities
     */
    @SuppressWarnings("unchecked")
    public static List<HasMetadata> toItemList(Object entity) throws IOException {
        if (entity instanceof List) {
            return (List<HasMetadata>) entity;
        } else if (entity instanceof HasMetadata[]) {
            HasMetadata[] array = (HasMetadata[]) entity;
            return Arrays.asList(array);
        } else if (entity instanceof KubernetesList) {
            KubernetesList config = (KubernetesList) entity;
            return config.getItems();
        } else if (entity instanceof Template) {
            Template objects = (Template) entity;
            return objects.getObjects();
        } else {
            List<HasMetadata> answer = new ArrayList<>();
            if (entity instanceof HasMetadata) {
                answer.add((HasMetadata) entity);
            }
            return answer;
        }
    }

    /**
     * Saves the json object to the given file
     */
    public static void saveJson(File json, Object object) throws IOException {
        OBJECT_MAPPER.writer().withDefaultPrettyPrinter().writeValue(json, object);
    }

    /**
     * Returns a map indexed by pod id of the pods
     */
    public static Map<String, Pod> toPodMap(PodList podSchema) {
        return toFilteredPodMap(podSchema, Filters.<Pod>trueFilter());
    }

    protected static Map<String, Pod> toFilteredPodMap(PodList podSchema, Filter<Pod> filter) {
        List<Pod> list = podSchema != null ? podSchema.getItems() : null;
        List<Pod> filteredList = Filters.filter(list, filter);
        return toPodMap(filteredList);
    }

    /**
     * Returns a map indexed by pod id of the pods
     */
    public static Map<String, Pod> toPodMap(List<Pod> pods) {
        List<Pod> list = notNullList(pods);
        Map<String, Pod> answer = new HashMap<>();
        for (Pod pod : list) {
            String id = getName(pod);
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
            String id = getName(service);
            if (Strings.isNotBlank(id)) {
                answer.put(id, service);
            }
        }
        return answer;
    }

    public static Map<String, Service> toFilteredServiceMap(ServiceList serviceList, Filter<Service> filter) {
        List<Service> list = serviceList != null ? serviceList.getItems() : null;
        List<Service> filteredList = Filters.filter(list, filter);
        return toServiceMap(filteredList);
    }

    /**
     * Returns a map indexed by replicationController id of the replicationControllers
     */
    public static Map<String, ReplicationController> toReplicationControllerMap(ReplicationControllerList replicationControllerSchema) {
        Filter<ReplicationController> filter = createReplicationControllerFilter((String) null);
        return toFilteredReplicationControllerMap(replicationControllerSchema, filter);
    }

    protected static Map<String, ReplicationController> toFilteredReplicationControllerMap(ReplicationControllerList replicationControllerSchema, Filter<ReplicationController> filter) {
        List<ReplicationController> list = replicationControllerSchema != null ? replicationControllerSchema.getItems() : null;
        List<ReplicationController> filteredList = Filters.filter(list, filter);
        return toReplicationControllerMap(filteredList);
    }

    /**
     * Returns a map indexed by replicationController id of the replicationControllers
     */
    public static Map<String, ReplicationController> toReplicationControllerMap(List<ReplicationController> replicationControllers) {
        List<ReplicationController> list = notNullList(replicationControllers);
        Map<String, ReplicationController> answer = new HashMap<>();
        for (ReplicationController replicationControllerSchema : list) {
            String id = getName(replicationControllerSchema);
            if (Strings.isNotBlank(id)) {
                answer.put(id, replicationControllerSchema);
            }
        }
        return answer;
    }

    public static Map<String, Pod> getPodMap(KubernetesClient kubernetes) {
        return getPodMap(kubernetes, null);
    }

    public static Map<String, Pod> getPodMap(KubernetesClient kubernetes, String namespace) {
        PodList pods = null;
        try {
            pods = kubernetes.pods().inNamespace(namespace).list();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404) {
                // ignore not found
            } else {
                throw e;
            }
        }
        return toPodMap(pods);
    }

    public static Map<String, Pod> getSelectedPodMap(KubernetesClient kubernetes, String selector) {
        return getSelectedPodMap(kubernetes, null, selector);
    }

    public static Map<String, Pod> getSelectedPodMap(KubernetesClient kubernetes, String namespace, String selector) {
        return getFilteredPodMap(kubernetes, namespace, createPodFilter(selector));
    }

    public static Map<String, Pod> getFilteredPodMap(KubernetesClient kubernetes, Filter<Pod> filter) {
        return getFilteredPodMap(kubernetes, null, filter);
    }

    public static Map<String, Pod> getFilteredPodMap(KubernetesClient kubernetes, String namespace, Filter<Pod> filter) {
        return toFilteredPodMap(kubernetes.pods().inNamespace(namespace).list(), filter);
    }

    public static Map<String, Service> getServiceMap(KubernetesClient kubernetes) {
        return getServiceMap(kubernetes, null);
    }

    public static Map<String, Service> getServiceMap(KubernetesClient kubernetes, String namespace) {
        return toServiceMap(kubernetes.services().inNamespace(namespace).list());
    }

    public static Map<String, ReplicationController> getReplicationControllerMap(KubernetesClient kubernetes) {
        return getReplicationControllerMap(kubernetes, null);
    }

    public static Map<String, ReplicationController> getReplicationControllerMap(KubernetesClient kubernetes, String namespace) {
        return toReplicationControllerMap(kubernetes.replicationControllers().inNamespace(namespace).list());
    }

    public static Map<String, ReplicationController> getSelectedReplicationControllerMap(KubernetesClient kubernetes, String selector) {
        return getSelectedReplicationControllerMap(kubernetes, null, selector);
    }

    public static Map<String, ReplicationController> getSelectedReplicationControllerMap(KubernetesClient kubernetes, String namespace, String selector) {
        return toReplicationControllerMap(kubernetes.replicationControllers().inNamespace(namespace).withLabels(toLabelsMap(selector)).list());
    }

    /**
     * Removes empty pods returned by Kubernetes
     */
    public static void removeEmptyPods(PodList podSchema) {
        List<Pod> list = notNullList(podSchema.getItems());

        List<Pod> removeItems = new ArrayList<Pod>();

        for (Pod pod : list) {
            if (Strings.isNullOrBlank(getName(pod))) {
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
                    return filterMatchesIdOrLabels(textFilter, getName(entity), entity.getMetadata().getLabels());
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
                    return filterLabels(labelSelector, entity.getMetadata().getLabels());
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
                    return filterLabels(annotationSelector, entity.getMetadata().getAnnotations());
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
                    return filterMatchesIdOrLabels(textFilter, getName(entity), entity.getMetadata().getLabels());
                }
            };
        }
    }

    /**
     * Creates a filter on a service if it matches the given namespace
     */
    public static Filter<Service> createNamespaceServiceFilter(final String namespace) {
        if (isNullOrBlank(namespace)) {
            return Filters.<Service>trueFilter();
        } else {
            return new Filter<Service>() {
                public String toString() {
                    return "NamespaceServiceFilter(" + namespace + ")";
                }

                public boolean matches(Service entity) {
                    return Objects.equal(namespace, getNamespace(entity.getMetadata()));
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
                    return filterLabels(labelSelector, entity.getMetadata().getLabels());
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
                    return filterMatchesIdOrLabels(textFilter, getName(entity), entity.getMetadata().getLabels());
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
                    return filterLabels(labelSelector, entity.getMetadata().getLabels());
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

            if (!selectorMap.isEmpty() && labels != null && !labels.isEmpty()) {
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
    @SuppressWarnings("unchecked")
    public static List<Container> getContainers(Pod pod) {
        if (pod != null) {
            PodSpec podSpec = pod.getSpec();
            return getContainers(podSpec);

        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns all the containers from the given Replication Controller
     */
    @SuppressWarnings("unchecked")
    public static List<Container> getContainers(ReplicationController replicationController) {
        if (replicationController != null) {
            ReplicationControllerSpec replicationControllerSpec = replicationController.getSpec();
            return getContainers(replicationControllerSpec);
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns all the containers from the given Replication Controller's replicationControllerSpec
     */
    @SuppressWarnings("unchecked")
    public static List<Container> getContainers(ReplicationControllerSpec replicationControllerSpec) {
        if (replicationControllerSpec != null) {
            PodTemplateSpec podTemplateSpec = replicationControllerSpec.getTemplate();
            return getContainers(podTemplateSpec);
        }
        return Collections.EMPTY_LIST;
    }

    @SuppressWarnings("unchecked")
    public static List<Container> getContainers(PodSpec podSpec) {
        if (podSpec != null) {
            return podSpec.getContainers();
        }
        return Collections.EMPTY_LIST;
    }

    @SuppressWarnings("unchecked")
    public static List<Container> getContainers(PodTemplateSpec podTemplateSpec) {
        if (podTemplateSpec != null) {
            return getContainers(podTemplateSpec.getSpec());
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns all the containers from the given Replication Controller
     */
    @SuppressWarnings("unchecked")
    public static List<Container> getCurrentContainers(ReplicationController replicationController) {
        if (replicationController != null) {
            // TODO
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns all the current containers from the given currentState
     */
    @SuppressWarnings("unchecked")
    public static Map<String, ContainerStatus> getCurrentContainers(Pod pod) {
        if (pod != null) {
            PodStatus currentStatus = pod.getStatus();
            return getCurrentContainers(currentStatus);

        }
        return Collections.EMPTY_MAP;
    }

    /**
     * Returns all the current containers from the given podStatus
     */
    @SuppressWarnings("unchecked")
    public static Map<String, ContainerStatus> getCurrentContainers(PodStatus podStatus) {
        if (podStatus != null) {
            List<ContainerStatus> containerStatuses = podStatus.getContainerStatuses();
            Map<String, ContainerStatus> info = new Hashtable<>(containerStatuses.size());
            for (ContainerStatus status : containerStatuses) {
                info.put(status.getContainerID(), status);
            }
            return info;
        }
        return Collections.EMPTY_MAP;
    }

    /**
     * Returns the host of the pod
     */
    public static String getHost(Pod pod) {
        if (pod != null) {
            PodStatus currentState = pod.getStatus();
            if (currentState != null) {
                return currentState.getHostIP();
            }
        }
        return null;
    }

    /**
     * Returns the container port number for the given service
     */
    @SuppressWarnings("unchecked")
    public static Set<Integer> getContainerPorts(Service service) {
        Set<Integer> answer = Collections.EMPTY_SET;
        String id = getName(service);
        ServiceSpec spec = service.getSpec();
        if (spec != null) {
            List<ServicePort> servicePorts = spec.getPorts();
            Objects.notNull(servicePorts, "servicePorts for service " + id);

            answer = new HashSet<>(servicePorts.size());
            String message = "service " + id;

            for (ServicePort port : servicePorts) {
                IntOrString intOrStringValue = port.getTargetPort();
                Integer intValue = intOrStringToInteger(intOrStringValue, message);
                if (intValue != null) {
                    answer.add(intValue);
                }
            }
        }
        return answer;
    }

    /**
     * Returns the IntOrString converted to an Integer value or throws an exception with the given message
     */
    public static Integer intOrStringToInteger(IntOrString intOrStringValue, String message) {
        Integer intValue = intOrStringValue.getIntVal();
        if (intValue == null) {
            String containerPortText = intOrStringValue.getStrVal();
            if (Strings.isNullOrBlank(containerPortText)) {
                throw new IllegalArgumentException("No port for " +
                        message);
            }
            try {
                intValue = Integer.parseInt(containerPortText);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Invalid servicePorts expression " + containerPortText + " for " +
                        message + ". " + e, e);
            }
        }
        return intValue;
    }

    /**
     * Returns the container port number for the given service
     */
    @SuppressWarnings("unchecked")
    public static Set<String> getContainerPortsStrings(Service service) {
        Set<String> answer = Collections.EMPTY_SET;
        String id = getName(service);
        ServiceSpec spec = service.getSpec();
        if (spec != null) {
            List<ServicePort> servicePorts = spec.getPorts();
            Objects.notNull(servicePorts, "servicePorts for service " + id);

            answer = new HashSet<>(servicePorts.size());

            for (ServicePort port : servicePorts) {
                IntOrString intOrStringValue = port.getTargetPort();
                Integer intValue = intOrStringValue.getIntVal();
                if (intValue != null) {
                    answer.add(intValue.toString());
                } else {
                    String containerPortText = intOrStringValue.getStrVal();
                    if (Strings.isNullOrBlank(containerPortText)) {
                        throw new IllegalArgumentException("No servicePorts for service " + id);
                    }
                    answer.add(containerPortText);
                }
            }
        }
        return answer;
    }

    /**
     * Combines the JSON objects into a config object
     */
    public static Object combineJson(Object... objects) throws IOException {
        KubernetesList list = findOrCreateList(objects);
        List<HasMetadata> items = list.getItems();
        if (items == null) {
            items = new ArrayList<>();
            list.setItems(items);
        }
        for (Object object : objects) {
            if (object != list) {
                addObjectsToItemArray(items, object);
            }
        }
        KubernetesList fullList = new KubernetesList();
        fullList.setItems(items);
        moveServicesToFrontOfArray(items);
        removeDuplicates(items);
        Object answer = Templates.combineTemplates(fullList);
        items = toItemList(answer);
        removeDuplicates(items);
        return answer;
    }

    /**
     * Lets move all Service resources before any other to avoid ordering issues creating things
     */
    public static void moveServicesToFrontOfArray(List<HasMetadata> list) {
        int size = list.size();
        int lastNonService = -1;
        for (int i = 0; i < size; i++) {
            HasMetadata item = list.get(i);
            if (item instanceof Service) {
                if (lastNonService >= 0) {
                    HasMetadata nonService = list.get(lastNonService);
                    list.set(i, nonService);
                    list.set(lastNonService, item);
                    lastNonService++;
                }
            } else if (lastNonService < 0) {
                lastNonService = i;
            }
        }
    }

    /**
     * Remove any duplicate resources using the kind and id
     */
    protected static void removeDuplicates(List<HasMetadata> itemArray) {
        int size = itemArray.size();
        int lastNonService = -1;
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < size; i++) {
            HasMetadata item = itemArray.get(i);
            if (item == null) {
                itemArray.remove(i);
                i--;
                size--;
            } else {
                String id = getObjectId(item);
                String kind = item.getClass().getSimpleName();
                if (Strings.isNotBlank(id)) {
                    String key = kind + ":" + id;
                    if (!keys.add(key)) {
                        // lets remove this one
                        itemArray.remove(i);
                        i--;
                        size--;
                    }
                }

            }
        }
    }

    @SuppressWarnings("unchecked")
    protected static void addObjectsToItemArray(List destinationList, Object object) throws IOException {
        if (object instanceof KubernetesList) {
            KubernetesList kubernetesList = (KubernetesList) object;
            List<HasMetadata> items = kubernetesList.getItems();
            if (items != null) {
                destinationList.addAll(items);
            }
        } else if (object instanceof Collection) {
            Collection collection = (Collection) object;
            destinationList.addAll(collection);
        } else {
            destinationList.add(object);
        }
    }

    protected static KubernetesList findOrCreateList(Object[] objects) {
        KubernetesList list = null;
        for (Object object : objects) {
            if (object instanceof KubernetesList) {
                list = (KubernetesList) object;
                break;
            }
        }
        if (list == null) {
            list = new KubernetesList();
        }
        return list;
    }

    /**
     * Returns the URL to access the service; using the service portalIP and port
     */
    public static String getServiceURL(Service service) {
        if (service != null) {
            ServiceSpec spec = service.getSpec();
            if (spec != null) {
                String portalIP = spec.getClusterIP();
                if (portalIP != null) {
                    Integer port = spec.getPorts().iterator().next().getPort();
                    if (port != null && port > 0) {
                        portalIP += ":" + port;
                    }
                    String protocol = "http://";
                    if (KubernetesHelper.isServiceSsl(spec.getClusterIP(), port, Utils.getSystemPropertyOrEnvVar(io.fabric8.kubernetes.client.Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, false))) {
                        protocol = "https://";
                    }
                    return protocol + portalIP;
                }
            }
        }
        return null;
    }

    /**
     * Returns the URL to access the service; using the environment variables, routes
     * or service portalIP address
     *
     * @throws IllegalArgumentException if the URL cannot be found for the serviceName and namespace
     */
    public static String getServiceURL(KubernetesClient client, String serviceName, String serviceNamespace, String serviceProtocol, boolean serviceExternal) {
        return getServiceURL(client, serviceName, serviceNamespace, serviceProtocol, null, serviceExternal);
    }

    /**
     * Returns the URL to access the service; using the environment variables, routes
     * or service portalIP address
     *
     * @throws IllegalArgumentException if the URL cannot be found for the serviceName and namespace
     */
    public static String getServiceURL(KubernetesClient client, String serviceName, String serviceNamespace, String serviceProtocol, String servicePortName, boolean serviceExternal) {
        Service srv = null;
        String serviceHost = KubernetesServices.serviceToHostOrBlank(serviceName);
        String servicePort = KubernetesServices.serviceToPortOrBlank(serviceName, servicePortName);
        String serviceProto = serviceProtocol != null ? serviceProtocol : KubernetesServices.serviceToProtocol(serviceName, servicePort);

        //Use specified or fallback namespace.
        String actualNamespace = Strings.isNotBlank(serviceNamespace) ? serviceNamespace : client.getNamespace();

        //1. Inside Kubernetes: Services as ENV vars
        if (!serviceExternal && Strings.isNotBlank(serviceHost) && Strings.isNotBlank(servicePort) && Strings.isNotBlank(serviceProtocol)) {
            return serviceProtocol + "://" + serviceHost + ":" + servicePort;
            //2. Anywhere: When namespace is passed System / Env var. Mostly needed for integration tests.
        } else if (Strings.isNotBlank(actualNamespace)) {
            try {
                srv = client.services().inNamespace(actualNamespace).withName(serviceName).get();
            } catch (Exception e) {
                LOGGER.warn("Could not lookup service:"+serviceName+" in namespace:"+actualNamespace+", due to: " + e.getMessage());
            }
        }

        if (srv == null) {
            throw new IllegalArgumentException("No kubernetes service could be found for name: " + serviceName + " in namespace: " + actualNamespace);
        }

        try {
            if (Strings.isNullOrBlank(servicePortName) && isOpenShift(client)) {
                OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
                Route route = openShiftClient.routes().inNamespace(actualNamespace).withName(serviceName).get();
                if (route != null) {
                    return (serviceProto + "://" + route.getSpec().getHost()).toLowerCase();
                }
            }
        } catch (KubernetesClientException e) {
            if (e.getCode() == 403) {
                LOGGER.warn("Could not lookup route:"+serviceName+" in namespace:"+actualNamespace+", due to: " + e.getMessage());
            } else {
                throw e;
            }
        }

        ServicePort port = findServicePortByName(srv, servicePortName);
        if (port == null) {
            throw new RuntimeException("Couldn't find port: " + servicePortName + " for service:" + serviceName);
        }
        if ("None".equals(srv.getSpec().getClusterIP())) {
            throw new IllegalStateException("Service: " + serviceName + " in namespace:" + serviceNamespace + "is head-less. Search for endpoints instead.");
        }
        return (serviceProto + "://" + srv.getSpec().getPortalIP() + ":" + port.getPort()).toLowerCase();
    }

    /**
     * Returns the URL to access the service; using the environment variables, routes
     * or service portalIP address
     *
     * @throws IllegalArgumentException if the URL cannot be found for the serviceName and namespace
     */
    public static String getServiceURLInCurrentNamespace(KubernetesClient client, String serviceName, String serviceProtocol, String servicePortName, boolean serviceExternal) {
        Service srv = null;
        String serviceHost = KubernetesServices.serviceToHostOrBlank(serviceName);
        String servicePort = KubernetesServices.serviceToPortOrBlank(serviceName, servicePortName);
        String serviceProto = serviceProtocol != null ? serviceProtocol : KubernetesServices.serviceToProtocol(serviceName, servicePort);

        //1. Inside Kubernetes: Services as ENV vars
        if (!serviceExternal && Strings.isNotBlank(serviceHost) && Strings.isNotBlank(servicePort) && Strings.isNotBlank(serviceProtocol)) {
            return serviceProtocol + "://" + serviceHost + ":" + servicePort;
            //2. Anywhere: When namespace is passed System / Env var. Mostly needed for integration tests.
        } else  {
            srv = client.services().withName(serviceName).get();
        }
        if (srv == null) {
            throw new IllegalArgumentException("No kubernetes service could be found for name: " + serviceName);
        }

        if (Strings.isNullOrBlank(servicePortName) && isOpenShift(client)) {
            OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
            RouteList routeList = openShiftClient.routes().list();
            for (Route route : routeList.getItems()) {
                if (route.getSpec().getTo().getName().equals(serviceName)) {
                    return (serviceProto + "://" + route.getSpec().getHost()).toLowerCase();
                }
            }
        }
        ServicePort port = findServicePortByName(srv, servicePortName);
        if (port == null) {
            throw new RuntimeException("Couldn't find port: " + servicePortName + " for service:" + serviceName);
        }
        return (serviceProto + "://" + srv.getSpec().getPortalIP() + ":" + port.getPort()).toLowerCase();
    }

    /**
     * Returns the port for the given port number on the pod
     */
    public static ContainerPort findContainerPort(Pod pod, Integer portNumber) {
        List<Container> containers = KubernetesHelper.getContainers(pod);
        for (Container container : containers) {
            List<ContainerPort> ports = container.getPorts();
            for (ContainerPort port : ports) {
                if (Objects.equal(portNumber, port.getContainerPort())) {
                    return port;
                }
            }
        }
        return null;
    }

    public static ServicePort findServicePortByName(Service service, String portName) {
        if (Strings.isNullOrBlank(portName)) {
            return service.getSpec().getPorts().iterator().next();
        }

        for (ServicePort servicePort : service.getSpec().getPorts()) {
            if (Objects.equal(servicePort.getName(), portName)) {
                return servicePort;
            }
        }
        return null;
    }

    /**
     * Returns the port for the given port name
     */
    public static ContainerPort findContainerPortByName(Pod pod, String name) {
        List<Container> containers = KubernetesHelper.getContainers(pod);
        for (Container container : containers) {
            List<ContainerPort> ports = container.getPorts();
            for (ContainerPort port : ports) {
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
    public static ContainerPort findContainerPortByNumberOrName(Pod pod, String numberOrName) {
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

    public static PodStatusType getPodStatus(Pod pod) {
        String text = getPodStatusText(pod);
        if (Strings.isNotBlank(text)) {
            text = text.toLowerCase();
            if (text.startsWith("run")) {
                return PodStatusType.OK;
            } else if (text.startsWith("wait")) {
                return PodStatusType.WAIT;
            } else {
                return PodStatusType.ERROR;
            }
        }
        return PodStatusType.WAIT;
    }

    /**
     * Returns true if the pod is running
     */
    public static boolean isPodRunning(Pod pod) {
        PodStatusType status = getPodStatus(pod);
        return Objects.equal(status, PodStatusType.OK);
    }

    /**
     * Returns true if the pod is running and ready
     */
    public static boolean isPodReady(Pod pod) {
        if (!isPodRunning(pod)) {
            return false;
        }

        PodStatus podStatus = pod.getStatus();
        if (podStatus == null) {
            return true;
        }

        List<PodCondition> conditions = podStatus.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        // Check "ready" condition
        for (PodCondition condition : conditions) {
            if ("ready".equalsIgnoreCase(condition.getType())) {
                return Boolean.parseBoolean(condition.getStatus());
            }
        }

        return true;
    }

    public static String getPodStatusText(Pod pod) {
        if (pod != null) {
            PodStatus podStatus = pod.getStatus();
            if (podStatus != null) {
                return podStatus.getPhase();
            }
        }
        return null;
    }

    /**
     * Returns the environment variable value for the first container which has a value for it in th epod
     */
    public static String getPodEnvVar(Pod pod, String envVarName) {
        if (pod != null) {
            PodSpec spec = pod.getSpec();
            if (spec != null) {
                List<Container> containers = spec.getContainers();
                if (containers != null) {
                    for (Container container : containers) {
                        String answer = getContainerEnvVar(container, envVarName);
                        if (Strings.isNotBlank(answer)) {
                            return answer;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the environment variable value for the given container and name
     */
    public static String getContainerEnvVar(Container container, String envVarName) {
        if (container != null) {
            List<EnvVar> env = container.getEnv();
            if (env != null) {
                for (EnvVar envVar : env) {
                    if (Objects.equal(envVarName, envVar.getName())) {
                        return envVar.getValue();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the pods for the given replication controller
     */
    @SuppressWarnings("unchecked")
    public static List<Pod> getPodsForReplicationController(ReplicationController replicationController, Iterable<Pod> pods) {
        ReplicationControllerSpec replicationControllerSpec = replicationController.getSpec();
        if (replicationControllerSpec == null) {
            LOG.warn("Cannot instantiate replication controller: " + getName(replicationController) + " due to missing ReplicationController.Spec!");
        } else {
            Map<String, String> replicaSelector = replicationControllerSpec.getSelector();
            Filter<Pod> podFilter = KubernetesHelper.createPodFilter(replicaSelector);
            return Filters.filter(pods, podFilter);
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

    /**
     * Looks up the service endpoints in DNS.
     * <p/>
     * Endpoints are registered as SRV records in DNS so this method returns
     * endpoints in the format "host:port". This is a list as SRV records are ordered
     * by priority & weight before being returned to the client.
     * <p/>
     * See https://github.com/GoogleCloudPlatform/kubernetes/blob/master/cluster/addons/dns/README.md
     */
    public static List<String> lookupServiceEndpointsInDns(String serviceName) throws IllegalArgumentException, UnknownHostException {
        try {
            Lookup l = new Lookup(serviceName, Type.SRV);
            Record[] records = l.run();
            if (l.getResult() == Lookup.SUCCESSFUL) {

                SRVRecord[] srvRecords = Arrays.copyOf(records, records.length, SRVRecord[].class);
                Arrays.sort(srvRecords, new Comparator<SRVRecord>() {
                    @Override
                    public int compare(SRVRecord a, SRVRecord b) {
                        int ret = Integer.compare(b.getPriority(), a.getPriority());
                        if (ret == 0) {
                            ret = Integer.compare(b.getWeight(), a.getWeight());
                        }
                        return ret;
                    }
                });

                List<String> endpointAddresses = new ArrayList<>(srvRecords.length);
                for (SRVRecord srvRecord : srvRecords) {
                    endpointAddresses.add(srvRecord.getTarget().toString(true).concat(":").concat(String.valueOf(srvRecord.getPort())));
                }
                return endpointAddresses;
            } else {
                LOG.warn("Lookup {} result: {}", serviceName, l.getErrorString());
            }
        } catch (TextParseException e) {
            LOG.error("Unparseable service name: {}", serviceName, e);
        } catch (ClassCastException e) {
            LOG.error("Invalid response from DNS server - should have been A records", e);
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Looks up the service in DNS.
     * If this is a headless service, this call returns the endpoint IPs from DNS.
     * If this is a non-headless service, this call returns the service IP only.
     * <p/>
     * See https://github.com/GoogleCloudPlatform/kubernetes/blob/master/docs/services.md#headless-services
     */
    public static Set<String> lookupServiceInDns(String serviceName) throws IllegalArgumentException, UnknownHostException {
        try {
            Lookup l = new Lookup(serviceName);
            Record[] records = l.run();
            if (l.getResult() == Lookup.SUCCESSFUL) {
                Set<String> endpointAddresses = new HashSet<>(records.length);
                for (int i = 0; i < records.length; i++) {
                    ARecord aRecord = (ARecord) records[i];
                    endpointAddresses.add(aRecord.getAddress().getHostAddress());
                }
                return endpointAddresses;
            } else {
                LOG.warn("Lookup {} result: {}", serviceName, l.getErrorString());
            }
        } catch (TextParseException e) {
            LOG.error("Unparseable service name: {}", serviceName, e);
        } catch (ClassCastException e) {
            LOG.error("Invalid response from DNS server - should have been A records", e);
        }
        return Collections.EMPTY_SET;
    }

    public static boolean isServiceSsl(String host, int port, boolean trustAllCerts) {
        try {
            LOG.info("Checking if a service is SSL on " + host + ":" + port);
            SSLSocketFactory sslsocketfactory;
            if (trustAllCerts) {
                sslsocketfactory = TrustEverythingSSLTrustManager.getTrustingSSLSocketFactory();
            } else {
                sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            }

            Socket socket = sslsocketfactory.createSocket();

            // Connect, with an explicit timeout value
            socket.connect(new InetSocketAddress(host, port), 1 * 1000);
            try {

                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                // Write a test byte to get a reaction :)
                out.write(1);

                while (in.available() > 0) {
                    System.out.print(in.read());
                }
                return true;
            } finally {
                LOG.info("Checked if a service is SSL on " + host + ":" + port);
                socket.close();
            }
        } catch (SSLHandshakeException e) {
            LOG.error("SSL handshake failed - this probably means that you need to trust the kubernetes root SSL certificate or set the environment variable " + Utils.convertSystemPropertyNameToEnvVar(io.fabric8.kubernetes.client.Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY), e);
        } catch (SSLProtocolException e) {
            LOG.error("SSL protocol error", e);
        } catch (SSLKeyException e) {
            LOG.error("Bad SSL key", e);
        } catch (SSLPeerUnverifiedException e) {
            LOG.error("Could not verify server", e);
        } catch (SSLException e) {
            LOG.debug("Address does not appear to be SSL-enabled - falling back to http", e);
        } catch (IOException e) {
            LOG.debug("Failed to validate service", e);
        }
        return false;
    }

    /**
     * Validates that the given value is valid according to the kubernetes ID parsing rules, throwing an exception if not.
     */
    public static String validateKubernetesId(String currentValue, String description) throws IllegalArgumentException {
        if (isNullOrBlank(currentValue)) {
            throw new IllegalArgumentException("No " + description + " is specified!");
        }
        int size = currentValue.length();
        for (int i = 0; i < size; i++) {
            char ch = currentValue.charAt(i);
            if (Character.isUpperCase(ch)) {
                throw new IllegalArgumentException("Invalid upper case letter '" + ch + "' at index " + i + " for " + description + " value: " + currentValue);
            }
        }
        return currentValue;
    }

    public static Date parseDate(String text) {
        try {
            return dateTimeFormat.parse(text);
        } catch (ParseException e) {
            LOG.warn("Failed to parse date: " + text + ". Reason: " + e);
            return null;
        }
    }

    /**
     * Returns a short summary text message for the given kubernetes resource
     */
    public static String summaryText(Object object) {
        if (object instanceof Route) {
            return summaryText((Route) object);
        } else if (object instanceof Service) {
            return summaryText((Service) object);
        } else if (object instanceof ReplicationController) {
            return summaryText((ReplicationController) object);
        } else if (object instanceof Pod) {
            return summaryText((Pod) object);
        } else if (object instanceof Template) {
            return summaryText((Template) object);
        } else if (object instanceof DeploymentConfig) {
            return summaryText((DeploymentConfig) object);
        } else if (object instanceof OAuthClient) {
            return summaryText((OAuthClient) object);
        } else if (object instanceof String) {
            return object.toString();
        }
        return "";
    }

    /**
     * Returns a short summary text message for the given kubernetes resource
     */
    public static String summaryText(Route entity) {
        RouteSpec spec = entity.getSpec();
        if (spec == null) {
            return "No spec!";
        }
        return "host: " + spec.getHost();
    }

    /**
     * Returns a short summary text message for the given kubernetes resource
     */
    public static String summaryText(ContainerState entity) {
        ContainerStateRunning running = entity.getRunning();
        if (running != null) {
            return "Running";
        }
        ContainerStateWaiting waiting = entity.getWaiting();
        if (waiting != null) {
            return "Waiting";
        }
        ContainerStateTerminated termination = entity.getTerminated();
        if (termination != null) {
            return "Terminated";
        }
        return "Unknown";
    }

    /**
     * Returns a short summary text message for the given kubernetes resource
     */
    public static String summaryText(Template entity) {
        StringBuilder buffer = new StringBuilder();
        List<Parameter> parameters = entity.getParameters();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                String name = parameter.getName();
                appendText(buffer, name);
            }
        }
        return "parameters: " + buffer;
    }

    /**
     * Returns a short summary text message for the given kubernetes resource
     */
    public static String summaryText(OAuthClient entity) {
        return "redirectURIs: " + entity.getRedirectURIs();
    }

    /**
     * Returns a short summary text message for the given kubernetes resource
     */
    public static String summaryText(Service entity) {
        StringBuilder portText = new StringBuilder();
        ServiceSpec spec = entity.getSpec();
        if (spec == null) {
            return "No spec";
        } else {
            List<ServicePort> ports = spec.getPorts();
            if (ports != null) {
                for (ServicePort port : ports) {
                    Integer number = port.getPort();
                    if (number != null) {
                        if (portText.length() > 0) {
                            portText.append(", ");
                        }
                        portText.append("").append(number);
                    }
                }

            }
            return "selector: " + spec.getSelector() + " ports: " + portText;
        }
    }

    /**
     * Returns a short summary text message for the given kubernetes resource
     */
    public static String summaryText(ReplicationController entity) {
        StringBuilder buffer = new StringBuilder();
        ReplicationControllerSpec spec = entity.getSpec();
        if (spec != null) {
            buffer.append("replicas: ").append(spec.getReplicas());
            PodTemplateSpec podTemplateSpec = spec.getTemplate();
            if (podTemplateSpec != null) {
                appendSummaryText(buffer, podTemplateSpec);
            }
        }
        return buffer.toString();
    }

    /**
     * Returns a short summary text message for the given kubernetes resource
     */
    public static String summaryText(DeploymentConfig entity) {
        StringBuilder buffer = new StringBuilder();
        DeploymentConfigSpec spec = entity.getSpec();
        if (spec != null) {
            buffer.append("replicas: " + spec.getReplicas());
            PodTemplateSpec podTemplateSpec = spec.getTemplate();
            if (podTemplateSpec != null) {
                appendSummaryText(buffer, podTemplateSpec);
            }
        }
        return buffer.toString();
    }

    /**
     * Returns a short summary text message for the given kubernetes resource
     */
    public static String summaryText(Pod entity) {
        StringBuilder buffer = new StringBuilder();
        PodSpec podSpec = entity.getSpec();
        appendSummaryText(buffer, podSpec);
        return buffer.toString();
    }

    protected static void appendSummaryText(StringBuilder buffer, PodTemplateSpec podTemplateSpec) {
        if (podTemplateSpec != null) {
            appendSummaryText(buffer, podTemplateSpec.getSpec());
        }
    }

    protected static void appendSummaryText(StringBuilder buffer, PodSpec podSpec) {
        if (podSpec != null) {
            List<Container> containers = podSpec.getContainers();
            if (containers != null) {
                for (Container container : containers) {
                    String image = container.getImage();
                    appendText(buffer, "image: " + image);
                }
            }
        }
    }

    protected static void appendText(StringBuilder buffer, String text) {
        if (buffer.length() > 0) {
            buffer.append(", ");
        }
        buffer.append(text);
    }

    /**
     * Creates an IntOrString from the given string which could be a number or a name
     */
    public static IntOrString createIntOrString(int intVal) {
        IntOrString answer = new IntOrString();
        answer.setIntVal(intVal);
        answer.setKind(INTORSTRING_KIND_INT);
        return answer;
    }

    /**
     * Creates an IntOrString from the given string which could be a number or a name
     */
    public static IntOrString createIntOrString(String nameOrNumber) {
        if (isNullOrBlank(nameOrNumber)) {
            return null;
        } else {
            IntOrString answer = new IntOrString();
            Integer intVal = null;
            try {
                intVal = Integer.parseInt(nameOrNumber);
            } catch (Exception e) {
                // ignore invalid number
            }
            if (intVal != null) {
                answer.setIntVal(intVal);
                answer.setKind(INTORSTRING_KIND_INT);
            } else {
                answer.setStrVal(nameOrNumber);
                answer.setKind(INTORSTRING_KIND_STRING);
            }
            return answer;
        }
    }

    public static String getStatusText(PodStatus podStatus) {
        String status;
        List<String> statusList = new ArrayList<>();
        List<ContainerStatus> containerStatuses = podStatus.getContainerStatuses();
        for (ContainerStatus containerStatus : containerStatuses) {
            ContainerState state = containerStatus.getState();
            String statusText = summaryText(state);
            if (statusText != null) {
                statusList.add(statusText);
            }
        }
        if (statusList.size() == 1) {
            status = statusList.get(0);
        } else {
            status = statusList.toString();
        }
        return status;
    }

    public static Secret validateSecretExists(KubernetesClient kubernetes, String namespace, String secretName) {
        Secret secret = null;
        try {
            secret = kubernetes.secrets().inNamespace(namespace).withName(secretName).get();
        } catch (KubernetesClientException e) {
            if (e.getCode() == 404 || e.getCode() == 403) {
                // does not exist or namespace does not exists
            } else {
                throw e;
            }
        }
        if (secret == null) {
            throw new IllegalArgumentException("No secret named: " + secretName +
                    " for namespace " + namespace + " is available on Kubernetes" +
                    ". For how to create secrets see: http://fabric8.io/guide/fabric8OnOpenShift.html#requirements ");
        } else {
            return secret;
        }
    }

    /**
     * Converts the DTO loaded from JSON to a {@link KubernetesList} assuming its not a {@link Template}
     */
    public static KubernetesList asKubernetesList(Object dto) throws IOException {
        if (dto instanceof KubernetesList) {
            return (KubernetesList) dto;
        } else {
            KubernetesList answer = new KubernetesList();
            List<HasMetadata> items = toItemList(dto);
            answer.setItems(items);
            return answer;
        }
    }

    /**
     * Returns true if this object is a pure kubernetes DTO
     */
    public static boolean isPureKubernetes(HasMetadata item) {
        if (item != null) {
            String name = item.getClass().getName();
            return name.startsWith("io.fabric8.kubernetes");
        }
        return false;
    }


    public static boolean isOpenShift(KubernetesClient client) {
        URL masterUrl = client.getMasterUrl();
        if (IS_OPENSHIFT.containsKey(masterUrl)) {
            return IS_OPENSHIFT.get(masterUrl);
        } else {
            RootPaths rootPaths = client.rootPaths();
            if (rootPaths != null) {
                List<String> paths = rootPaths.getPaths();
                if (paths != null) {
                    for (String path : paths) {
                        if (java.util.Objects.equals("/oapi", path) || java.util.Objects.equals("oapi", path)) {
                            IS_OPENSHIFT.putIfAbsent(masterUrl, true);
                            return true;
                        }
                    }
                }
            }
        }
        IS_OPENSHIFT.putIfAbsent(masterUrl, false);
        return false;
    }
}
