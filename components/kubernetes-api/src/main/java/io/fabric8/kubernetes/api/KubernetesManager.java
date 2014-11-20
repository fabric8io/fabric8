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

import io.fabric8.kubernetes.api.model.CurrentState;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import io.fabric8.utils.JMXUtils;
import io.fabric8.utils.Strings;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Map;

/**
 * A simple MBean for performing custom operations on kubernetes
 */
public class KubernetesManager implements KubernetesManagerMXBean {
    public static ObjectName OBJECT_NAME;

    static {
        try {
            OBJECT_NAME = new ObjectName("io.fabric8:type=KubernetesManager");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }

    private KubernetesClient kubernetes = new KubernetesClient();

    public void init() {
        JMXUtils.registerMBean(this, OBJECT_NAME);
    }

    public void destroy() {
        JMXUtils.unregisterMBean(OBJECT_NAME);
    }

    @Override
    public String apply(String json) throws IOException {
        Controller controller = createController();
        return controller.applyJson(json);
    }

    /**
     * Returns the service URL of the given service name
     */
    @Override
    public String getServiceUrl(String serviceName) {
        // TODO we could cache these and update them in the background!
        Map<String, ServiceSchema> serviceMap = KubernetesHelper.getServiceMap(kubernetes);
        ServiceSchema service = serviceMap.get(serviceName);
        return KubernetesHelper.getServiceURL(service);
    }

    /**
     * Returns the pod IP of the given pod name
     */
    @Override
    public String getPodIP(String name) {
        // TODO we could cache these and update them in the background!
        Map<String, PodSchema> podMap = KubernetesHelper.getPodMap(kubernetes);
        PodSchema pod = podMap.get(name);
        if (pod != null) {
            CurrentState currentState = pod.getCurrentState();
            if (currentState != null) {
                String podIP = currentState.getPodIP();
                if (podIP != null) {
                    return podIP;
                }
            }
        }
        return null;
    }

    @Override
    public String getDockerRegistry() {
        // TODO we could find the docker registry by querying pods using a selector
        // for now lets just reuse the DOCKER_REGISTRY environment variable
        String answer = System.getenv("DOCKER_REGISTRY");
        if (Strings.isNotBlank(answer)) {
            if (!answer.contains("://")) {
                answer = "http://" + answer;
            }
        }
        if (Strings.isNullOrBlank(answer)) {
            // lets use the registry service
            String registryHost = System.getenv("REGISTRY_SERVICE_HOST");
            String registryPort = System.getenv("REGISTRY_SERVICE_PORT");
            if (Strings.isNotBlank(registryHost) && Strings.isNotBlank(registryPort)) {
                return registryHost + ":" + registryPort;
            }
        }
        return answer;
    }

    @Override
    public String getReplicationControllerIdForPod(String podId) {
        ReplicationControllerSchema replicationController = kubernetes.getReplicationControllerForPod(podId);
        if (replicationController != null) {
            return replicationController.getId();
        }
        return null;
    }

    public KubernetesClient getKubernetes() {
        return kubernetes;
    }

    public void setKubernetes(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;
    }


    protected Controller createController() {
        return new Controller(kubernetes);
    }
}
