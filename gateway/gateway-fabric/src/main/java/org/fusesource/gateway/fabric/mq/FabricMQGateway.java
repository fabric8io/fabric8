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
package org.fusesource.gateway.fabric.mq;

import io.fabric8.api.FabricService;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.internal.Objects;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.fusesource.common.util.Strings;
import org.fusesource.gateway.ServiceDetails;
import org.fusesource.gateway.ServiceMap;
import org.fusesource.gateway.fabric.support.vertx.VertxService;
import org.fusesource.gateway.handlers.tcp.TcpGateway;
import org.fusesource.gateway.handlers.tcp.TcpGatewayHandler;
import org.fusesource.gateway.loadbalancer.LoadBalancer;
import org.fusesource.gateway.loadbalancer.LoadBalancers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An MQ gateway which listens to a part of the ZooKeeper tree for messaging services and exposes those over protocol specific ports.
 */
@Component(name = "io.fabric8.gateway.mq", immediate = true, metatype = true, policy = ConfigurationPolicy.REQUIRE,
        label = "Fabric8 MQ Gateway",
        description = "Provides a discovery and load balancing gateway between clients using various messaging protocols and the available message brokers in the fabric")
public class FabricMQGateway extends AbstractComponent {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricMQGateway.class);

    @Reference
    private Configurer configurer;
    @Reference(referenceInterface = VertxService.class, cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setVertxService", unbind = "unsetVertxService")
    private VertxService vertxService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setFabricService", unbind = "unsetFabricService")
    private FabricService fabricService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setCurator", unbind = "unsetCurator")
    private CuratorFramework curator;

    @Property(name = "zooKeeperPath", value = "/fabric/registry/clusters/fusemq",
            label = "ZooKeeper path", description = "The path in ZooKeeper which is monitored to discover the available message brokers")
    private String zooKeeperPath;

    @Property(name = "host",
            label = "Host name", description = "The host name used when listening on the various messaging ports")
    private String host;

    @Property(name = "openWireEnabled", boolValue = true,
            label = "OpenWire enabled", description = "Enable or disable the OpenWire transport protocol")
    private boolean openWireEnabled = true;
    @Property(name = "openWirePort", intValue = 61616,
            label = "OpenWire port", description = "Port number to listen on for OpenWire")
    private int openWirePort = 61616;

    @Property(name = "stompEnabled", boolValue = true,
            label = "STOMP enabled", description = "Enable or disable the STOMP transport protocol")
    private boolean stompEnabled = true;
    @Property(name = "stompPort", intValue = 61613,
            label = "STOMP port", description = "Port number to listen on for STOMP")
    private int stompPort = 61613;

    @Property(name = "amqpEnabled", boolValue = true,
            label = "AMQP enabled", description = "Enable or disable the AMQP transport protocol")
    private boolean amqpEnabled = true;
    @Property(name = "amqpPort", intValue = 5672,
            label = "AMQP port", description = "Port number to listen on for AMQP")
    private int amqpPort = 5672;

    @Property(name = "mqttEnabled", boolValue = true,
            label = "MQTT enabled", description = "Enable or disable the MQTT transport protocol")
    private boolean mqttEnabled = true;
    @Property(name = "mqttPort", intValue = 5672,
            label = "MQTT port", description = "Port number to listen on for MQTT")
    private int mqttPort = 5672;

    @Property(name = "websocketEnabled", boolValue = true,
            label = "WebSocket enabled", description = "Enable or disable the WebSocket transport protocol")
    private boolean websocketEnabled = true;
    @Property(name = "websocketPort", intValue = 61614,
            label = "WebSocket port", description = "Port number to listen on for WebSocket")
    private int websocketPort = 61614;

    @Property(name = "loadBalancerType",
            value = LoadBalancers.ROUND_ROBIN_LOAD_BALANCER,
            options = {
                    @PropertyOption(name = LoadBalancers.RANDOM_LOAD_BALANCER, value = "Random"),
                    @PropertyOption(name = LoadBalancers.ROUND_ROBIN_LOAD_BALANCER, value = "Round Robin"),
                    @PropertyOption(name = LoadBalancers.STICKY_LOAD_BALANCER, value = "Sticky")
            },
            label = "Load Balancer", description = "The kind of load balancing strategy used")
    private String loadBalancerType;

    @Property(name = "stickyLoadBalancerCacheSize", intValue = LoadBalancers.STICKY_LOAD_BALANCER_DEFAULT_CACHE_SIZE,
            label = "Sticky Load Balancer Cache Size", description = "The number of unique client keys to cache for the sticky load balancer (using an LRU caching algorithm)")
    private int stickyLoadBalancerCacheSize = LoadBalancers.STICKY_LOAD_BALANCER_DEFAULT_CACHE_SIZE;

    private GatewayServiceTreeCache gatewayServiceTreeCache;

    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);
        Objects.notNull(getVertxService(), "vertxService");
        Objects.notNull(getZooKeeperPath(), "zooKeeperPath");
        activateComponent();

        gatewayServiceTreeCache = createListener();
        if (gatewayServiceTreeCache != null) {
            gatewayServiceTreeCache.init();
        }
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        if (gatewayServiceTreeCache != null) {
            gatewayServiceTreeCache.destroy();
        }
    }

    protected GatewayServiceTreeCache createListener() {
        String zkPath = getZooKeeperPath();

        // TODO we should discover the broker group configuration here using the same
        // mq-create / mq-client profiles so that we only listen to a subset of the available brokers here?

        ServiceMap serviceMap = new ServiceMap();

        VertxService vertxService = getVertxService();
        Vertx vertx = vertxService.getVertx();
        CuratorFramework curator = getCurator();

        LoadBalancer<String> pathLoadBalancer = LoadBalancers.createLoadBalancer(loadBalancerType, stickyLoadBalancerCacheSize);
        LoadBalancer<ServiceDetails> serviceLoadBalancer = LoadBalancers.createLoadBalancer(loadBalancerType, stickyLoadBalancerCacheSize);

        LOG.info("activating MQ mapping ZooKeeper path: " + zkPath + " host: " + host
                + " with load balancer: " + pathLoadBalancer);


        List<TcpGateway> gateways = new ArrayList<TcpGateway>();
        addGateway(gateways, vertx, serviceMap, "tcp", isOpenWireEnabled(), getOpenWirePort(), pathLoadBalancer, serviceLoadBalancer);
        addGateway(gateways, vertx, serviceMap, "stomp", isStompEnabled(), getStompPort(), pathLoadBalancer, serviceLoadBalancer);
        addGateway(gateways, vertx, serviceMap, "amqp", isAmqpEnabled(), getAmqpPort(), pathLoadBalancer, serviceLoadBalancer);
        addGateway(gateways, vertx, serviceMap, "mqtt", isMqttEnabled(), getMqttPort(), pathLoadBalancer, serviceLoadBalancer);
        addGateway(gateways, vertx, serviceMap, "ws", isWebsocketEnabled(), getWebsocketPort(), pathLoadBalancer, serviceLoadBalancer);

        if (gateways.isEmpty()) {
            return null;
        }
        return new GatewayServiceTreeCache(curator, zkPath, serviceMap, gateways);
    }

    protected TcpGateway addGateway(List<TcpGateway> gateways, Vertx vertx, ServiceMap serviceMap, String protocolName, boolean enabled, int listenPort, LoadBalancer pathLoadBalancer, LoadBalancer<ServiceDetails> serviceLoadBalancer) {
        if (enabled) {
            TcpGatewayHandler handler = new TcpGatewayHandler(vertx, serviceMap, protocolName, pathLoadBalancer, serviceLoadBalancer);
            TcpGateway gateway = new TcpGateway(vertx, serviceMap, listenPort, protocolName, handler);
            if (Strings.isNotBlank(host)) {
                gateway.setHost(host);
            }
            gateways.add(gateway);
            return gateway;
        } else {
            return null;
        }
    }

    // Properties
    //-------------------------------------------------------------------------

    public VertxService getVertxService() {
        return vertxService;
    }

    public void setVertxService(VertxService vertxService) {
        this.vertxService = vertxService;
    }

    public void unsetVertxService(VertxService vertxService) {
        this.vertxService = null;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public void unsetCurator(CuratorFramework curator) {
        this.curator = null;
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public void unsetFabricService(FabricService fabricService) {
        this.fabricService = null;
    }

    public String getZooKeeperPath() {
        return zooKeeperPath;
    }

    public void setZooKeeperPath(String zooKeeperPath) {
        this.zooKeeperPath = zooKeeperPath;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isOpenWireEnabled() {
        return openWireEnabled;
    }

    public void setOpenWireEnabled(boolean openWireEnabled) {
        this.openWireEnabled = openWireEnabled;
    }

    public int getOpenWirePort() {
        return openWirePort;
    }

    public void setOpenWirePort(int openWirePort) {
        this.openWirePort = openWirePort;
    }

    public boolean isStompEnabled() {
        return stompEnabled;
    }

    public void setStompEnabled(boolean stompEnabled) {
        this.stompEnabled = stompEnabled;
    }

    public int getStompPort() {
        return stompPort;
    }

    public void setStompPort(int stompPort) {
        this.stompPort = stompPort;
    }

    public boolean isAmqpEnabled() {
        return amqpEnabled;
    }

    public void setAmqpEnabled(boolean amqpEnabled) {
        this.amqpEnabled = amqpEnabled;
    }

    public int getAmqpPort() {
        return amqpPort;
    }

    public void setAmqpPort(int amqpPort) {
        this.amqpPort = amqpPort;
    }

    public boolean isMqttEnabled() {
        return mqttEnabled;
    }

    public void setMqttEnabled(boolean mqttEnabled) {
        this.mqttEnabled = mqttEnabled;
    }

    public int getMqttPort() {
        return mqttPort;
    }

    public void setMqttPort(int mqttPort) {
        this.mqttPort = mqttPort;
    }

    public boolean isWebsocketEnabled() {
        return websocketEnabled;
    }

    public void setWebsocketEnabled(boolean websocketEnabled) {
        this.websocketEnabled = websocketEnabled;
    }

    public int getWebsocketPort() {
        return websocketPort;
    }

    public void setWebsocketPort(int websocketPort) {
        this.websocketPort = websocketPort;
    }
}
