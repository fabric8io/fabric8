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
package io.fabric8.kubernetes.jolokia;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import org.jolokia.client.J4pClient;
import org.jolokia.client.J4pClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.fabric8.kubernetes.api.KubernetesHelper.getDockerIp;
import static io.fabric8.utils.Objects.assertNotNull;

/**
 * Provides simple access to jolokia clients for a cluster
 */
public class JolokiaClients {
    private static final transient Logger LOG = LoggerFactory.getLogger(JolokiaClients.class);

    private final KubernetesClient kubernetes;
    private String user = Systems.getEnvVarOrSystemProperty("JOLOKIA_USER", "JOLOKIA_USER", "admin");
    private String password = Systems.getEnvVarOrSystemProperty("JOLOKIA_PASSWORD", "JOLOKIA_PASSWORD", "admin");;
    private Filter<Pod> podFilter = null;

    public JolokiaClients() {
        this(new DefaultKubernetesClient());
    }

    public JolokiaClients(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;
    }

    public KubernetesClient getKubernetes() {
        return kubernetes;
    }

    /**
     * Returns a client for the first working pod for the given replication controller or throws an assertion error if one could not be found
     */
    public J4pClient assertClientForReplicationController(String replicationControllerId, String namespace) {
        J4pClient client = clientForReplicationController(replicationControllerId, namespace);
        assertNotNull(client, "No client for replicationController: " + replicationControllerId);
        return client;
    }


    /**
     * Returns a client for the first working pod for the given service  or throws an assertion error if one could not be found
     */
    public J4pClient assertClientForService(String serviceId, String namespace) {
        J4pClient client = clientForService(serviceId, namespace);
        assertNotNull(client, "No client for service: " + serviceId);
        return client;
    }

    /**
     * Returns a client for the first working pod for the given replication controller
     */
    public J4pClient clientForReplicationController(ReplicationController replicationController) {
        List<Pod> pods = KubernetesHelper.getPodsForReplicationController(replicationController, kubernetes.pods().inNamespace(replicationController.getMetadata().getNamespace()).list().getItems());
        return clientForPod(pods);
    }


    /**
     * Returns a client for the first working pod for the given replication controller
     */
    public J4pClient clientForReplicationController(String replicationControllerId, String namespace) {
        return clientForReplicationController(kubernetes.replicationControllers().inNamespace(namespace).withName(replicationControllerId).get());
    }


    /**
     * Returns all the clients for the first working pod for the given replication controller
     */
    public List<J4pClient> clientsForReplicationController(ReplicationController replicationController) {
        List<Pod> pods = KubernetesHelper.getPodsForReplicationController(replicationController, kubernetes.pods().inNamespace(replicationController.getMetadata().getNamespace()).list().getItems());
        return clientsForPod(pods);
    }

    /**
     * Returns all the clients for the first working pod for the given replication controller
     */
    public List<J4pClient> clientsForReplicationController(String replicationControllerId, String namespace) {
        List<Pod> pods = KubernetesHelper.getPodsForReplicationController(kubernetes.replicationControllers().inNamespace(namespace).withName(replicationControllerId).get(),
                kubernetes.pods().inNamespace(namespace).list().getItems());
        return clientsForPod(pods);
    }



    /**
     * Returns a client for the first working pod for the given service
     */
    public J4pClient clientForService(String serviceId, String namespace) {
        List<Pod> pods = KubernetesHelper.getPodsForService(kubernetes.services().inNamespace(namespace).withName(serviceId).get(),
                kubernetes.pods().inNamespace(namespace).list().getItems());
        return clientForPod(pods);
    }

    /**
     * Returns a client for the first working pod for the given service
     */
    public J4pClient clientForService(Service service) {
        List<Pod> pods = KubernetesHelper.getPodsForService(service, kubernetes.pods().inNamespace(service.getMetadata().getNamespace()).list().getItems());
        return clientForPod(pods);
    }

    /**
     * Returns all the clients for the first working pod for the given service
     */
    public List<J4pClient> clientsForService(String serviceId, String namespace) {
        List<Pod> pods = KubernetesHelper.getPodsForService(kubernetes.services().inNamespace(namespace).withName(serviceId).get(),
                kubernetes.pods().inNamespace(namespace).list().getItems());
        return clientsForPod(pods);
    }

    /**
     * Returns all the clients the first working pod for the given service
     */
    public List<J4pClient> clientsForService(Service service) {
        List<Pod> pods = KubernetesHelper.getPodsForService(service, kubernetes.pods().inNamespace(service.getMetadata().getNamespace()).list().getItems());
        return clientsForPod(pods);
    }

