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
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodListSchemaAssert;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.PodSchemaAssert;
import io.fabric8.kubernetes.api.model.ReplicationControllerListSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerListSchemaAssert;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchemaAssert;
import io.fabric8.kubernetes.api.model.ServiceListSchema;
import io.fabric8.kubernetes.api.model.ServiceListSchemaAssert;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import io.fabric8.kubernetes.api.model.ServiceSchemaAssert;
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

    public PodListSchemaAssert podList() {
        PodListSchema pods = client.getPods();
        return assertThat(pods).isNotNull();
    }

    public PodsAssert pods() {
        List<PodSchema> pods = getPods();
        return Assertions.assertThat(pods);
    }

    protected List<PodSchema> getPods() {
        PodListSchema podList = client.getPods();
        assertThat(podList).isNotNull();
        List<PodSchema> pods = podList.getItems();
        assertThat(pods).isNotNull();
        return pods;
    }

    public ReplicationControllerListSchemaAssert replicationControllerList() {
        ReplicationControllerListSchema replicationControllers = client.getReplicationControllers();
        return assertThat(replicationControllers).isNotNull();
    }

    public ListAssert<ReplicationControllerSchema> replicationControllers() {
        ReplicationControllerListSchema replicationControllerList = client.getReplicationControllers();
        assertThat(replicationControllerList).isNotNull();
        List<ReplicationControllerSchema> replicationControllers = replicationControllerList.getItems();
        return (ListAssert<ReplicationControllerSchema>) assertThat(replicationControllers);
    }

    public ServiceListSchemaAssert serviceList() {
        ServiceListSchema serviceList = client.getServices();
        return assertThat(serviceList).isNotNull();
    }

    public ListAssert<ServiceSchema> services() {
        ServiceListSchema serviceList = client.getServices();
        assertThat(serviceList).isNotNull();
        List<ServiceSchema> services = serviceList.getItems();
        return (ListAssert<ServiceSchema>) assertThat(services);
    }

    /**
     * Asserts that we can find the given replication controller and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForReplicationController(String replicationControllerId) {
        ReplicationControllerSchema replicationController = getReplicationController(replicationControllerId);
        return podsForReplicationController(replicationController);
    }

    /**
     * Asserts that we can find the given replication controller and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForReplicationController(ReplicationControllerSchema replicationController) {
        List<PodSchema> allPods = getPods();
        List<PodSchema> pods = KubernetesHelper.getPodsForReplicationController(replicationController, allPods);
        return Assertions.assertThat(pods);
    }

    /**
     * Asserts that we can find the given service and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForService(String serviceId) {
        ServiceSchema service = getService(serviceId);
        return podsForService(service);
    }

    /**
     * Asserts that we can find the given service and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForService(ServiceSchema service) {
        List<PodSchema> allPods = getPods();
        List<PodSchema> pods = KubernetesHelper.getPodsForService(service, allPods);
        return Assertions.assertThat(pods);
    }

    /**
     * Asserts that the replication controller can be found for the given ID
     */
    public ReplicationControllerSchemaAssert replicationController(String replicationControllerId) {
        return assertThat(getReplicationController(replicationControllerId));
    }

    protected ReplicationControllerSchema getReplicationController(String replicationControllerId) {
        assertThat(replicationControllerId).isNotNull();
        ReplicationControllerSchema replicationController = null;
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
    public ServiceSchemaAssert service(String serviceId) {
        return assertThat(getService(serviceId));
    }

    protected ServiceSchema getService(String serviceId) {
        assertThat(serviceId).isNotNull();
        ServiceSchema service = null;
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
    public PodSchemaAssert pod(String podId) {
        return assertThat(getPod(podId));
    }

    protected PodSchema getPod(String podId) {
        assertThat(podId).isNotNull();
        PodSchema pod = null;
        try {
            pod = client.getPod(podId);
        } catch (NotFoundException e) {
            fail("Could not find pod for '" + podId + "'");
        }
        assertThat(pod).isNotNull();
        return pod;
    }

}
