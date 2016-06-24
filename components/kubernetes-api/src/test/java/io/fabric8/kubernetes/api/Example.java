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
package io.fabric8.kubernetes.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountList;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import static io.fabric8.kubernetes.api.KubernetesHelper.getPorts;
import static io.fabric8.kubernetes.api.KubernetesHelper.getSelector;

/**
 * A simple example program testing out the REST API
 */
public class Example {

    public static void main(String... args) {
        System.out.println("\n\nfabric8 Kubernetes-api example");
        KubernetesClient kube = new DefaultKubernetesClient();
        System.out.println("=========================================================================");

        try {
            listPods(kube);
            listReplicationControllers(kube);
            listServices(kube);
            listServiceAccounts(kube);
            listEndpoints(kube);

        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        } finally {
            kube.close();
        }
        System.out.println("=========================================================================");
    }

    protected static void listPods(KubernetesClient kube) {
        System.out.println("\n\nLooking up pods");
        System.out.println("=========================================================================");
        PodList pods = kube.pods().list();
        //System.out.println("Got pods: " + pods);
        List<Pod> items = pods.getItems();
        for (Pod item : items) {
            System.out.println("Pod " + KubernetesHelper.getName(item) + " with ip: " + item.getStatus().getPodIP() + " created: " + item.getMetadata().getCreationTimestamp());
            PodSpec spec = item.getSpec();
            if (spec != null) {
                List<Container> containers = spec.getContainers();
                if (containers != null) {
                    for (Container container : containers) {
                        System.out.println("Container " + container.getImage() + " " + container.getCommand() + " ports: " + container.getPorts());
                    }
                }
            }
            Map<String, ContainerStatus> currentContainers = KubernetesHelper.getCurrentContainers(item);
            System.out.println("Has " + currentContainers.size() + " container(s)");
            Set<Map.Entry<String, ContainerStatus>> entries = currentContainers.entrySet();
            for (Map.Entry<String, ContainerStatus> entry : entries) {
                String id = entry.getKey();
                ContainerStatus info = entry.getValue();
                System.out.println("Current container: " + id + " info: " + info);
            }
        }
        System.out.println();

    }

    protected static void listServices(KubernetesClient kube) {
        System.out.println("\n\nLooking up services");
        System.out.println("=========================================================================");
        ServiceList services = kube.services().list();
        List<Service> serviceItems = services.getItems();
        for (Service service : serviceItems) {
            System.out.println("Service " + KubernetesHelper.getName(service) + " labels: " + service.getMetadata().getLabels() + " selector: " + getSelector(service) + " ports: " + getPorts(service));
        }
        System.out.println();

    }

    protected static void listReplicationControllers(KubernetesClient kube) {
        System.out.println("\n\nLooking up replicationControllers");
        System.out.println("=========================================================================");
        ReplicationControllerList replicationControllers = kube.replicationControllers().list();
        List<ReplicationController> items = replicationControllers.getItems();
        for (ReplicationController item : items) {
            ReplicationControllerSpec replicationControllerSpec = item.getSpec();
            if (replicationControllerSpec != null) {
                System.out.println("ReplicationController " + KubernetesHelper.getName(item) + " labels: " + item.getMetadata().getLabels()
                        + " replicas: " + replicationControllerSpec.getReplicas() + " replicatorSelector: " + replicationControllerSpec.getSelector() + " podTemplate: " + replicationControllerSpec.getTemplate());
            } else {
                System.out.println("ReplicationController " + KubernetesHelper.getName(item) + " labels: " + item.getMetadata().getLabels() + " no replicationControllerSpec");
            }
        }
        System.out.println();
    }

    protected static void listServiceAccounts(KubernetesClient kube) {
        System.out.println("\n\nLooking up service accounts");
        System.out.println("=========================================================================");
        ServiceAccountList serviceAccounts = kube.serviceAccounts().list();
        List<ServiceAccount> serviceAccountItems = serviceAccounts.getItems();
        for (ServiceAccount serviceAccount : serviceAccountItems) {
            System.out.println("Service Account " + KubernetesHelper.getName(serviceAccount) + " labels: " + serviceAccount.getMetadata().getLabels());
        }
        System.out.println();    
    }

    protected static void listEndpoints(KubernetesClient kube) {
        System.out.println("\n\nLooking up endpoints");
        System.out.println("=========================================================================");
        EndpointsList endpoints = kube.endpoints().list();
        List<Endpoints> endpointItems = endpoints.getItems();
        for (Endpoints endpoint : endpointItems) {
            System.out.println("Endpoint " + KubernetesHelper.getName(endpoint) + " labels: " + endpoint.getMetadata().getLabels());
        }
        System.out.println();
    }

}
