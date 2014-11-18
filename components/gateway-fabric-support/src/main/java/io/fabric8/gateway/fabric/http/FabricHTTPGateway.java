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
package io.fabric8.gateway.fabric.http;


import io.fabric8.gateway.api.CallDetailRecord;
import io.fabric8.gateway.api.apimanager.ApiManager;
import io.fabric8.gateway.api.handlers.http.HttpGateway;
import io.fabric8.gateway.api.handlers.http.HttpGatewayHandler;
import io.fabric8.gateway.api.handlers.http.HttpMappingRule;
import io.fabric8.gateway.api.handlers.http.IMappedServices;
import io.fabric8.gateway.fabric.support.vertx.VertxService;
import io.fabric8.gateway.handlers.http.HttpGatewayServer;
import io.fabric8.utils.ShutdownTracker;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.management.MBeanServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

/**
 * An HTTP gateway which listens on a port and applies a number of {@link HttpMappingRuleConfiguration} instances to bind
 * HTTP requests to different HTTP based services running within the fabric.
 */
@ApplicationScoped
public class FabricHTTPGateway implements HttpGateway {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricHTTPGateway.class);
        
    @Inject VertxService vertxService;
    
    HTTPGatewayConfig gatewayConfig;
    
    private HttpGatewayServer server;
    private HttpGatewayHandler handler;
    //private DetectingGatewayWebSocketHandler websocketHandler = new DetectingGatewayWebSocketHandler();
    private MBeanServer mbeanServer;
    private Set<HttpMappingRule> mappingRuleConfigurations = new CopyOnWriteArraySet<HttpMappingRule>();

    ShutdownTracker shutdownTracker = new ShutdownTracker();
    private FabricHTTPGatewayInfo fabricHTTPGatewayInfoMBean;
    
    public void configure(HTTPGatewayConfig httpGatewayConfig) throws Exception {
    	LOG.info("configuring the HTTP gateway");
        gatewayConfig = httpGatewayConfig;
        updateConfiguration();
        mbeanServer = ManagementFactory.getPlatformMBeanServer();
        registerHttpGatewayMBeans();
    }

    @PreDestroy
    void deactivate() {
        deactivateInternal();
        unregisterHttpGatewayMBeans();
        gatewayConfig = null;
    }

    private void updateConfiguration() throws Exception {
        Vertx vertx = getVertx();
        handler = new HttpGatewayHandler(vertx, this);
        //websocketHandler.setPathPrefix(websocketGatewayPrefix);
        server = new HttpGatewayServer(vertx, null, gatewayConfig.getPort(), handler);
        server.init();
    }

    private void deactivateInternal() {
        if (server != null) {
            server.destroy();
        }
    }
    
    @Override
    public void addCallDetailRecord(CallDetailRecord cdr) {
    	fabricHTTPGatewayInfoMBean.setLastCallDate(cdr.getCallDate().toString());
    	fabricHTTPGatewayInfoMBean.registerCall(cdr.getCallTimeNanos());
    	if (cdr.getError()!=null) {
    		fabricHTTPGatewayInfoMBean.setLastError(cdr.getError());
    	}
    }

    @Override
    public void addMappingRuleConfiguration(HttpMappingRule mappingRuleConfiguration) {
        mappingRuleConfigurations.add(mappingRuleConfiguration);
    }

    @Override
    public void removeMappingRuleConfiguration(HttpMappingRule mappingRuleConfiguration) {
        mappingRuleConfigurations.remove(mappingRuleConfiguration);
    }

    @Override
    public Map<String, IMappedServices> getMappedServices() {
        Map<String, IMappedServices> answer = new HashMap<String, IMappedServices>();
        for (HttpMappingRule mappingRuleConfiguration : mappingRuleConfigurations) {
            mappingRuleConfiguration.appendMappedServices(answer);
        }
        return answer;
    }

    @Override
    public boolean isEnableIndex() {
        return gatewayConfig.isIndexEnabled();
    }

    /**
     * Returns address the gateway service is listening on.
     */
    public InetSocketAddress getLocalAddress() {
        return new InetSocketAddress(gatewayConfig.getHost()==null?"0.0.0.0":gatewayConfig.getHost(), gatewayConfig.getPort());
    }

    private Vertx getVertx() {
        return vertxService.getVertx();
    }

    String getGatewayVersion() {
        return "1.0";
    }
//TODO Kurt - where is the version now coming from?
//    /**
//     * Returns the default profile version used to filter out the current versions of services
//     * if no version expression is used the URI template
//     */
//    String getGatewayVersion() {
//        Container currentContainer = fabricService.get().getCurrentContainer();
//        if (currentContainer != null) {
//            Version version = currentContainer.getVersion();
//            if (version != null) {
//                return version.getId();
//            }
//        }
//        return null;
//    }

    int getPort() {
        return gatewayConfig.getPort();
    }
    
    String getHost() {
    	return gatewayConfig.getHost();
    }
    
    private void registerHttpGatewayMBeans() {
    	fabricHTTPGatewayInfoMBean = new FabricHTTPGatewayInfo(this);
        fabricHTTPGatewayInfoMBean.registerMBeanServer(shutdownTracker, mbeanServer);
    }
    
    private void unregisterHttpGatewayMBeans() {
        fabricHTTPGatewayInfoMBean.unregisterMBeanServer(mbeanServer);
    }

	@Override
	public ApiManager getApiManager() {
		// TODO Auto-generated method stub
		return null;
	}

	

}