    /**
     * Returns a client for the first working pod in the collection
     */
    public J4pClient clientForPod(Iterable<Pod> pods) {
        for (Pod pod : pods) {
            if (KubernetesHelper.isPodRunning(pod) && filterPod(pod)) {
                J4pClient client = clientForPod(pod);
                if (client != null) {
                    return client;
                }
            }
        }
        return null;
    }

    /**
     * Returns the clients for the running pods in the collection
     */
    public List<J4pClient> clientsForPod(Iterable<Pod> pods) {
        List<J4pClient> answer = new ArrayList<>();
        for (Pod pod : pods) {
            if (KubernetesHelper.isPodRunning(pod) && filterPod(pod)) {
                J4pClient client = clientForPod(pod);
                if (client != null) {
                    answer.add(client);
                }
            }
        }
        return answer;
    }
    /**
     * Strategy method to filter pods before creating clients for them.
     */
    protected boolean filterPod(Pod pod) {
        if (podFilter != null) {
            return podFilter.matches(pod);
        } else {
            return true;
        }
    }


    /**
     * Returns the Jolokia client for the first container in the pod which exposes the jolokia port
     */
    public J4pClient clientForPod(Pod pod) {
        String host = KubernetesHelper.getHost(pod);
        List<Container> containers = KubernetesHelper.getContainers(pod);
        for (Container container : containers) {
            J4pClient jolokia = clientForContainer(host, container, pod);
            if (jolokia != null) {
                return jolokia;
            }
        }
        return null;
    }

    /**
     * Returns the jolokia client for the given container
     */
    public J4pClient clientForContainer(String host, Container container, Pod pod) {
        if (container != null) {
            List<ContainerPort> ports = container.getPorts();
            for (ContainerPort port : ports) {
                Integer containerPort = port.getContainerPort();
                if (containerPort != null) {
                    String name = port.getName();
                    if (containerPort == 8778 || (Objects.equals("jolokia", name) && containerPort.intValue() > 0)) {
                        PodStatus currentState = pod.getStatus();
                        String podIP = currentState.getPodIP();
                        if (Strings.isNotBlank(podIP)) {
                            return createJolokiaClientFromHostAndPort(container, podIP, containerPort);
                        }
                        Integer hostPort = port.getHostPort();
                        if (hostPort != null && hasDocker(pod)) {
                            // if Kubernetes is running locally on a platform which doesn't support docker natively
                            // then docker containers will be on a different IP so lets check for localhost and
                            // switch to the docker IP if its available
                            if (host.equals("localhost") || host.equals("127.0.0.1")) {
                                String dockerIp = getDockerIp();
                                if (Strings.isNotBlank(dockerIp)) {
                                    host = dockerIp;
                                }
                            }
                        }
                        if (Strings.isNotBlank(host)) {
                            return createJolokiaClientFromHostAndPort(container, host, hostPort);
                        }
                    }
                }
            }
        }
        return null;
    }

    protected J4pClient createJolokiaClientFromHostAndPort(Container container, String host, Integer hostPort) {
        String jolokiaUrl = "http://" + host + ":" + hostPort + "/jolokia/";
        return createJolokiaClient(container, jolokiaUrl);
    }

    /**
     * Returns true if we detect we are running inside docker
     */
    protected boolean hasDocker(Pod pod) {
        PodStatus currentState = pod.getStatus();
        if (currentState != null) {
            List<ContainerStatus> containerStatuses = currentState.getContainerStatuses();
/*
            ContainerManifest manifest = currentState.getManifest();
            if (manifest != null) {
                List<Container> containers = manifest.getContainers();
                for (Container container : containers) {
                    Number memory = container.getMemory();
                    if (memory != null && memory.longValue() > 0) {
                        return true;
                    }
                }
            }
*/
/*
            Map<String, ContainerStatus> info = currentState.getInfo();
            if (info != null) {
                Collection<ContainerStatus> containers = info.values();
                for (ContainerStatus container : containers) {
                    DetailInfo detailInfo = container.get();
                    if (detailInfo != null) {
                        Map<String, Object> additionalProperties = detailInfo.getAdditionalProperties();
                        if (additionalProperties != null) {
                            if (additionalProperties.containsKey("HostConfig")) {
                                return true;
                            }
                        }
                    }
                }
            }
*/
        }
        return false;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Filter<Pod> getPodFilter() {
        return podFilter;
    }

    public void setPodFilter(Filter<Pod> podFilter) {
        this.podFilter = podFilter;
    }

    protected J4pClient createJolokiaClient(Container container, String jolokiaUrl) {
        String name = container.getName();
        LOG.debug("Creating jolokia client for : " + name + " at URL: " + jolokiaUrl);
        J4pClientBuilder builder = J4pClient.url(jolokiaUrl);
        if (Strings.isNotBlank(user)) {
            builder = builder.user(user);
        }
        if (Strings.isNotBlank(password)) {
            builder = builder.password(password);
        }
        return builder.build();
    }
}
