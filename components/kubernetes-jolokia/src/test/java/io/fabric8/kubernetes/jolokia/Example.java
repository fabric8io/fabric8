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
package io.fabric8.kubernetes.jolokia;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 */
public class Example {
    private static final transient Logger LOG = LoggerFactory.getLogger(Example.class);

    JolokiaClients clients = new JolokiaClients();
    KubernetesClient kubernetes = clients.getKubernetes();

    public static void main(String[] args) {
        String selector = null;
        if (args.length > 0) {
            selector = args[0];
        }
        Example example = new Example();
        example.findReplicationControllers(selector);

        example.findPods(selector);
    }

    public void findPods(String selector) {
        Map<String, Pod> podMap = KubernetesHelper.getSelectedPodMap(kubernetes, selector);
        Collection<Pod> pods = podMap.values();
        for (Pod pod : pods) {
            String host = KubernetesHelper.getHost(pod);
            List<Container> containers = KubernetesHelper.getContainers(pod);
            for (Container container : containers) {
                System.out.println("pod " + KubernetesHelper.getName(pod) + " container: " + container.getName() + " image: " + container.getImage());
                J4pClient jolokia = clients.clientForContainer(host, container, pod);

                if (jolokia != null) {
                    System.out.println("   has jolokia client: " + jolokia + " from host: " + host + " URL: " + jolokia.getUri());
                    try {
                        ObjectName objectName = new ObjectName("java.lang:type=OperatingSystem");
                        J4pResponse<J4pReadRequest> results = jolokia.execute(new J4pReadRequest(objectName, "SystemCpuLoad"));
                        Object value = results.getValue();
                        System.out.println("  System CPU Load: " + value);
                    } catch (Exception e) {
                        LOG.error("Failed to look up attribute. " + e, e);
                    }

                }
            }
        }
    }

    public void findReplicationControllers(String selector) {
        Map<String, ReplicationController> replicationControllerMap = KubernetesHelper.getSelectedReplicationControllerMap(kubernetes, selector);
        Collection<ReplicationController> replicationControllers = replicationControllerMap.values();
        for (ReplicationController replicationController : replicationControllers) {
            System.out.println("" + KubernetesHelper.getName(replicationController));
        }
    }
}
