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
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodListAssert;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodAssert;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.ReplicationControllerListAssert;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerAssert;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServiceListAssert;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAssert;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecAssert;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;

import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;

import static io.fabric8.kubernetes.assertions.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


/**
 * An assertion class for making assertions about the current pods, services and replication controllers
 * using the <a href="http://joel-costigliola.github.io/assertj">assertj library</a>
 */
public class KubernetesAssert extends AbstractAssert<KubernetesAssert, KubernetesClient> {
    private final KubernetesClient client;

    public KubernetesAssert(KubernetesClient client) {
        super(client, KubernetesAssert.class);
        this.client = client;
    }

    public PodListAssert podList() {
        PodList pods = client.getPods();
        return assertThat(pods).isNotNull();
    }

    public PodsAssert pods() {
        List<Pod> pods = getPods(client.getNamespace());
        return Assertions.assertThat(pods);
    }

    public PodsAssert pods(String namespace) {
        List<Pod> pods = getPods(namespace);
        return Assertions.assertThat(pods);
    }

    protected List<Pod> getPods(String namespace) {
        PodList podList = client.getPods(namespace);
        assertThat(podList).isNotNull();
        List<Pod> pods = podList.getItems();
        assertThat(pods).isNotNull();
        return pods;
    }

    public ReplicationControllerListAssert replicationControllerList() {
        ReplicationControllerList replicationControllers = client.getReplicationControllers();
        return assertThat(replicationControllers).isNotNull();
    }

    public ReplicationControllerListAssert replicationControllerList(String namespace) {
        ReplicationControllerList replicationControllers = client.getReplicationControllers(namespace);
        return assertThat(replicationControllers).isNotNull();
    }

    public ListAssert<ReplicationController> replicationControllers() {
        return replicationControllers(client.getNamespace());
    }

    public ListAssert<ReplicationController> replicationControllers(String namespace) {
        ReplicationControllerList replicationControllerList = client.getReplicationControllers(namespace);
        assertThat(replicationControllerList).isNotNull();
        List<ReplicationController> replicationControllers = replicationControllerList.getItems();
        return (ListAssert<ReplicationController>) assertThat(replicationControllers);
    }

    public ServiceListAssert serviceList() {
        ServiceList serviceList = client.getServices();
        return assertThat(serviceList).isNotNull();
    }

    public ServiceListAssert serviceList(String namespace) {
        ServiceList serviceList = client.getServices(namespace);
        return assertThat(serviceList).isNotNull();
    }

    public ListAssert<Service> services() {
       return services(client.getNamespace());
    }

    public ListAssert<Service> services(String namespace) {
        ServiceList serviceList = client.getServices(namespace);
        assertThat(serviceList).isNotNull();
        List<Service> services = serviceList.getItems();
        return (ListAssert<Service>) assertThat(services);
    }

    /**
     * Asserts that we can find the given replication controller and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForReplicationController(String replicationControllerId) {
        return podsForReplicationController(replicationControllerId, client.getNamespace());
    }

    /**
     * Asserts that we can find the given replication controller and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForReplicationController(String replicationControllerId, String namespace) {
        ReplicationController replicationController = getReplicationController(replicationControllerId, namespace);
        return podsForReplicationController(replicationController);
    }

    /**
     * Asserts that we can find the given replication controller and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForReplicationController(ReplicationController replicationController) {
        List<Pod> allPods = getPods(replicationController.getMetadata().getNamespace());
        List<Pod> pods = KubernetesHelper.getPodsForReplicationController(replicationController, allPods);
        return Assertions.assertThat(pods);
    }

    /**
     * Asserts that we can find the given service and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForService(String serviceId) {
        Service service = getService(serviceId, client.getNamespace());
        return podsForService(service);
    }

    /**
     * Asserts that we can find the given service and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForService(String serviceId, String namespace) {
        Service service = getService(serviceId, namespace);
        return podsForService(service);
    }

    /**
     * Asserts that we can find the given service and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForService(Service service) {
        List<Pod> allPods = getPods(service.getMetadata().getNamespace());
        List<Pod> pods = KubernetesHelper.getPodsForService(service, allPods);
        return Assertions.assertThat(pods);
    }

    /**
     * Asserts that the replication controller can be found for the given ID
     */
    public ReplicationControllerAssert replicationController(String replicationControllerId) {
        return assertThat(getReplicationController(replicationControllerId, client.getNamespace()));
    }

