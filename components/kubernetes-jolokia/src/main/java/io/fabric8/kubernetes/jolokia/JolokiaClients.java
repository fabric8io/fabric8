/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
import io.fabric8.kubernetes.client.internal.SSLUtils;
import io.fabric8.kubernetes.client.utils.URLUtils;
import io.fabric8.utils.Filter;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.jolokia.client.BasicAuthenticator;
import org.jolokia.client.J4pClient;
import org.jolokia.client.J4pClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.SSLContext;

import static io.fabric8.kubernetes.api.KubernetesHelper.getDockerIp;
import static io.fabric8.utils.Objects.assertNotNull;

/**
 * Provides simple access to jolokia clients for a cluster
 */
public class JolokiaClients {
    private static final transient Logger LOG = LoggerFactory.getLogger(JolokiaClients.class);

    private final KubernetesClient kubernetes;

    private String user = Systems.getEnvVarOrSystemProperty("JOLOKIA_USER", "JOLOKIA_USER", "admin");

    private String password = Systems.getEnvVarOrSystemProperty("JOLOKIA_PASSWORD", "JOLOKIA_PASSWORD", "admin");

    /**
     * The protocol used by the Jolokia server (http or https).
     * It is detected automatically when left empty.
     */
    private String protocol = Systems.getEnvVarOrSystemProperty("JOLOKIA_PROTOCOL");

    private Filter<Pod> podFilter = null;

    private boolean useKubeProxy = true;

    /**
     * The authentication mode.
     * Autodetected when left empty;
     */
    private AuthenticationMode authenticationMode;

    public JolokiaClients() {
        this(new DefaultKubernetesClient());
    }

    public JolokiaClients(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;

        if (Systems.hasEnvVarOrSystemProperty("JOLOKIA_AUTHENTICATION_MODE")) {
            authenticationMode = AuthenticationMode.valueOf(Systems.getEnvVarOrSystemProperty("JOLOKIA_AUTHENTICATION_MODE"));
        }
    }

    public KubernetesClient getKubernetes() {
        return kubernetes;
    }

    /**
     * Returns a client for the first working pod for the given replication controller or throws an assertion error if one could not be found
     */
    public J4pClient assertClientForReplicationController(String replicationControllerName) {
        J4pClient client = clientForReplicationController(replicationControllerName);
        assertNotNull(client, "No client for replicationController: " + replicationControllerName);
        return client;
    }


    /**
     * Returns a client for the first working pod for the given replication controller or throws an assertion error if one could not be found
     */
    public J4pClient assertClientForReplicationController(String replicationControllerName, String namespace) {
        J4pClient client = clientForReplicationController(replicationControllerName, namespace);
        assertNotNull(client, "No client for replicationController: " + replicationControllerName);
        return client;
    }


    /**
     * Returns a client for the first working pod for the given service  or throws an assertion error if one could not be found
     */
    public J4pClient assertClientForService(String serviceName) {
        J4pClient client = clientForService(serviceName);
        assertNotNull(client, "No client for service: " + serviceName);
        return client;
    }

    /**
     * Returns a client for the first working pod for the given service  or throws an assertion error if one could not be found
     */
    public J4pClient assertClientForService(String serviceName, String namespace) {
        J4pClient client = clientForService(serviceName, namespace);
        assertNotNull(client, "No client for service: " + serviceName);
        return client;
    }

    /**
     * Returns a client for the first working pod for the given replication controller
     */
    public J4pClient clientForReplicationController(ReplicationController replicationController) {
        Objects.requireNonNull(replicationController, "ReplicationController");
        PodList podList = kubernetes.pods().inNamespace(replicationController.getMetadata().getNamespace()).list();
        List<Pod> items = null;
        if (podList != null) {
            items = podList.getItems();
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("No pods found for ReplicationController " + KubernetesHelper.summaryText(replicationController));
        }
        List<Pod> pods = KubernetesHelper.getPodsForReplicationController(replicationController, items);
        return clientForPod(pods);
    }


    /**
     * Returns a client for the first working pod for the given replication controller
     */
    public J4pClient clientForReplicationController(String replicationControllerName, String namespace) {
        ReplicationController replicationController = requireReplicationController(replicationControllerName, namespace);
        return clientForReplicationController(replicationController);
    }



