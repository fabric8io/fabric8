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
package io.fabric8.gateway.handlers.detecting;


import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.apollo.broker.Broker;
import org.apache.activemq.apollo.dto.AcceptingConnectorDTO;
import org.apache.activemq.apollo.dto.BrokerDTO;
import org.apache.activemq.apollo.dto.VirtualHostDTO;
import org.apache.activemq.apollo.util.ServiceControl;
import org.apache.qpid.amqp_1_0.jms.impl.ConnectionFactoryImpl;
import io.fabric8.gateway.handlers.detecting.protocol.http.HttpProtocol;
import io.fabric8.gateway.handlers.detecting.protocol.openwire.OpenwireProtocol;
import io.fabric8.gateway.handlers.detecting.protocol.stomp.StompProtocol;
import io.fabric8.gateway.loadbalancer.LoadBalancer;
import io.fabric8.gateway.ServiceDTO;
import io.fabric8.gateway.ServiceDetails;
import io.fabric8.gateway.ServiceMap;
import io.fabric8.gateway.handlers.detecting.protocol.amqp.AmqpProtocol;
import io.fabric8.gateway.handlers.detecting.protocol.mqtt.MqttProtocol;
import io.fabric8.gateway.handlers.detecting.protocol.ssl.SslConfig;
import io.fabric8.gateway.handlers.detecting.protocol.ssl.SslProtocol;
import io.fabric8.gateway.loadbalancer.LoadBalancers;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.stomp.client.Stomp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;

import javax.jms.Connection;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 */
public class DetectingGatewayTest {

    private static final transient Logger LOG = LoggerFactory.getLogger(DetectingGatewayTest.class);
    ServiceMap serviceMap = new ServiceMap();

    // Setup Vertx
    protected Vertx vertx;
    @Before
    public void startVertx() {
        if( vertx == null ) {
            vertx = VertxFactory.newVertx();
        }
    }
    @After
    public void stopVertx(){
        if( vertx!=null ) {
            vertx.stop();
            vertx = null;
        }
    }

    // Setup some brokers.
    final protected ArrayList<Broker> brokers = new ArrayList<Broker>();

    @Before
    public void startBrokers() {
        for(int i=0; i < 2; i++) {

            // create a broker..
            String name = "broker" + i;
            Broker broker = createBroker(name);
            ServiceControl.start(broker);
            brokers.add(broker);

            // Add a service map entry for the broker.
            ServiceDTO details = new ServiceDTO();
            details.setId(name);
            details.setVersion("1.0");
            details.setContainer("testing");
            details.setBundleName("none");
            details.setBundleVersion("1.0");
            List<String> services = Arrays.asList(
                "stomp://localhost:" + portOfBroker(i),
                "mqtt://localhost:" + portOfBroker(i),
                "amqp://localhost:" + portOfBroker(i),
                "tcp://localhost:" + portOfBroker(i)
            );
            details.setServices(services);
            serviceMap.serviceUpdated(name, details);

            println(String.format("Broker %s is exposing: %s", name, services));
        }
    }

    @After
    public void stopBrokers() {
        for (Broker broker : brokers) {
            ServiceControl.stop(broker);
        }
        brokers.clear();
    }

    int portOfBroker(int broker) {
        return ((InetSocketAddress)brokers.get(broker).get_socket_address()).getPort();
    }

    public Broker createBroker(String hostname) {
        Broker broker = new Broker();
        BrokerDTO config = broker.config();

        // Configure the virtual host..
        VirtualHostDTO virtualHost = new VirtualHostDTO();
        virtualHost.id = hostname;
        virtualHost.host_names.add(hostname);
        config.virtual_hosts.add(virtualHost);

        // Configure the connectors
        AcceptingConnectorDTO connector = new AcceptingConnectorDTO();
        connector.connection_limit = 100;
        connector.bind = "tcp://0.0.0.0:0";
        config.connectors.clear();
        config.connectors.add(connector);

        return broker;
    }

    protected void println(Object msg) {
        LOG.info(msg.toString());
    }

    @Test
    public void canDetectTheStompProtocol() throws Exception {
        DetectingGateway gateway = createGateway();
        gateway.init();

        // Lets establish a connection....
        Stomp stomp = new Stomp("localhost", gateway.getBoundPort());
        stomp.setHost("broker0"); // lets connect to the broker0 virtual host..
        org.fusesource.stomp.client.BlockingConnection connection = stomp.connectBlocking();

        assertConnectedToBroker(0);
        connection.close();
    }