    /**
     * Asserts that the replication controller can be found for the given ID and namespace.
     */
    public ReplicationControllerAssert replicationController(String replicationControllerId, String namespace) {
        return assertThat(getReplicationController(replicationControllerId, namespace));
    }
    

    protected ReplicationController getReplicationController(String replicationControllerId, String namespace) {
        assertThat(replicationControllerId).isNotNull();
        ReplicationController replicationController = null;
        try {
            replicationController = client.getReplicationController(replicationControllerId, namespace);
        } catch (NotFoundException e) {
            fail("Could not find replicationController for '" + replicationControllerId + "'");
        }
        assertThat(replicationController).isNotNull();
        return replicationController;
    }


    /**
     * Asserts that the service can be found for the given ID
     */
    public ServiceAssert service(String serviceId) {
        return assertThat(getService(serviceId, client.getNamespace()));
    }

    /**
     * Asserts that the service can be found for the given ID and namespace
     */
    public ServiceAssert service(String serviceId, String namespace) {
        return assertThat(getService(serviceId, namespace));
    }

    /**
     * Asserts that the service spec can be found for the given ID
     */
    public ServiceSpecAssert serviceSpec(String serviceId) {
        return serviceSpec(serviceId, client.getNamespace());
    }

    /**
     * Asserts that the service can be found for the given ID and namespace and has a port of the given value
     */
    public void hasServicePort(String serviceId, String namespace, int port) {
        ServiceSpec spec = getServiceSpec(serviceId, namespace);
        boolean found = false;
        List<ServicePort> ports = spec.getPorts();
        List<Integer> portNumbers = new ArrayList<>();
        if (ports != null) {
            for (ServicePort servicePort : ports) {
                Integer aPort = servicePort.getPort();
                if (aPort != null) {
                    if (aPort == port) {
                        found = true;
                        break;
                    } else {
                        portNumbers.add(aPort);
                    }

                }
            }
        }
        assertThat(found).describedAs("No port found for " + port + " but found ports: " + portNumbers).isTrue();
    }

    /**
     * Asserts that the service can be found for the given ID and namespace and has a port of the given value
     */
    public void hasServicePort(String serviceId, int port) {
        hasServicePort(serviceId, client.getNamespace(), port);
    }

    /**
     * Asserts that the service spec can be found for the given ID and namespace
     */
    public ServiceSpecAssert serviceSpec(String serviceId, String namespace) {
        return assertThat(getServiceSpec(serviceId, namespace));
    }

    protected Service getService(String serviceId, String namespace) {
        assertThat(serviceId).isNotNull();
        Service service = null;
        try {
            service = client.getService(serviceId, namespace);
        } catch (NotFoundException e) {
            fail("Could not find service for '" + serviceId + "'");
        }
        assertThat(service).isNotNull();
        return service;
    }

    protected ServiceSpec getServiceSpec(String serviceId, String namespace) {
        Service service = getService(serviceId, namespace);
        ServiceSpec spec = service.getSpec();
        assertThat(spec).isNotNull();
        return spec;
    }


    /**
     * Asserts that the pod can be found for the given ID
     */
    public PodAssert pod(String podId) {
        return assertThat(getPod(podId, client.getNamespace()));
    }

    /**
     * Asserts that the pod can be found for the given ID and namespace
     */
    public PodAssert pod(String podId, String namespace) {
        return assertThat(getPod(podId, namespace));
    }

    protected Pod getPod(String podId, String namespace) {
        assertThat(podId).isNotNull();
        Pod pod = null;
        try {
            pod = client.getPod(namespace);
        } catch (NotFoundException e) {
            fail("Could not find pod for '" + podId + "'");
        }
        assertThat(pod).isNotNull();
        return pod;
    }

}
