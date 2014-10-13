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

import io.fabric8.common.util.JMXUtils;

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
        Object dto = KubernetesHelper.loadJson(json);
        controller.apply(dto, "REST call");
        return "";
    }

    public KubernetesClient getKubernetes() {
        return kubernetes;
    }

    public void setKubernetes(KubernetesClient kubernetes) {
        this.kubernetes = kubernetes;
    }
}
