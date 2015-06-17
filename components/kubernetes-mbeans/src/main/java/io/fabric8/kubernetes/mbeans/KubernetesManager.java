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
package io.fabric8.kubernetes.mbeans;

import io.fabric8.kubernetes.api.Controller;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.utils.JMXUtils;
import io.fabric8.utils.Strings;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Map;

import static io.fabric8.kubernetes.api.KubernetesHelper.getId;

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

    private KubernetesClient kubernetes;

    public KubernetesManager() {
        this(new KubernetesClient());
    }

    public KubernetesManager(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;
    }

    public void init() {
        JMXUtils.registerMBean(this, OBJECT_NAME);
    }

    public void destroy() {
        JMXUtils.unregisterMBean(OBJECT_NAME);
    }

    @Override
    public String apply(String json) throws Exception {
        Controller controller = createController();
        return controller.applyJson(json);
    }

    /**
     * Returns the service URL of the given service name
     */
    @Override
    public String getServiceUrl(String serviceName) {
        // TODO we could cache these and update them in the background!
        Map<String, Service> serviceMap = KubernetesHelper.getServiceMap(kubernetes);
        Service service = serviceMap.get(serviceName);
        return KubernetesHelper.getServiceURL(service);
    }

    /**
     * Returns the pod IP of the given pod name
     */
    @Override
    public String getPodUrl(String name, String portNumberOrName) {
        // TODO we could cache these and update them in the background!
        Map<String, Pod> podMap = KubernetesHelper.getPodMap(kubernetes);
        Pod pod = podMap.get(name);
        if (pod != null) {
            PodState currentState = pod.getCurrentState();
            if (currentState != null) {
                String protocol = "http://";

                // find the port either by port number or name
                ContainerPort port = KubernetesHelper.findContainerPortByNumberOrName(pod, portNumberOrName);
                Integer containerPort = port != null ? port.getContainerPort() : null;
                // TODO if we can detect HTTPS then add that too...

                // try use the Pod IP if we can
                String podIP = currentState.getPodIP();
                if (Strings.isNotBlank(podIP)) {
                    return addPortToIP(protocol + podIP, containerPort, portNumberOrName);
                }

                // lets default to host name for cases where we are on Jube and use the host port too
                String host = currentState.getHost();
                if (Strings.isNotBlank(host)) {
                    return addPortToIP(protocol + host, port != null ? port.getHostPort() : null, portNumberOrName);
                }
            }
        }
        return null;
    }

    protected String addPortToIP(String podIP, Integer containerPort, String portNumberOrName) {
        if (containerPort != null) {
            if (containerPort > 0 && containerPort != 80) {
                return podIP + ":" + containerPort;
            }
        }
        if (Strings.isNotBlank(portNumberOrName)) {
            return podIP + ":" + portNumberOrName;
        } else {
            return podIP;
        }
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
        ReplicationController replicationController = kubernetes.getReplicationControllerForPod(podId);
        if (replicationController != null) {
            return KubernetesHelper.getName(replicationController);
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
