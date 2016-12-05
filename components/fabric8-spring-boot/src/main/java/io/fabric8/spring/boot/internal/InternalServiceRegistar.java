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
package io.fabric8.spring.boot.internal;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.spring.boot.AbstractServiceRegistar;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class InternalServiceRegistar extends AbstractServiceRegistar {

    private static final String HOST_SUFFIX = "_SERVICE_HOST";
    private static final String SERVICE_PORT = "_SERVICE_PORT";
    private static final String PROTO_SUFFIX = "_TCP_PROTO";


    @Override
    public Service getService(String serviceName) {
        Map<String, String> env = System.getenv();
        String prefix = serviceName.toUpperCase();
        String serviceHost = env.get(prefix + HOST_SUFFIX);

        String defaultPortName = prefix + SERVICE_PORT;
        String namedPortPrefix = defaultPortName + "_";

        List<ServicePort> servicePorts = new ArrayList<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(namedPortPrefix)) {
                String name = key.substring(namedPortPrefix.length());
                String portValue = entry.getValue();
                String protocolValue = env.get(key + "_" + PROTO_SUFFIX);
                servicePorts.add(new ServicePortBuilder()
                        .withName(name.toLowerCase())
                        .withPort(Integer.parseInt(portValue))
                        .withProtocol(protocolValue != null ? protocolValue : "TCP")
                        .build());
            }
        }

        //Check if we need to fallback to single port.
        if (servicePorts.isEmpty()) {
            String portValue = env.get(defaultPortName);
            String protocolValue = env.get(defaultPortName + PROTO_SUFFIX);

            servicePorts.add(new ServicePortBuilder()
                    .withPort(Integer.parseInt(portValue))
                    .withProtocol(protocolValue != null ? protocolValue : "TCP")
                    .build());
        }

        return new ServiceBuilder()
                .withNewMetadata()
                    .withName(serviceName)
                .endMetadata()
                .withNewSpec()
                .withClusterIP(serviceHost)
                .withPorts(servicePorts)
                .endSpec()
                .build();
    }
}
