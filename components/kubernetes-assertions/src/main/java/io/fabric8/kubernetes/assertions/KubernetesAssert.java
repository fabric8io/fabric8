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
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;

import javax.ws.rs.NotFoundException;
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
        List<Pod> pods = getPods();
        return Assertions.assertThat(pods);
    }

    protected List<Pod> getPods() {
        PodList podList = client.getPods();
        assertThat(podList).isNotNull();
        List<Pod> pods = podList.getItems();
        assertThat(pods).isNotNull();
        return pods;
    }

    public ReplicationControllerListAssert replicationControllerList() {
        ReplicationControllerList replicationControllers = client.getReplicationControllers();
        return assertThat(replicationControllers).isNotNull();
    }

    public ListAssert<ReplicationController> replicationControllers() {
        ReplicationControllerList replicationControllerList = client.getReplicationControllers();
        assertThat(replicationControllerList).isNotNull();
        List<ReplicationController> replicationControllers = replicationControllerList.getItems();
        return (ListAssert<ReplicationController>) assertThat(replicationControllers);
    }

    public ServiceListAssert serviceList() {
        ServiceList serviceList = client.getServices();
        return assertThat(serviceList).isNotNull();
    }

    public ListAssert<Service> services() {
        ServiceList serviceList = client.getServices();
        assertThat(serviceList).isNotNull();
        List<Service> services = serviceList.getItems();
        return (ListAssert<Service>) assertThat(services);
    }

    /**
     * Asserts that we can find the given replication controller and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForReplicationController(String replicationControllerId) {
        ReplicationController replicationController = getReplicationController(replicationControllerId);
        return podsForReplicationController(replicationController);
    }

    /**
     * Asserts that we can find the given replication controller and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForReplicationController(ReplicationController replicationController) {
        List<Pod> allPods = getPods();
        List<Pod> pods = KubernetesHelper.getPodsForReplicationController(replicationController, allPods);
        return Assertions.assertThat(pods);
    }

    /**
     * Asserts that we can find the given service and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForService(String serviceId) {
        Service service = getService(serviceId);
        return podsForService(service);
    }

    /**
     * Asserts that we can find the given service and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForService(Service service) {
        List<Pod> allPods = getPods();
        List<Pod> pods = KubernetesHelper.getPodsForService(service, allPods);
        return Assertions.assertThat(pods);
    }

    /**
     * Asserts that the replication controller can be found for the given ID
     */
    public ReplicationControllerAssert replicationController(String replicationControllerId) {
        return assertThat(getReplicationController(replicationControllerId));
    }

    protected ReplicationController getReplicationController(String replicationControllerId) {
        assertThat(replicationControllerId).isNotNull();
        ReplicationController replicationController = null;
        try {
            replicationController = client.getReplicationController(replicationControllerId);
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
        return assertThat(getService(serviceId));
    }

    protected Service getService(String serviceId) {
        assertThat(serviceId).isNotNull();
        Service service = null;
        try {
            service = client.getService(serviceId);
        } catch (NotFoundException e) {
            fail("Could not find service for '" + serviceId + "'");
        }
        assertThat(service).isNotNull();
        return service;
    }


    /**
     * Asserts that the pod can be found for the given ID
     */
    public PodAssert pod(String podId) {
        return assertThat(getPod(podId));
    }

    protected Pod getPod(String podId) {
        assertThat(podId).isNotNull();
        Pod pod = null;
        try {
            pod = client.getPod(podId);
        } catch (NotFoundException e) {
            fail("Could not find pod for '" + podId + "'");
        }
        assertThat(pod).isNotNull();
        return pod;
    }

}
