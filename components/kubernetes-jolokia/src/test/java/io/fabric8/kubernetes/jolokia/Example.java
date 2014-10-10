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
package io.fabric8.kubernetes.jolokia;

import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ManifestContainer;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
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
    Kubernetes kubernetes = clients.getKubernetes();

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
        Map<String, PodSchema> podMap = KubernetesHelper.getPodMap(kubernetes, selector);
        Collection<PodSchema> pods = podMap.values();
        for (PodSchema pod : pods) {
            String host = KubernetesHelper.getHost(pod);
            List<ManifestContainer> containers = KubernetesHelper.getContainers(pod);
            for (ManifestContainer container : containers) {
                System.out.println("pod " + pod.getId() + " container: " + container.getName() + " image: " + container.getImage());
                J4pClient jolokia = clients.jolokiaClient(host, container);

                if (jolokia != null) {
                    System.out.println("   has jolokia client: " + jolokia);
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
        Map<String, ReplicationControllerSchema> replicationControllerMap = KubernetesHelper.getReplicationControllerMap(kubernetes, selector);
        Collection<ReplicationControllerSchema> replicationControllers = replicationControllerMap.values();
        for (ReplicationControllerSchema replicationController : replicationControllers) {
            System.out.println("" + replicationController.getId());
        }
    }
}
