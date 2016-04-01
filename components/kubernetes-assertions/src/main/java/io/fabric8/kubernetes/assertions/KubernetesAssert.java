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
package io.fabric8.kubernetes.assertions;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodAssert;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerAssert;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.ReplicationControllerListAssert;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAssert;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServiceListAssert;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecAssert;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;

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

    public KubernetesNamespaceAssert namespace(String namespace) {
        return new KubernetesNamespaceAssert(client, namespace);
    }

    public PodsAssert podList() {
        PodList pods = client.pods().list();
        return podList(pods);
    }

    public PodsAssert podList(PodList pods) {
        assertThat(pods).isNotNull();
        return podList(pods.getItems());
    }

    public PodsAssert podList(List<Pod> pods) {
        assertThat(pods).isNotNull();
        return new PodsAssert(pods, client);
    }

    public PodsAssert pods() {
        return pods(null);
    }

    public PodsAssert pods(String namespace) {
        List<Pod> pods = getPods(namespace);
        return podList(pods);
    }

    protected List<Pod> getPods(String namespace) {
        PodList podList = client.pods().inNamespace(namespace).list();
        assertThat(podList).isNotNull();
        List<Pod> pods = podList.getItems();
        podList(pods).isNotNull();
        return pods;
    }

    public ReplicationControllerListAssert replicationControllerList() {
        ReplicationControllerList replicationControllers = client.replicationControllers().list();
        return assertThat(replicationControllers).isNotNull();
    }

    public ReplicationControllerListAssert replicationControllerList(String namespace) {
        ReplicationControllerList replicationControllers = client.replicationControllers().inNamespace(namespace).list();
        return assertThat(replicationControllers).isNotNull();
    }

    public ListAssert<ReplicationController> replicationControllers() {
        return replicationControllers(null);
    }

    public ListAssert<ReplicationController> replicationControllers(String namespace) {
        ReplicationControllerList replicationControllerList = client.replicationControllers().inNamespace(namespace).list();
        assertThat(replicationControllerList).isNotNull();
        List<ReplicationController> replicationControllers = replicationControllerList.getItems();
        return (ListAssert<ReplicationController>) assertThat(replicationControllers);
    }

    public ServiceListAssert serviceList() {
        ServiceList serviceList = client.services().list();
        return assertThat(serviceList).isNotNull();
    }

    public ServiceListAssert serviceList(String namespace) {
        ServiceList serviceList = client.services().inNamespace(namespace).list();
        return assertThat(serviceList).isNotNull();
    }

    public ListAssert<Service> services() {
       return services(null);
    }

    public ListAssert<Service> services(String namespace) {
        ServiceList serviceList = client.services().inNamespace(namespace).list();
        assertThat(serviceList).isNotNull();
        List<Service> services = serviceList.getItems();
        return (ListAssert<Service>) assertThat(services);
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
        return podList(pods);
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
        return podList(pods);
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
            replicationController = client.replicationControllers().inNamespace(namespace).withName(replicationControllerId).get();
        } catch (Exception e) {
            fail("Could not find replicationController for '" + replicationControllerId + "'");
        }
        assertThat(replicationController).isNotNull();
        return replicationController;
    }

    /**
     * Asserts that the service can be found for the given ID and namespace
     */
    public ServiceAssert service(String serviceId, String namespace) {
        return assertThat(getService(serviceId, namespace));
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
     * Asserts that the service spec can be found for the given ID and namespace
     */
    public ServiceSpecAssert serviceSpec(String serviceId, String namespace) {
        return assertThat(getServiceSpec(serviceId, namespace));
    }

    protected Service getService(String serviceId, String namespace) {
        assertThat(serviceId).isNotNull();
        Service service = null;
        try {
            service = client.services().inNamespace(namespace).withName(serviceId).get();
        } catch (Exception e) {
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
     * Asserts that the pod can be found for the given ID and namespace
     */
    public PodAssert pod(String podId, String namespace) {
        return assertThat(getPod(podId, namespace));
    }

    protected Pod getPod(String podId, String namespace) {
        assertThat(podId).isNotNull();
        Pod pod = null;
        try {
            pod = client.pods().inNamespace(namespace).withName(podId).get();
        } catch (Exception e) {
            fail("Could not find pod for '" + podId + "'");
        }
        assertThat(pod).isNotNull();
        return pod;
    }

}
