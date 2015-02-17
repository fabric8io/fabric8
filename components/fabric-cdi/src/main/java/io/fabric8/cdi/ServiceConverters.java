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


import io.fabric8.cdi.annotations.Service;
import io.fabric8.utils.Strings;
import io.fabric8.utils.Systems;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import java.net.MalformedURLException;
import java.net.URL;

import static io.fabric8.cdi.KubernetesHolder.KUBERNETES;

public class ServiceConverters {

    private static final String KUBERNETES_NAMESPACE = "KUBERNETES_NAMESPACE";
    private static final String HOST_SUFFIX = "_SERVICE_HOST";
    private static final String PORT_SUFFIX = "_SERVICE_PORT";
    private static final String PROTO_SUFFIX = "_TCP_PROTO";
    private static final String DEFAULT_PROTO = "tcp";


    @Produces
    @Service
    public String serviceToString(InjectionPoint injectionPoint) {
        Annotated annotated = injectionPoint.getAnnotated();
        Service service = annotated.getAnnotation(Service.class);
        io.fabric8.kubernetes.api.model.Service srv = null;

        String id = service.value();
        String namespace = Systems.getEnvVarOrSystemProperty(KUBERNETES_NAMESPACE, (String) null);
        String serviceHost = hostOfService(id);
        String servicePort = portOfService(id);
        String serviceProtocol = protocolOfService(id, servicePort);

        //1. Inside Kubernetes: Services as ENV vars
        if (Strings.isNotBlank(serviceHost) && Strings.isNotBlank(servicePort) && Strings.isNotBlank(serviceProtocol)) {
            return serviceProtocol + "://" + serviceHost + ":" + servicePort;
        //2. Anywhere: When namespace is passed System / Env var. Mostly needed for integration tests.
        } else if (Strings.isNotBlank(namespace)) {
            srv = KUBERNETES.getService(service.value(), namespace);
        //3. Fallback
        //TODO: Check if we really need to fallback or fail.
        } else {
            for (io.fabric8.kubernetes.api.model.Service s : KUBERNETES.getServices().getItems()) {
                if (s.getId().equals(service.value())) {
                    srv = s;
                    break;
                }
            }
        }
        return (srv.getProtocol() + "://" + srv.getPortalIP() + ":" + srv.getPort()).toLowerCase();
    }


    @Produces
    @Service
    public URL serviceToUrl(InjectionPoint injectionPoint) {
        try {
            return new URL(serviceToString(injectionPoint));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String hostOfService(String id) {
        return Systems.getEnvVarOrSystemProperty(toEnv(id + HOST_SUFFIX), "");
    }

    private static String portOfService(String id) {
        return Systems.getEnvVarOrSystemProperty(toEnv(id + PORT_SUFFIX), "");
    }

    private static String protocolOfService(String id, String servicePort) {
        return Systems.getEnvVarOrSystemProperty(toEnv(id + PORT_SUFFIX + "_" + servicePort + PROTO_SUFFIX), DEFAULT_PROTO);
    }


    private static String toEnv(String str) {
        return str.toUpperCase().replaceAll("-", "_");
    }
}
