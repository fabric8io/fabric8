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

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Version;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.fabric8.gateway.fabric.detecting.FabricDetectingGatewayService;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.gateway.fabric.support.vertx.VertxService;
import io.fabric8.gateway.handlers.detecting.DetectingGatewayWebSocketHandler;
import io.fabric8.gateway.handlers.http.HttpGateway;
import io.fabric8.gateway.handlers.http.HttpGatewayHandler;
import io.fabric8.gateway.handlers.http.HttpGatewayServer;
import io.fabric8.gateway.handlers.http.HttpMappingRule;
import io.fabric8.gateway.handlers.http.MappedServices;
import org.vertx.java.core.Vertx;

/**
 * An HTTP gateway which listens on a port and applies a number of {@link HttpMappingRuleConfiguration} instances to bind
 * HTTP requests to different HTTP based services running within the fabric.
 */
@Component(name = "io.fabric8.gateway.http", immediate = true, metatype = true, policy = ConfigurationPolicy.REQUIRE,
        label = "Fabric8 HTTP Gateway",
        description = "Provides a discovery and load balancing HTTP gateway (or reverse proxy) between HTTP clients and HTTP servers such as web applications, REST APIs and web applications")
@Service(FabricHTTPGateway.class)
public final class FabricHTTPGateway extends AbstractComponent implements HttpGateway {

    @Property(name = "host", label = "Host name", description = "The host name used when listening for HTTP traffic")
    private String host;

    @Property(name = "port", intValue = 8080, label = "Port", description = "Port number to listen on for HTTP requests")
    private int port = 8080;

    @Property(name = "enableIndex", boolValue = true, label = "Enable index page", description = "If enabled then performing a HTTP GET on the path '/' will return a JSON representation of the gateway mappings")
    private boolean enableIndex = true;

    @Property(name = "enableWebSocketGateway", boolValue = true, label = "Enable the Web Socket Gateway", description = "If enabled then Web Socket connections will be handled by protocol detecting gateway")
    private boolean enableWebSocketGateway = true;

    @Property(name = "websocketGatewayPrefix", label = "Web Socket Path Prefix", description = "The prefix a websocket requests must have")
    private String websocketGatewayPrefix = "";

    @Reference
    private Configurer configurer;

    @Reference(referenceInterface = VertxService.class)
    private final ValidatingReference<VertxService> vertxService = new ValidatingReference<VertxService>();
    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();
    @Reference(referenceInterface = FabricDetectingGatewayService.class, cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy= ReferencePolicy.DYNAMIC)
    private final ValidatingReference<FabricDetectingGatewayService> fabricDetectingGatewayService = new ValidatingReference<FabricDetectingGatewayService>();

    private HttpGatewayServer server;
    private HttpGatewayHandler handler;
    private DetectingGatewayWebSocketHandler websocketHandler = new DetectingGatewayWebSocketHandler();

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

    private void updateConfiguration(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);

        Vertx vertx = getVertx();
        handler = new HttpGatewayHandler(vertx, this);
        websocketHandler.setPathPrefix(websocketGatewayPrefix);
        server = new HttpGatewayServer(vertx, handler, enableWebSocketGateway ? websocketHandler : null, port);
        server.init();
    }

    private void deactivateInternal() {
        if (server != null) {
            server.destroy();
        }
    }

    @Override
    public void addMappingRuleConfiguration(HttpMappingRule mappingRuleConfiguration) {
        assertValid();
        mappingRuleConfigurations.add(mappingRuleConfiguration);
    }

    @Override
    public void removeMappingRuleConfiguration(HttpMappingRule mappingRuleConfiguration) {
        assertValid();
        mappingRuleConfigurations.remove(mappingRuleConfiguration);
    }

    @Override
    public Map<String, MappedServices> getMappedServices() {
        assertValid();
        Map<String, MappedServices> answer = new HashMap<String, MappedServices>();
        for (HttpMappingRule mappingRuleConfiguration : mappingRuleConfigurations) {
            mappingRuleConfiguration.appendMappedServices(answer);
        }
        return answer;
    }

    @Override
    public boolean isEnableIndex() {
        return enableIndex;
    }

    /**
     * Returns address the gateway service is listening on.
     */
    public InetSocketAddress getLocalAddress() {
        return new InetSocketAddress(host==null?"0.0.0.0":host, port);
    }

    private Vertx getVertx() {
        return vertxService.get().getVertx();
    }

    /**
     * Returns the default profile version used to filter out the current versions of services
     * if no version expression is used the URI template
     */
    String getGatewayVersion() {
        assertValid();
        Container currentContainer = fabricService.get().getCurrentContainer();
        if (currentContainer != null) {
            Version version = currentContainer.getVersion();
            if (version != null) {
                return version.getId();
            }
        }
        return null;
    }

    int getPort() {
        return port;
    }

    void bindVertxService(VertxService vertxService) {
        this.vertxService.bind(vertxService);
    }

    void unbindVertxService(VertxService vertxService) {
        this.vertxService.unbind(vertxService);
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindFabricDetectingGatewayService(FabricDetectingGatewayService fabricDetectingGatewayService) {
        this.fabricDetectingGatewayService.bind(fabricDetectingGatewayService);
        websocketHandler.setGateway(fabricDetectingGatewayService.getDetectingGateway());
    }

    void unbindFabricDetectingGatewayService(FabricDetectingGatewayService fabricDetectingGatewayService) {
        this.fabricDetectingGatewayService.unbind(fabricDetectingGatewayService);
        websocketHandler.setGateway(null);
    }
}