    @Test// (timeout=60 * 1000)
    public void canDetectTheMQTTProtocol() throws Exception {

        DetectingGateway gateway = createGateway();
        gateway.init();

        // Lets establish a connection....
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", gateway.getBoundPort());
        mqtt.setClientId("myclientid");
//        mqtt.setVersion("3.1.1");
        mqtt.setUserName("broker0/chirino");
        mqtt.setConnectAttemptsMax(1);

        org.fusesource.mqtt.client.BlockingConnection connection = mqtt.blockingConnection();
        connection.connect();

        assertConnectedToBroker(0);
        connection.kill();
    }

    void assertConnectedToBroker(int broker) {
        for( int i = 0; i < brokers.size(); i++) {
            if( i==broker ) {
                assertEquals(1, getConnectionsOnBroker(i));
            } else {
                assertEquals(0, getConnectionsOnBroker(i));
            }
        }
    }

    // This test is not yet ready for prime time..
    @Test
    public void canDetectTheAMQPProtocol() throws Exception {
        DetectingGateway gateway = createGateway();
        gateway.init();
        final ConnectionFactoryImpl factory = new ConnectionFactoryImpl("localhost", gateway.getBoundPort(), "admin", "password");
        Connection connection = factory.createConnection();
        connection.start();

        // We can't get the virtual host from AMQP connections yet, so the connection
        // Should get routed to the default virtual host which is broker 1.
        assertConnectedToBroker(1);
        connection.close();
    }

    @Test
    public void canDetectTheOpenwireProtocol() throws Exception {

        DetectingGateway gateway = createGateway();

        gateway.init();
        String url = "tcp://localhost:" + gateway.getBoundPort()+"?wireFormat.host=broker0";
        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(url);
        Connection connection = factory.createConnection();
        connection.start();

        assertConnectedToBroker(0);
        connection.close();
    }

    @Test
    public void canDetectTheOpenwireSslProtocol() throws Exception {

        System.setProperty("javax.net.ssl.trustStore", new File(basedir(), "src/test/resources/client.ks").getCanonicalPath());
        System.setProperty("javax.net.ssl.trustStorePassword", "password");
        System.setProperty("javax.net.ssl.trustStoreType", "jks");

        DetectingGateway gateway = createGateway();

        gateway.init();
        String url = "ssl://localhost:" + gateway.getBoundPort()+"?wireFormat.host=broker0";
        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(url);
        Connection connection = factory.createConnection();
        connection.start();

        assertConnectedToBroker(0);
        connection.close();
    }


    private int getConnectionsOnBroker(int brokerIdx) {
        return brokers.get(brokerIdx).connections().size();
    }

    public DetectingGateway createGateway() {
        String loadBalancerType = LoadBalancers.STICKY_LOAD_BALANCER;
        int stickyLoadBalancerCacheSize = LoadBalancers.STICKY_LOAD_BALANCER_DEFAULT_CACHE_SIZE;

        LoadBalancer<ServiceDetails> serviceLoadBalancer = LoadBalancers.createLoadBalancer(loadBalancerType, stickyLoadBalancerCacheSize);

        ArrayList<Protocol> protocols = new ArrayList<Protocol>();
        protocols.add(new StompProtocol());
        protocols.add(new MqttProtocol());
        protocols.add(new AmqpProtocol());
        protocols.add(new OpenwireProtocol());
        protocols.add(new HttpProtocol());
        protocols.add(new SslProtocol());
        DetectingGatewayProtocolHandler handler = new DetectingGatewayProtocolHandler();
        handler.setVertx(vertx);
        SslConfig sslConfig = new SslConfig(new File(basedir(), "src/test/resources/server.ks"), "password");
        sslConfig.setKeyPassword("password");
        handler.setSslConfig(sslConfig);
        handler.setServiceMap(serviceMap);
        handler.setProtocols(protocols);
        handler.setServiceLoadBalancer(serviceLoadBalancer);
        handler.setDefaultVirtualHost("broker1");
        return new DetectingGateway(vertx, 0, handler);
    }

    protected File basedir() {
        try {
          File file = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getFile());
          file = file.getParentFile().getParentFile().getCanonicalFile();
          if( file.isDirectory() ) {
              return file.getCanonicalFile();
          } else {
              return new File(".").getCanonicalFile();
          }
        } catch (Throwable e){
            return new File(".");
        }
    }

}
