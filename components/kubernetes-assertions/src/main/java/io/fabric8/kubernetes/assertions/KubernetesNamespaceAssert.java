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
import io.fabric8.kubernetes.api.model.ServiceSpecAssert;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.assertj.core.api.ListAssert;

import java.util.List;

import static io.fabric8.kubernetes.assertions.internal.Assertions.assertThat;

/**
 * Provides a set of assertions for a namespace
 */
public class KubernetesNamespaceAssert extends KubernetesAssert {
    private final KubernetesClient client;
    private final String namespace;

    public KubernetesNamespaceAssert(KubernetesClient client, String namespace) {
        super(client);
        this.client = client;
        this.namespace = namespace;
    }

    public String namespace() {
        return namespace;
    }

    public PodsAssert podList() {
        PodList pods = client.pods().inNamespace(namespace).list();
        return podList(pods);
    }


    @Override
    public PodsAssert pods() {
        return pods(namespace);
    }


    @Override
    public ReplicationControllerListAssert replicationControllerList() {
        ReplicationControllerList replicationControllers = client.replicationControllers().inNamespace(namespace).list();
        return assertThat(replicationControllers).isNotNull();
    }

    @Override
    public ListAssert<ReplicationController> replicationControllers() {
        return replicationControllers(namespace);
    }

    @Override
    public ServiceListAssert serviceList() {
        ServiceList serviceList = client.services().inNamespace(namespace).list();
        return assertThat(serviceList).isNotNull();
    }

    @Override
    public ListAssert<Service> services() {
        return services(namespace);
    }


    /**
     * Returns the first running pod for the given replication controller or throws an assert
     */
    public Pod podForReplicationController(String replicationControllerName) {
        PodsAssert forgePodsAssert = podsForReplicationController(replicationControllerName).runningStatus();
        forgePodsAssert.describedAs("pods for " + replicationControllerName).isNotEmpty();
        List<Pod> pods = forgePodsAssert.get();
        return pods.get(0);
    }

    /**
     * Asserts that we can find the given replication controller and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForReplicationController(String name) {
        ReplicationController replicationController = getReplicationController(name, namespace);
        return podsForReplicationController(replicationController);
    }


    /**
     * Asserts that we can find the given service and match it to a list of pods, returning the pods for further assertions
     */
    public PodsAssert podsForService(String name) {
        Service service = getService(name, namespace);
        return podsForService(service);
    }


    /**
     * Asserts that the replication controller can be found for the given ID and namespace.
     */
    public ReplicationControllerAssert replicationController(String name) {
        return assertThat(getReplicationController(name, namespace));
    }



    /**
     * Asserts that the service can be found for the given ID and namespace
     */
    public ServiceAssert service(String name) {
        return assertThat(getService(name, namespace));
    }

    /**
     * Asserts that the service can be found for the given ID and namespace and has a port of the given value
     */
    public void hasServicePort(String serviceName, int port) {
        hasServicePort(serviceName, namespace ,port);
    }

    public ServiceSpecAssert serviceSpec(String serviceName) {
        return assertThat(getServiceSpec(serviceName, namespace));
    }

    public PodAssert pod(String podName) {
        return assertThat(getPod(podName, namespace));
    }

}
