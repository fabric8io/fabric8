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
package org.fusesource.gateway.fabric.detecting;

import io.fabric8.api.FabricService;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.internal.Objects;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.*;
import org.fusesource.gateway.ServiceDetails;
import org.fusesource.gateway.ServiceMap;
import org.fusesource.gateway.fabric.support.vertx.VertxService;
import org.fusesource.gateway.handlers.detecting.DetectingGateway;
import org.fusesource.gateway.handlers.detecting.DetectingGatewayProtocolHandler;
import org.fusesource.gateway.handlers.detecting.Protocol;
import org.fusesource.gateway.handlers.detecting.protocol.amqp.AmqpProtocol;
import org.fusesource.gateway.handlers.detecting.protocol.http.HttpProtocol;
import org.fusesource.gateway.handlers.detecting.protocol.mqtt.MqttProtocol;
import org.fusesource.gateway.handlers.detecting.protocol.openwire.OpenwireProtocol;
import org.fusesource.gateway.handlers.detecting.protocol.stomp.StompProtocol;
import org.fusesource.gateway.handlers.http.HttpGateway;
import org.fusesource.gateway.loadbalancer.LoadBalancer;
import org.fusesource.gateway.loadbalancer.LoadBalancers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

import java.util.ArrayList;
import java.util.Map;

/**
 * A gateway which listens to a part of the ZooKeeper tree for messaging services and exposes those over a protocol detecting port.
 */
@Component(name = "io.fabric8.gateway.detecting", immediate = true, metatype = true, policy = ConfigurationPolicy.REQUIRE,
        label = "Fabric8 Detecting Gateway",
        description = "Provides a discovery and load balancing gateway between clients using various messaging protocols and the available message brokers in the fabric")
public class FabricDetectingGateway extends AbstractComponent implements FabricDetectingGatewayService {
    private static final transient Logger LOG = LoggerFactory.getLogger(FabricDetectingGateway.class);

    @Reference
    private Configurer configurer;
    @Reference(referenceInterface = VertxService.class, cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setVertxService", unbind = "unsetVertxService")
    private VertxService vertxService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setFabricService", unbind = "unsetFabricService")
    private FabricService fabricService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setCurator", unbind = "unsetCurator")
    private CuratorFramework curator;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, bind = "setHttpGateway", unbind = "unsetHttpGateway")
    private HttpGateway httpGateway;

    @Property(name = "zooKeeperPath", value = "/fabric/registry/clusters/fusemq",
            label = "ZooKeeper path", description = "The path in ZooKeeper which is monitored to discover the available message brokers")
    private String zooKeeperPath;

    @Property(name = "address",
            label = "Bind Address", description = "The IP address or host name to bind when to listen for new connections")
    private String address;

    @Property(name = "defaultVirtualHost",
            label = "Default Virtual Host", description = "The virtual host name to assume a client is trying to connect to when the client protocol does not specify a valid virtual hostname.")
    private String defaultVirtualHost;

    @Property(name = "port", intValue = 61616, label = "Bind Port", description = "The IP port to bind when to listen for new connections")
    private int port = 61616;

    @Property(name = "openWireEnabled", boolValue = true,
            label = "OpenWire enabled", description = "Enable or disable the OpenWire transport protocol")
    private boolean openWireEnabled = true;

    @Property(name = "stompEnabled", boolValue = true,
            label = "STOMP enabled", description = "Enable or disable the STOMP transport protocol")
    private boolean stompEnabled = true;

    @Property(name = "amqpEnabled", boolValue = true,
            label = "AMQP enabled", description = "Enable or disable the AMQP transport protocol")
    private boolean amqpEnabled = true;

    @Property(name = "mqttEnabled", boolValue = true,
            label = "MQTT enabled", description = "Enable or disable the MQTT transport protocol")
    private boolean mqttEnabled = true;

    @Property(name = "httpEnabled", boolValue = true,
            label = "HTTP enabled", description = "Enable or disable the HTTP protocol detection")
    private boolean httpEnabled = true;

    @Property(name = "loadBalancerType",
            value = LoadBalancers.ROUND_ROBIN_LOAD_BALANCER,
            options = {
                    @PropertyOption(name = LoadBalancers.RANDOM_LOAD_BALANCER, value = "Random"),
                    @PropertyOption(name = LoadBalancers.ROUND_ROBIN_LOAD_BALANCER, value = "Round Robin"),
                    @PropertyOption(name = LoadBalancers.STICKY_LOAD_BALANCER, value = "Sticky")
            },
            label = "Load Balancer", description = "The kind of load balancing strategy to use when multiple endpoints can service the client conneciton")
    private String loadBalancerType;

