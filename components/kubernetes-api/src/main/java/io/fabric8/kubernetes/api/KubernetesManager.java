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

import io.fabric8.utils.JMXUtils;
import io.fabric8.utils.Strings;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;

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
        Controller controller = new Controller(kubernetes);
        return controller.applyJson(json);
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
        return answer;
    }

    public KubernetesClient getKubernetes() {
        return kubernetes;
    }

    public void setKubernetes(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;
    }
}
