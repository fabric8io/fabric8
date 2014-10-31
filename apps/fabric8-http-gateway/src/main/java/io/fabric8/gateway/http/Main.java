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
package io.fabric8.gateway.http;

import io.fabric8.utils.Systems;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.weld.environment.se.bindings.Parameters;
import org.jboss.weld.environment.se.events.ContainerInitialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final String DEFAULT_PORT = "8080";
    public static final String DEFAULT_INDEX_ENABLED = "true";
    
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    
    @Inject FabricHTTPGateway fabricHTTPGateway;
    
    @Inject @Parameters String[] args;
    
    @Inject HTTPGatewayConfig gatewayConfig;
    
    public void main(@Observes ContainerInitialized event) {
        
        String serviceName = Systems.getEnvVarOrSystemProperty("HTTP_GATEWAY_SERVICE_ID", "HTTP_GATEWAY_SERVICE_ID", "FABRIC8HTTPGATEWAY").toUpperCase() + "_SERVICE_";
        String hostEnvVar = serviceName + HTTPGatewayConfig.HOST;
        String portEnvVar = serviceName + HTTPGatewayConfig.PORT;
        gatewayConfig.put(HTTPGatewayConfig.HOST,
                Systems.getEnvVarOrSystemProperty(hostEnvVar, hostEnvVar, DEFAULT_HOST));
        gatewayConfig.put(HTTPGatewayConfig.PORT,
                Systems.getEnvVarOrSystemProperty(portEnvVar, portEnvVar, DEFAULT_PORT));
        gatewayConfig.put(HTTPGatewayConfig.ENABLE_INDEX,
                Systems.getEnvVarOrSystemProperty(portEnvVar, portEnvVar, DEFAULT_INDEX_ENABLED));
        
        try {
            fabricHTTPGateway.activate(gatewayConfig);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        waitUntilStop();
    }
        
    protected static void waitUntilStop() {
        Object lock = new Object();
        while (true) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }
    
}
