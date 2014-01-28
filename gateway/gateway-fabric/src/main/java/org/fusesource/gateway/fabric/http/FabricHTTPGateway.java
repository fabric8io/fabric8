/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.gateway.fabric.http;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Version;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.support.ConfigInjection;
import io.fabric8.internal.Objects;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.gateway.fabric.FabricGateway;
import org.fusesource.gateway.handlers.http.HttpGateway;
import org.fusesource.gateway.handlers.http.HttpGatewayHandler;
import org.fusesource.gateway.handlers.http.HttpGatewayServer;
import org.fusesource.gateway.handlers.http.HttpMappingRule;
import org.fusesource.gateway.handlers.http.MappedServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * An HTTP gateway which listens on a port and applies a number of {@link HttpMappingRuleConfiguration} instances to bind
 * HTTP requests to different HTTP based services running within the fabric.
 */
@Service(FabricHTTPGateway.class)
@Component(name = "io.fabric8.gateway.http", immediate = true, metatype = true, policy = ConfigurationPolicy.REQUIRE,
        label = "Fabric8 HTTP Gateway",
        description = "Provides a discovery and load balancing HTTP gateway (or reverse proxy) between HTTP clients and HTTP servers such as web applications, REST APIs and web applications")
public class FabricHTTPGateway extends AbstractComponent implements HttpGateway {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricHTTPGateway.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setGateway", unbind = "unsetGateway")
    private FabricGateway gateway;

    @Property(name = "host",
            label = "Host name", description = "The host name used when listening for HTTP traffic")
    private String host;

    @Property(name = "port", intValue = 8080,
            label = "Port", description = "Port number to listen on for HTTP requests")
    private int port = 8080;

    @Property(name = "enableIndex", boolValue = true,
            label = "Enable index page", description = "If enabled then performing a HTTP GET on the path '/' will return a JSON representation of the gateway mappings")
    private boolean enableIndex = true;

    private HttpGatewayServer server;
    private HttpGatewayHandler handler;

    private Set<HttpMappingRule> mappingRuleConfigurations = new CopyOnWriteArraySet<HttpMappingRule>();

    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        updateConfiguration(configuration);
        activateComponent();
    }


    @Modified
    void modified(Map<String, ?> configuration) throws Exception {
        deactivateInternal();
        updateConfiguration(configuration);
    }


    @Deactivate
    void deactivate() {
        deactivateInternal();
        deactivateComponent();
    }

    protected void updateConfiguration(Map<String, ?> configuration) throws Exception {
        ConfigInjection.applyConfiguration(configuration, this);
        Objects.notNull(getGateway(), "gateway");

        Vertx vertx = getVertx();
        handler = new HttpGatewayHandler(vertx, this);
        server = new HttpGatewayServer(vertx, handler, port);
        server.init();
    }

    protected void deactivateInternal() {
        if (server != null) {
            server.destroy();
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
    public Map<String, MappedServices> getMappedServices() {
        Map<String, MappedServices> answer = new HashMap<String, MappedServices>();
        for (HttpMappingRule mappingRuleConfiguration : mappingRuleConfigurations) {
            mappingRuleConfiguration.appendMappedServices(answer);
        }
        return answer;
    }

    // Properties
    //-------------------------------------------------------------------------

    public FabricGateway getGateway() {
        return gateway;
    }

    public void setGateway(FabricGateway gateway) {
        this.gateway = gateway;
    }

    public void unsetGateway(FabricGateway gateway) {
        this.gateway = null;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public boolean isEnableIndex() {
        return enableIndex;
    }

    public void setEnableIndex(boolean enableIndex) {
        this.enableIndex = enableIndex;
    }

    public Vertx getVertx() {
        Objects.notNull(getGateway(), "gateway");
        return gateway.getVertx();
    }

    public CuratorFramework getCurator() {
        Objects.notNull(getGateway(), "gateway");
        return gateway.getCurator();
    }

    /**
     * Returns the default profile version used to filter out the current versions of services
     * if no version expression is used the URI template
     */
    public String getGatewayVersion() {
        FabricService fabricService = gateway.getFabricService();
        if (fabricService != null) {
            Container currentContainer = fabricService.getCurrentContainer();
            if (currentContainer != null) {
                Version version = currentContainer.getVersion();
                if (version != null) {
                    return version.getId();
                }
            }
        }
        return null;
    }
}
