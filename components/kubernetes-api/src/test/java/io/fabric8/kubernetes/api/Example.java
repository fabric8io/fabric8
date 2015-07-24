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

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.*;

import static io.fabric8.kubernetes.api.KubernetesHelper.getPorts;
import static io.fabric8.kubernetes.api.KubernetesHelper.getSelector;

/**
 * A simple example program testing out the REST API
 */
public class Example {
    public static void main(String... args) {
        try {
            KubernetesClient kube = new DefaultKubernetesClient();
            listPods(kube);
            listServices(kube);
            listReplicationControllers(kube);
            createPod(kube);
            listPods(kube);
        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        }
    }

    protected static void createPod(KubernetesClient kubernetes) throws Exception {
        String name = "console2";
        String image = "fabric8/hawtio";

        Pod pod = new Pod();
        KubernetesHelper.setName(pod, name);

        Map<String, String> labels = new HashMap<>();
        labels.put("fabric8", "true");
        labels.put("container", name);

        pod.getMetadata().setLabels(labels);
        PodSpec podSpec = new PodSpec();
        pod.setSpec(podSpec);

        Container manifestContainer = new Container();
        manifestContainer.setName(name);
        manifestContainer.setImage(image);

        List<Container> containers = new ArrayList<>();
        containers.add(manifestContainer);
        podSpec.setContainers(containers);

        kubernetes.pods().inNamespace("mynamespace").create(pod);
        System.out.println("Created pod: " + name);
        System.out.println();
    }

    protected static void listPods(KubernetesClient kube) {
        System.out.println("Looking up pods");
        PodList pods = kube.pods().list();
        //System.out.println("Got pods: " + pods);
        List<Pod> items = pods.getItems();
        for (Pod item : items) {
            System.out.println("Pod " + KubernetesHelper.getName(item) + " created: " + item.getMetadata().getCreationTimestamp());
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
        System.out.println("Looking up services");
        ServiceList services = kube.services().list();
        List<Service> serviceItems = services.getItems();
        for (Service service : serviceItems) {
            System.out.println("Service " + KubernetesHelper.getName(service) + " labels: " + service.getMetadata().getLabels() + " selector: " + getSelector(service) + " ports: " + getPorts(service));
        }
        System.out.println();

    }

    protected static void listReplicationControllers(KubernetesClient kube) {
        System.out.println("Looking up replicationControllers");
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
        System.out.println("Looking up service accounts");
        ServiceAccountList serviceAccounts = kube.serviceAccounts().list();
        List<ServiceAccount> serviceAccountItems = serviceAccounts.getItems();
        for (ServiceAccount serviceAccount : serviceAccountItems) {
            System.out.println("Service Account " + KubernetesHelper.getName(serviceAccount) + " labels: " + serviceAccount.getMetadata().getLabels());
        }
        System.out.println();    
    }

}
