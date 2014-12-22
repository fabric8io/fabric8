/**
 *  Copyright 2005-2014 Red Hat, Inc.
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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerManifest;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodState;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.ReplicationControllerState;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServiceSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fabric8.kubernetes.api.KubernetesHelper.getId;

/**
 * A simple example program testing out the REST API
 */
public class Example {
    public static void main(String... args) {
        KubernetesFactory kubeFactory = new KubernetesFactory();
        if (args.length > 0) {
            kubeFactory.setAddress(args[0]);
        }
        System.out.println("Connecting to kubernetes on: " + kubeFactory.getAddress());

        try {
            Kubernetes kube = kubeFactory.createKubernetes();
            listPods(kube);
            listServices(kube);
            listReplicationControllers(kube);
            createPod(kube, kubeFactory);
            listPods(kube);
        } catch (Exception e) {
            System.out.println("FAILED: " + e);
            e.printStackTrace();
        }
    }

    protected static void createPod(Kubernetes kubernetes, KubernetesFactory kubernetesFactory) throws Exception {
        String name = "cheese";
        String image = "fabric8/fabric8";

        Pod pod = new Pod();
        pod.setUid(name);

        Map<String, String> labels = new HashMap<>();
        labels.put("fabric8", "true");
        labels.put("container", name);

        pod.setLabels(labels);
        PodState desiredState = new PodState();
        pod.setDesiredState(desiredState);
        ContainerManifest manifest = new ContainerManifest();
        // TODO
        // manifest.setVersion(ContainerManifest.Version.V_1_BETA_1);
        manifest.setVersion("v1beta1");
        desiredState.setManifest(manifest);

        Container manifestContainer = new Container();
        manifestContainer.setName(name);
        manifestContainer.setImage(image);

        List<Container> containers = new ArrayList<>();
        containers.add(manifestContainer);
        manifest.setContainers(containers);

        System.out.println("About to create pod on " + kubernetesFactory.getAddress() + " with " + pod);
        kubernetes.createPod(pod);
        System.out.println("Created pod: " + name);
        System.out.println();
    }

    protected static void listPods(Kubernetes kube) {
        System.out.println("Looking up pods");
        PodList pods = kube.getPods();
        //System.out.println("Got pods: " + pods);
        List<Pod> items = pods.getItems();
        for (Pod item : items) {
            System.out.println("Pod " + getId(item) + " created: " + item.getCreationTimestamp());
            PodState desiredState = item.getDesiredState();
            if (desiredState != null) {
                ContainerManifest manifest = desiredState.getManifest();
                if (manifest != null) {
                    List<Container> containers = manifest.getContainers();
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

    protected static void listServices(Kubernetes kube) {
        System.out.println("Looking up services");
        ServiceList services = kube.getServices();
        List<Service> items = services.getItems();
        for (Service item : items) {
            ServiceSpec spec = item.getSpec();
            System.out.println("Service " + getId(item) + " labels: " + item.getLabels() + " selector: " + spec.getSelector() + " port: " + spec.getPort());
        }
        System.out.println();

    }

    protected static void listReplicationControllers(Kubernetes kube) {
        System.out.println("Looking up replicationControllers");
        ReplicationControllerList replicationControllers = kube.getReplicationControllers();
        List<ReplicationController> items = replicationControllers.getItems();
        for (ReplicationController item : items) {
            ReplicationControllerState desiredState = item.getDesiredState();
            if (desiredState != null) {
                System.out.println("ReplicationController " + getId(item) + " labels: " + item.getLabels()
                        + " replicas: " + desiredState.getReplicas() + " replicatorSelector: " + desiredState.getReplicaSelector() + " podTemplate: " + desiredState.getPodTemplate());
            } else {
                System.out.println("ReplicationController " + getId(item) + " labels: " + item.getLabels() + " no desiredState");
            }
        }
        System.out.println();
    }

}
