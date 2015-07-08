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
package io.fabric8.spring.boot.internal;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.spring.boot.AbstractServiceRegistar;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.regex.Pattern;

@Configuration
public class InternalServiceRegistar extends AbstractServiceRegistar {

    private static final String SERVICE = "service";
    private static final String SERVICE_HOST_REGEX = "(?<service>[A-Z_]+)_SERVICE_HOST";

    private static final Pattern SERVICE_HOST_PATTERN = Pattern.compile(SERVICE_HOST_REGEX);

    private static final String HOST_SUFFIX = "_SERVICE_HOST";
    private static final String PORT_SUFFIX = "_SERVICE_PORT";
    private static final String PROTO_SUFFIX = "_TCP_PROTO";


    @Override
    public Service getService(String name) {
        Map<String, String> env = System.getenv();
        String serviceHost = env.get(name + HOST_SUFFIX);
        String port = env.get(name + PORT_SUFFIX);
        String protocol = env.get(name + PORT_SUFFIX + "_" + port + PROTO_SUFFIX);

        return new ServiceBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withPortalIP(serviceHost)
                .addNewPort()
                .withNewTargetPort(port)
                .withProtocol(protocol)
                .endPort()
                .endSpec()
                .build();
    }
}
