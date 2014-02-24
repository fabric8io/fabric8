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
package org.fusesource.gateway.handlers.detecting;


import org.apache.activemq.apollo.broker.Broker;
import org.apache.activemq.apollo.dto.AcceptingConnectorDTO;
import org.apache.activemq.apollo.dto.BrokerDTO;
import org.apache.activemq.apollo.dto.VirtualHostDTO;
import org.apache.activemq.apollo.util.ServiceControl;
import org.apache.qpid.amqp_1_0.jms.impl.ConnectionFactoryImpl;
import org.fusesource.gateway.ServiceDTO;
import org.fusesource.gateway.ServiceDetails;
import org.fusesource.gateway.ServiceMap;
import org.fusesource.gateway.handlers.detecting.protocol.amqp.AmqpProtocol;
import org.fusesource.gateway.handlers.detecting.protocol.mqtt.MqttProtocol;
import org.fusesource.gateway.handlers.detecting.protocol.stomp.StompProtocol;
import org.fusesource.gateway.loadbalancer.LoadBalancer;
import org.fusesource.gateway.loadbalancer.LoadBalancers;
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
                "amqp://localhost:" + portOfBroker(i)
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

        assertEquals(1, getConnectionsOnBroker(0));
        for( int i = 1; i < brokers.size(); i++) {
            assertEquals(0, getConnectionsOnBroker(i));
        }

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

        assertEquals(1, getConnectionsOnBroker(0));
        for( int i = 1; i < brokers.size(); i++) {
            assertEquals(0, getConnectionsOnBroker(i));
        }

        connection.kill();
    }

    // This test is not yet ready for prime time..
    // @Test
    public void canDetectTheAMQPProtocol() throws Exception {
        DetectingGateway gateway = createGateway();
        gateway.init();
        final ConnectionFactoryImpl factory = new ConnectionFactoryImpl("localhost", gateway.getBoundPort(), "admin", "password");
        Connection connection = factory.createConnection();
        connection.start();

        assertEquals(1, getConnectionsOnBroker(0));
        for( int i = 1; i < brokers.size(); i++) {
            assertEquals(0, getConnectionsOnBroker(i));
        }

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
        DetectingGatewayProtocolHandler handler = new DetectingGatewayProtocolHandler(vertx, serviceMap, protocols, serviceLoadBalancer);
        return new DetectingGateway(vertx, 0, handler);
    }
}