    /**
     * Returns a client for the first working pod for the given replication controller
     */
    public J4pClient clientForReplicationController(String replicationControllerName) {
        ReplicationController replicationController = kubernetes.replicationControllers().withName(replicationControllerName).get();
        Objects.requireNonNull(replicationController, "No ReplicationController found for name: " + replicationControllerName);
        return clientForReplicationController(replicationController);
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
    public List<J4pClient> clientsForReplicationController(String replicationControllerName, String namespace) {
        ReplicationController replicationController = requireReplicationController(replicationControllerName, namespace);
        List<Pod> pods = KubernetesHelper.getPodsForReplicationController(replicationController,
                kubernetes.pods().inNamespace(namespace).list().getItems());
        return clientsForPod(pods);
    }



    /**
     * Returns a client for the first working pod for the given service
     */
    public J4pClient clientForService(String serviceName, String namespace) {
        List<Pod> pods = KubernetesHelper.getPodsForService(requireService(serviceName, namespace),
                kubernetes.pods().inNamespace(namespace).list().getItems());
        return clientForPod(pods);
    }


    /**
     * Returns a client for the first working pod for the given service
     */
    public J4pClient clientForService(String serviceName) {
        List<Pod> pods = KubernetesHelper.getPodsForService(requireService(serviceName),
                kubernetes.pods().list().getItems());
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
    public List<J4pClient> clientsForService(String serviceName, String namespace) {
        List<Pod> pods = KubernetesHelper.getPodsForService(requireService(serviceName, namespace),
                kubernetes.pods().inNamespace(namespace).list().getItems());
        return clientsForPod(pods);
    }

    /**
     * Returns all the clients for the first working pod for the given service
     */
    public List<J4pClient> clientsForService(String serviceName) {
        List<Pod> pods = KubernetesHelper.getPodsForService(requireService(serviceName),
                kubernetes.pods().list().getItems());
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
    public J4pClient  clientForPod(Pod pod) {
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
                        if (useKubeProxy) {
                            URL masterUrl = getKubernetes().getMasterUrl();
                            ObjectMeta metadata = pod.getMetadata();
                            String namespace = metadata.getNamespace();
                            String podName = metadata.getName();
                            String jolokiaUrl = URLUtils.join(masterUrl.toString(), "/api/v1/namespaces/" + namespace + "/pods/" + locateJolokiaProtocol() + ":" + podName + ":8778/proxy/jolokia/");
                            LOG.info("Using jolokia URL: " + jolokiaUrl);
                            return createJolokiaClient(container, jolokiaUrl);
                        }
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
        String jolokiaUrl = locateJolokiaProtocol() + "://" + host + ":" + hostPort + "/jolokia/";
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

    public boolean isUseKubeProxy() {
        return useKubeProxy;
    }

    public void setUseKubeProxy(boolean useKubeProxy) {
        this.useKubeProxy = useKubeProxy;
    }

    public Filter<Pod> getPodFilter() {
        return podFilter;
    }

    public void setPodFilter(Filter<Pod> podFilter) {
        this.podFilter = podFilter;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public AuthenticationMode getAuthenticationMode() {
        return authenticationMode;
    }

    public void setAuthenticationMode(AuthenticationMode authenticationMode) {
        this.authenticationMode = authenticationMode;
    }

    protected J4pClient createJolokiaClient(Container container, String jolokiaUrl) {
        String name = container.getName();
        LOG.debug("Creating jolokia client for : " + name + " at URL: " + jolokiaUrl);
        J4pClientBuilder builder = J4pClient.url(jolokiaUrl);

        if (useKubeProxy) {
            // When using the https proxy, inject the Kubernetes client's SSL context
            URL masterUrl = getKubernetes().getMasterUrl();
            if (masterUrl != null && masterUrl.toString().startsWith("https")) {
                try {
                    SSLContext sslCtx = SSLUtils.sslContext(kubernetes.getConfiguration());
                    ConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslCtx);
                    builder = builder.sslConnectionSocketFactory(factory);
                } catch (Exception e) {
                    LOG.warn("Unable to inject the Kubernetes SSL context into the Jolokia client. Using the default context", e);
                }
            }
        }

        AuthenticationMode mode = locateAuthenticationMode();
        switch (mode) {
        case BEARER:
            builder = builder.authenticator(new BearerTokenAuthenticator());

            String token = kubernetes.getConfiguration().getOauthToken();
            builder = builder.user(token);
            break;
        case BASIC:
            builder = builder.authenticator(new BasicAuthenticator());
            if (Strings.isNotBlank(user)) {
                builder = builder.user(user);
            }
            if (Strings.isNotBlank(password)) {
                builder = builder.password(password);
            }
            break;
        default:
            throw new IllegalStateException("Unsupported authentication mode: " + mode);
        }

        return builder.build();
    }

    /**
     * Returns the jolokia protocol or detects it from the environment.
     */
    protected String locateJolokiaProtocol() {
        if (this.protocol != null) {
            return protocol;
        }

        if (KubernetesHelper.isOpenShift(kubernetes)) {
            // Jolokia is secured by default on Openshift
            return "https";
        }

        return "http";
    }

    protected AuthenticationMode locateAuthenticationMode() {
        if (this.authenticationMode != null) {
            return this.authenticationMode;
        }

        if (KubernetesHelper.isOpenShift(kubernetes)) {
            // Jolokia needs the Bearer token by default on Openshift
            return AuthenticationMode.BEARER;
        }

        return AuthenticationMode.BASIC;
    }

    protected ReplicationController requireReplicationController(String replicationControllerName, String namespace) {
        ReplicationController answer = kubernetes.replicationControllers().inNamespace(namespace).withName(replicationControllerName).get();
        Objects.requireNonNull(answer, "No ReplicationController found for namespace: " + namespace + " name: " + replicationControllerName);
        return answer;
    }

    protected Service requireService(String serviceName) {
        Service answer = kubernetes.services().withName(serviceName).get();
        Objects.requireNonNull(answer, "No Service found for name: " + serviceName);
        return answer;
    }

    protected Service requireService(String serviceName, String namespace) {
        Service answer = kubernetes.services().inNamespace(namespace).withName(serviceName).get();
        Objects.requireNonNull(answer, "No Service found for namespace: " + namespace + " name: " + serviceName);
        return answer;
    }

    // =================================================================================================

    public enum AuthenticationMode {
        BASIC,
        BEARER
    }
}
