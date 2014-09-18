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
package io.fabric8.gateway.fabric.detecting;

import io.fabric8.api.FabricService;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.common.util.JMXUtils;
import io.fabric8.common.util.ShutdownTracker;
import io.fabric8.common.util.Strings;
import io.fabric8.gateway.ServiceMap;
import io.fabric8.gateway.fabric.http.FabricHTTPGateway;
import io.fabric8.gateway.fabric.support.vertx.VertxService;
import io.fabric8.gateway.handlers.detecting.DetectingGateway;
import io.fabric8.gateway.handlers.detecting.Protocol;
import io.fabric8.gateway.handlers.detecting.protocol.amqp.AmqpProtocol;
import io.fabric8.gateway.handlers.detecting.protocol.http.HttpProtocol;
import io.fabric8.gateway.handlers.detecting.protocol.mqtt.MqttProtocol;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.OpenwireProtocol;
import io.fabric8.gateway.handlers.detecting.protocol.ssl.SslConfig;
import io.fabric8.gateway.handlers.detecting.protocol.ssl.SslProtocol;
import io.fabric8.gateway.handlers.detecting.protocol.stomp.StompProtocol;
import io.fabric8.gateway.loadbalancer.LoadBalancer;
import io.fabric8.gateway.loadbalancer.LoadBalancers;
import io.fabric8.internal.Objects;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.net.URL;
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
    private MBeanServer mbeanServer;

    @Reference
    private Configurer configurer;
    @Reference(referenceInterface = VertxService.class, cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setVertxService", unbind = "unsetVertxService")
    private VertxService vertxService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setFabricService", unbind = "unsetFabricService")
    private FabricService fabricService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY, bind = "setCurator", unbind = "unsetCurator")
    private CuratorFramework curator;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, bind = "setHttpGateway", unbind = "unsetHttpGateway", policy=ReferencePolicy.DYNAMIC)
    private FabricHTTPGateway httpGateway;

    @Property(name = "zooKeeperPath", value = "/fabric/registry/clusters/amq",
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

    @Property(name = "sslEnabled", boolValue = false,
            label = "SSL enabled", description = "Enable or disable the SSL protocol detection")
    private boolean sslEnabled = false;

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

    @Property(name = "sslProtocol", value="TLS",
            label = "SSL Protocol", description = "Example: SSL, TLS, TLSv1, TLSv2 etc.")
    private String sslProtocol;

    @Property(name = "sslStoreType", value="JKS",
            label = "SSL Store Type ", description = "The type of store that the keys stored within")
    private String sslStoreType;

    @Property(name = "sslAlgorithm", value="SunX509",
            label = "SSL Certificate Algorithm", description = "The encryption algorithm of the certificates")
    private String sslAlgorithm;

    @Property(name = "trustStoreURL",
            label = "SSL Trust Store URL", description = "The trust store holds the public certificates of clients that will be trusted to SSL connect to the server.  If not set, the key store will be used.")
    private URL trustStoreURL;

    @Property(name = "trustStorePassword",
            label = "SSL Trust Store Password", description = "The password used to open the trust store")
    private String trustStorePassword;

    @Property(name = "keyStoreURL",
            label = "SSL Key Store URL", description = "The key store holds the private certificate of the server.")
    private URL keyStoreURL;

    @Property(name = "keyStorePassword",
            label = "SSL Key Store Password", description = "The password used to open the key store")
    private String keyStorePassword;

    @Property(name = "keyAlias",
            label = "SSL Private Key Alias", description = "Alias of the private key to use for SSL connections")
    private String keyAlias;
    @Property(name = "keyPassword",
            label = "SSL Private Key Password", description = "The password used to access the SSL private key")
    private String keyPassword;

    @Property(name = "enabledCipherSuites",
            label = "SSL Cipher Suites Enabled", description = "Comma separated list of cipher suites to enable on the SSL sessions.")
    String enabledCipherSuites;
    @Property(name = "disabledCypherSuites",
            label = "SSL Cipher Suites Disabled", description = "Comma separated list of cipher suites to disable on the SSL sessions.")
    String disabledCypherSuites;

    private DetectingGateway detectingGateway;
    private GatewayServiceTreeCache cache;
    private ServiceMap serviceMap = new ServiceMap();
    final private ShutdownTracker shutdownTacker = new ShutdownTracker();

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
        JMXUtils.registerMBean(shutdownTacker.mbeanProxy(detectingGateway), mbeanServer, new ObjectName("io.fabric8.gateway:type=DetectingGateway"));
    }

    @Deactivate
    void deactivate() throws Exception {
        JMXUtils.unregisterMBean(mbeanServer, new ObjectName("io.fabric8.gateway:type=DetectingGateway"));
        deactivateComponent();
        if (detectingGateway != null) {
            cache.destroy();
            detectingGateway.destroy();
        }
        shutdownTacker.stop();
    }

    protected DetectingGateway createDetectingGateway() {
        DetectingGateway gateway = new DetectingGateway();
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
        if( isSslEnabled() ) {
            SslConfig sslConfig = new SslConfig();
            if( Strings.isNotBlank(sslAlgorithm) ) {
                sslConfig.setAlgorithm(sslAlgorithm);
            }
            if( Strings.isNotBlank(keyAlias) ) {
                sslConfig.setKeyAlias(keyAlias);
            }
            if( Strings.isNotBlank(keyPassword) ) {
                sslConfig.setKeyPassword(keyPassword);
            }
            if( Strings.isNotBlank(keyStorePassword) ) {
                sslConfig.setKeyStorePassword(keyStorePassword);
            }
            if( keyStoreURL!=null ) {
                sslConfig.setKeyStoreURL(keyStoreURL);
            }
            if( Strings.isNotBlank(sslProtocol) ) {
                sslConfig.setProtocol(sslProtocol);
            }
            if( Strings.isNotBlank(sslStoreType) ) {
                sslConfig.setStoreType(sslStoreType);
            }
            if( Strings.isNotBlank(trustStorePassword) ) {
                sslConfig.setTrustStorePassword(trustStorePassword);
            }
            if( trustStoreURL != null ) {
                sslConfig.setTrustStoreURL(trustStoreURL);
            }
            if( Strings.isNotBlank(enabledCipherSuites) ) {
                sslConfig.setEnabledCipherSuites(enabledCipherSuites);
            }
            if( Strings.isNotBlank(disabledCypherSuites) ) {
                sslConfig.setDisabledCypherSuites(disabledCypherSuites);
            }
            gateway.setSslConfig(sslConfig);
            protocols.add(new SslProtocol());
        }

        if (protocols.isEmpty()) {
            return null;
        }

        VertxService vertxService = getVertxService();
        LoadBalancer serviceLoadBalancer = LoadBalancers.createLoadBalancer(loadBalancerType, stickyLoadBalancerCacheSize);
        gateway.setVertx(vertxService.getVertx());
        gateway.setPort(port);
        gateway.setServiceMap(serviceMap);
        gateway.setProtocols(protocols);
        gateway.setShutdownTacker(shutdownTacker);
        gateway.setServiceLoadBalancer(serviceLoadBalancer);
        gateway.setDefaultVirtualHost(defaultVirtualHost);
        return gateway;
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

    public void setHttpGateway(FabricHTTPGateway httpGateway) {
        this.httpGateway = httpGateway;
        LOG.info("HTTP Gateway address is: "+httpGateway.getLocalAddress());
        detectingGateway.setHttpGateway(httpGateway.getLocalAddress());
    }
    public void unsetHttpGateway(FabricHTTPGateway httpGateway) {
        this.httpGateway = null;
        detectingGateway.setHttpGateway(null);
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public void setSslAlgorithm(String sslAlgorithm) {
        this.sslAlgorithm = sslAlgorithm;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public void setKeyStoreURL(URL keyStoreURL) {
        this.keyStoreURL = keyStoreURL;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    public void setSslStoreType(String sslStoreType) {
        this.sslStoreType = sslStoreType;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public void setTrustStoreURL(URL trustStoreURL) {
        this.trustStoreURL = trustStoreURL;
    }

    public String getEnabledCipherSuites() {
        return enabledCipherSuites;
    }

    public void setEnabledCipherSuites(String enabledCipherSuites) {
        this.enabledCipherSuites = enabledCipherSuites;
    }

    public String getDisabledCypherSuites() {
        return disabledCypherSuites;
    }

    public void setDisabledCypherSuites(String disabledCypherSuites) {
        this.disabledCypherSuites = disabledCypherSuites;
    }

    void bindMbeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    void unbindMbeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = null;
    }

    public DetectingGateway getDetectingGateway() {
        return detectingGateway;
    }
}
