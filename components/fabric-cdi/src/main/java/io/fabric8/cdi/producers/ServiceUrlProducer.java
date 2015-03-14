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

package io.fabric8.cdi.producers;

import io.fabric8.cdi.Services;
import io.fabric8.utils.Systems;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import java.util.Collections;
import java.util.Set;

public class ServiceUrlProducer implements Producer<String> {

    private static final String KUBERNETES_NAMESPACE = "KUBERNETES_NAMESPACE";
    private static final String HOST_SUFFIX = "_SERVICE_HOST";
    private static final String PORT_SUFFIX = "_SERVICE_PORT";
    private static final String PROTO_SUFFIX = "_TCP_PROTO";
    private static final String DEFAULT_PROTO = "tcp";
    
    private final String serviceId;

    public ServiceUrlProducer(String serviceId) {
        this.serviceId = serviceId;
    }
    
    public ServiceUrlProducer withServiceId(String serviceId) {
        return new ServiceUrlProducer(serviceId);
    }

    @Override
    public String produce(CreationalContext<String> ctx) {
        if (serviceId == null) {
            throw new IllegalArgumentException("No service id has been specified.");
        }
        return Services.toServiceUrl(serviceId);
    }

    @Override
    public void dispose(String instance) {
        //do nothing
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    private static String toEnv(String str) {
        return str.toUpperCase().replaceAll("-", "_");
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
}
