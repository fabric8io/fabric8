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
package io.fabric8.kubernetes.api;

import io.fabric8.kubernetes.api.model.ControllerDesiredState;
import io.fabric8.kubernetes.api.model.DesiredState;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.ManifestSchema;
import io.fabric8.kubernetes.api.model.PodListSchema;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerListSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceListSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        PodSchema pod = new PodSchema();
        pod.setId(name);

        Map<String, String> labels = new HashMap<>();
        labels.put("fabric8", "true");
        labels.put("container", name);

        pod.setLabels(labels);
        DesiredState desiredState = new DesiredState();
        pod.setDesiredState(desiredState);
        ManifestSchema manifest = new ManifestSchema();
        manifest.setVersion(ManifestSchema.Version.V_1_BETA_1);
        desiredState.setManifest(manifest);

        ManifestContainer manifestContainer = new ManifestContainer();
        manifestContainer.setName(name);
        manifestContainer.setImage(image);

        List<ManifestContainer> containers = new ArrayList<>();
        containers.add(manifestContainer);
        manifest.setContainers(containers);

        System.out.println("About to create pod on " + kubernetesFactory.getAddress() + " with " + pod);
        kubernetes.createPod(pod);
        System.out.println("Created pod: " + name);
        System.out.println();
    }

    protected static void listPods(Kubernetes kube) {
        System.out.println("Looking up pods");
        PodListSchema pods = kube.getPods();
        //System.out.println("Got pods: " + pods);
        List<PodSchema> items = pods.getItems();
        for (PodSchema item : items) {
            System.out.println("PodSchema " + item.getId() + " created: " + item.getCreationTimestamp());
            DesiredState desiredState = item.getDesiredState();
            if (desiredState != null) {
                ManifestSchema manifest = desiredState.getManifest();
                if (manifest != null) {
                    List<ManifestContainer> containers = manifest.getContainers();
                    for (ManifestContainer container : containers) {
                        System.out.println("Container " + container.getImage() + " " + container.getCommand() + " ports: " + container.getPorts());
                    }
                }
            }
        }
        System.out.println();

    }

    protected static void listServices(Kubernetes kube) {
        System.out.println("Looking up services");
        ServiceListSchema services = kube.getServices();
        List<ServiceSchema> items = services.getItems();
        for (ServiceSchema item : items) {
            System.out.println("Service " + item.getId() + " labels: " + item.getLabels() + " selector: " + item.getSelector() + " port: " + item.getPort());
        }
        System.out.println();

    }

    protected static void listReplicationControllers(Kubernetes kube) {
        System.out.println("Looking up replicationControllers");
        ReplicationControllerListSchema replicationControllers = kube.getReplicationControllers();
        List<ReplicationControllerSchema> items = replicationControllers.getItems();
        for (ReplicationControllerSchema item : items) {
            ControllerDesiredState desiredState = item.getDesiredState();
            if (desiredState != null) {
                System.out.println("ReplicationController " + item.getId() + " labels: " + item.getLabels()
                        + " replicas: " + desiredState.getReplicas() + " replicatorSelector: " + desiredState.getReplicaSelector() + " podTemplate: " + desiredState.getPodTemplate());
            } else {
                System.out.println("ReplicationController " + item.getId() + " labels: " + item.getLabels() + " no desiredState");
            }
        }
        System.out.println();
    }

}
