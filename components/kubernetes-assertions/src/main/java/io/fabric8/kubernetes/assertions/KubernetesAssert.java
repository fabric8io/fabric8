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

import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.HasMetadata;
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
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;

import java.io.IOException;
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

    /**
     * Creates an asserter using the given namespace
     *
     * @param namespace
     * @return an asserter using the given namespace
     */
    public KubernetesNamespaceAssert namespace(String namespace) {
        return new KubernetesNamespaceAssert(client, namespace);
    }

    /**
     * Finds all the resources that create pod selections (Deployment, DeploymentConfig, ReplicaSet, ReplicationController)
     * and create a {@link HasPodSelectionAssert} to make assertions on their pods that they startup etc.
     *
     * @return the assertion object for the deployment
     */
    public HasPodSelectionAssert deployments() {
        List<HasPodSelectionAssert> asserters = new ArrayList<>();
        List<HasMetadata> resources = new ArrayList<>();
        try {
            resources = KubernetesHelper.findKubernetesResourcesOnClasspath(new Controller(client));
        } catch (IOException e) {
            fail("Failed to load kubernetes resources on the classpath: " + e, e);
        }
        for (HasMetadata resource : resources) {
            HasPodSelectionAssert asserter = createPodSelectionAssert(resource);
            if (asserter != null) {
                asserters.add(asserter);
            }
        }
        String message = "No pod selection kinds found on the classpath such as Deployment, DeploymentConfig, ReplicaSet, ReplicationController";
        // TODO we don't yet support size > 1
        assertThat(asserters).describedAs(message).isNotEmpty();
        if( asserters.size() == 1 ) {
            return asserters.get(0);
        }
        return new MultiHasPodSelectionAssert(asserters);
    }

    protected HasPodSelectionAssert createPodSelectionAssert(HasMetadata resource) {
        if (resource instanceof DeploymentConfig) {
            DeploymentConfig deployment = (DeploymentConfig) resource;
            return new DeploymentConfigPodsAssert(client, deployment);
        } else if (resource instanceof Deployment) {
            Deployment deployment = (Deployment) resource;
            return new DeploymentPodsAssert(client, deployment);
        } else if (resource instanceof ReplicaSet) {
            ReplicaSet replica = (ReplicaSet) resource;
            return new ReplicaSetPodsAssert(client, replica);
        } else if (resource instanceof ReplicationController) {
            ReplicationController replica = (ReplicationController) resource;
            return new ReplicationControllerPodsAssert(client, replica);
        } else {
            return null;
        }
    }

    /**
     * Asserts that there is a deployment of the given name
     *
     * @return the assertion object for the deployment
     */
    public HasPodSelectionAssert deployment(String deploymentName) {
        String namespace = namespace();
        String qualifiedName = namespace + "." + deploymentName;
        OpenShiftClient openShiftClient = new Controller(client).getOpenShiftClientOrNull();
        if (openShiftClient != null && openShiftClient.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.APPS)) {
            DeploymentConfig deployment = openShiftClient.deploymentConfigs().inNamespace(namespace).withName(deploymentName).get();
            assertThat(deployment).describedAs("DeploymentConfig: " + qualifiedName).isNotNull().metadata().name().isEqualTo(deploymentName);
            return new DeploymentConfigPodsAssert(client, deployment);
        } else {
            Deployment deployment = client.extensions().deployments().inNamespace(namespace).withName(deploymentName).get();
            assertThat(deployment).describedAs("Deployment: " + qualifiedName).isNotNull().metadata().name().isEqualTo(deploymentName);
            return new DeploymentPodsAssert(client, deployment);
        }
    }

    public String namespace() {
        return client.getNamespace();
    }

    /**
     * Asserts that there is a ReplicaSet or ReplicationController of the given name
     *
     * @return the assertion object for the replicas
     */
    public HasPodSelectionAssert replicas(String replicaName) {
        String namespace = namespace();
        String qualifiedName = namespace + "." + replicaName;
        ReplicaSet replicasSet = client.extensions().replicaSets().withName(replicaName).get();
        if (replicasSet != null) {
            assertThat(replicasSet).describedAs("ReplicaSet: " + qualifiedName).metadata().name().isEqualTo(replicaName);
            return new ReplicaSetPodsAssert(client, replicasSet);
        } else {
            ReplicationController replicationController = client.replicationControllers().withName(replicaName).get();
            assertThat(replicationController).describedAs("No ReplicaSet or ReplicationController called: " + qualifiedName).isNotNull();
            assertThat(replicationController).describedAs("ReplicationController: " + qualifiedName).metadata().name().isEqualTo(replicaName);
            return new ReplicationControllerPodsAssert(client, replicationController);
        }
    }

    public PodsAssert podList() {
        PodList pods = client.pods().inNamespace(namespace()).list();
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
        List<Pod> pods = getPods(namespace());
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

    public ListAssert<ReplicationController> replicationControllers() {
        ReplicationControllerList replicationControllerList = client.replicationControllers().inNamespace(namespace()).list();
        assertThat(replicationControllerList).isNotNull();
        List<ReplicationController> replicationControllers = replicationControllerList.getItems();
        return (ListAssert<ReplicationController>) assertThat(replicationControllers);
    }

    public ServiceListAssert serviceList() {
        ServiceList serviceList = client.services().inNamespace(namespace()).list();
        return assertThat(serviceList).isNotNull();
    }

    public ServicesAssert services() {
        ServiceList serviceList = client.services().inNamespace(namespace()).list();
        assertThat(serviceList).isNotNull();
        List<Service> services = serviceList.getItems();
        return new ServicesAssert(client, services);
    }

    /**
     * Asserts that the given service name exist
     *
     * @return the assertion object on the given service
     */
    public ServicePodsAssert service(String serviceName) {
        Service service = client.services().inNamespace(namespace()).withName(serviceName).get();
        assertThat(service).describedAs("No service exists for name: " + serviceName).isNotNull();
        return new ServicePodsAssert(client, service);
    }

    /**
     * Asserts that we can find the given replication controller and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForReplicationController(String replicationControllerName) {
        ReplicationController replicationController = getReplicationController(replicationControllerName, namespace());
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
    public PodsAssert podsForService(String serviceName) {
        Service service = getService(serviceName, namespace());
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
     * Asserts that the replication controller can be found for the given name
     */
    public ReplicationControllerAssert replicationController(String replicationControllerName) {
        return assertThat(getReplicationController(replicationControllerName, namespace()));
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
     * Asserts that the service can be found for the given name and has a port of the given value
     */
    public void hasServicePort(String serviceId, int port) {
        ServiceSpec spec = getServiceSpec(serviceId, namespace());
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
     * Asserts that the service spec can be found for the given name
     */
    public ServiceSpecAssert serviceSpec(String serviceName) {
        return assertThat(getServiceSpec(serviceName, namespace()));
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
     * Asserts that the pod can be found for the given name
     */
    public PodAssert pod(String podName) {
        return assertThat(getPod(podName, namespace()));
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