    @Property(name = "stickyLoadBalancerCacheSize", intValue = LoadBalancers.STICKY_LOAD_BALANCER_DEFAULT_CACHE_SIZE,
            label = "Sticky Load Balancer Cache Size", description = "The number of unique client keys to cache for the sticky load balancer (using an LRU caching algorithm)")
    private int stickyLoadBalancerCacheSize = LoadBalancers.STICKY_LOAD_BALANCER_DEFAULT_CACHE_SIZE;

    private DetectingGateway detectingGateway;
    DetectingGatewayProtocolHandler handler = new DetectingGatewayProtocolHandler();
    private GatewayServiceTreeCache cache;
    private ServiceMap serviceMap = new ServiceMap();


    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);
        Objects.notNull(getVertxService(), "vertxService");
        Objects.notNull(getZooKeeperPath(), "zooKeeperPath");
        activateComponent();

        detectingGateway = createDetectingGateway();
        if( detectingGateway!=null ) {
            cache = new GatewayServiceTreeCache(getCurator(), getZooKeeperPath(), serviceMap);
            cache.init();
            detectingGateway.init();
        }
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        if (detectingGateway != null) {
            cache.destroy();
            detectingGateway.destroy();
        }
    }

    protected DetectingGateway createDetectingGateway() {
        ArrayList<Protocol> protocols = new ArrayList<Protocol>();
        if( isStompEnabled() ) {
            protocols.add(new StompProtocol());
        }
        if( isMqttEnabled() ) {
            protocols.add(new MqttProtocol());
        }
        if( isAmqpEnabled() ) {
            protocols.add(new AmqpProtocol());
        }
        if( isOpenWireEnabled() ) {
            protocols.add(new OpenwireProtocol());
        }
        if( isHttpEnabled() ) {
            protocols.add(new HttpProtocol());
        }

        if (protocols.isEmpty()) {
            return null;
        }

        VertxService vertxService = getVertxService();
        Vertx vertx = vertxService.getVertx();
        LoadBalancer<ServiceDetails> serviceLoadBalancer = LoadBalancers.createLoadBalancer(loadBalancerType, stickyLoadBalancerCacheSize);
        handler.setVertx(vertx);
        handler.setServiceMap(serviceMap);
        handler.setProtocols(protocols);
        handler.setServiceLoadBalancer(serviceLoadBalancer);
        handler.setDefaultVirtualHost(defaultVirtualHost);
        return new DetectingGateway(vertx, 0, handler);
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

    public boolean isOpenWireEnabled() {
        return openWireEnabled;
    }

    public void setOpenWireEnabled(boolean openWireEnabled) {
        this.openWireEnabled = openWireEnabled;
    }

    public boolean isStompEnabled() {
        return stompEnabled;
    }

    public void setStompEnabled(boolean stompEnabled) {
        this.stompEnabled = stompEnabled;
    }

    public boolean isAmqpEnabled() {
        return amqpEnabled;
    }

    public void setAmqpEnabled(boolean amqpEnabled) {
        this.amqpEnabled = amqpEnabled;
    }

    public boolean isMqttEnabled() {
        return mqttEnabled;
    }

    public void setMqttEnabled(boolean mqttEnabled) {
        this.mqttEnabled = mqttEnabled;
    }

    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    public void setHttpEnabled(boolean httpEnabled) {
        this.httpEnabled = httpEnabled;
    }

    public String getDefaultVirtualHost() {
        return defaultVirtualHost;
    }

    public void setDefaultVirtualHost(String defaultVirtualHost) {
        this.defaultVirtualHost = defaultVirtualHost;
    }

    @Override
    public DetectingGatewayProtocolHandler getDetectingGatewayProtocolHandler() {
        return handler;
    }

    public void setHttpGateway(HttpGateway httpGateway) {
        this.httpGateway = httpGateway;
        handler.setHttpGateway(httpGateway.getLocalAddress());
    }
    public void unsetHttpGateway(HttpGateway httpGateway) {
        this.httpGateway = null;
        handler.setHttpGateway(null);
    }

}
