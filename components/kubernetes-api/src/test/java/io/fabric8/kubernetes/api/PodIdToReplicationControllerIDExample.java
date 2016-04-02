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

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.List;

/**
 */
public class PodIdToReplicationControllerIDExample {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Arguments: kuberneteMasterUrl namespace podID");
            return;
        }
        String kuberneteMasterUrl = args[0];
        String namespace = args[1];
        String podID = args[2];
        System.out.println("Looking up ReplicationController for pod ID: " + podID);
        KubernetesClient client = new DefaultKubernetesClient(new ConfigBuilder().withMasterUrl(kuberneteMasterUrl).build());
        Pod pod = (Pod) client.pods().inNamespace(namespace).withName(podID);
        pod.getMetadata().getLabels();
        List<ReplicationController> replicationControllers = client.replicationControllers().inNamespace(namespace).withLabels(pod.getMetadata().getLabels()).list().getItems();
        if (replicationControllers.size() == 1) {
            ReplicationController replicationController = replicationControllers.get(0);
            String id = KubernetesHelper.getName(replicationController);
            System.out.println("Found replication controller: " + id);
        } else {
            System.out.println("Could not find replication controller!");
        }
    }
}
