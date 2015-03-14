/*
 * Copyright 2005-2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.fabric8.cdi;


import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;

import static io.fabric8.cdi.KubernetesHolder.KUBERNETES;

public class Services {

    private static final String KUBERNETES_NAMESPACE = "KUBERNETES_NAMESPACE";
    private static final String HOST_SUFFIX = "_SERVICE_HOST";
    private static final String PORT_SUFFIX = "_SERVICE_PORT";
    private static final String PROTO_SUFFIX = "_TCP_PROTO";
    public static final String DEFAULT_PROTO = "tcp";

    public static String toServiceUrl(String serviceId, String serviceProtocol) {
        io.fabric8.kubernetes.api.model.Service srv = null;
        String namespace = Systems.getEnvVarOrSystemProperty(KUBERNETES_NAMESPACE, (String) null);
        String serviceHost = serviceToHost(serviceId);
        String servicePort = serviceToPort(serviceId);
        String serviceProto = serviceProtocol != null ? serviceProtocol : serviceToProtocol(serviceId, servicePort);

        //1. Inside Kubernetes: Services as ENV vars
        if (Strings.isNotBlank(serviceHost) && Strings.isNotBlank(servicePort) && Strings.isNotBlank(serviceProtocol)) {
            return serviceProtocol + "://" + serviceHost + ":" + servicePort;
            //2. Anywhere: When namespace is passed System / Env var. Mostly needed for integration tests.
        } else if (Strings.isNotBlank(namespace)) {
            srv = KUBERNETES.getService(serviceId, namespace);
        } else {
            for (io.fabric8.kubernetes.api.model.Service s : KUBERNETES.getServices().getItems()) {
                if (s.getId().equals(serviceId)) {
                    srv = s;
                    break;
                }
            }
        }
        if (srv == null) {
            throw new IllegalArgumentException("No kubernetes service could be found for name: " + serviceId + " in namespace: " + namespace);
        }
        return (serviceProto + "://" + srv.getPortalIP() + ":" + srv.getPort()).toLowerCase();
    }

    public static String serviceToHost(String id) {
        return Systems.getEnvVarOrSystemProperty(toEnvVariable(id + HOST_SUFFIX), "");
    }

    public static String serviceToPort(String id) {
        return Systems.getEnvVarOrSystemProperty(toEnvVariable(id + PORT_SUFFIX), "");
    }

    public static String serviceToProtocol(String id, String servicePort) {
        return Systems.getEnvVarOrSystemProperty(toEnvVariable(id + PORT_SUFFIX + "_" + servicePort + PROTO_SUFFIX), DEFAULT_PROTO);
    }


    public static String toEnvVariable(String str) {
        return str.toUpperCase().replaceAll("-", "_");
    }
}
