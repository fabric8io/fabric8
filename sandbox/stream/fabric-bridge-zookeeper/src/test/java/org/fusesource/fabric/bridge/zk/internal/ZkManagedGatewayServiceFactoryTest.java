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

package io.fabric8.bridge.zk.internal;

import org.apache.activemq.pool.PooledConnectionFactory;
import io.fabric8.api.FabricService;
import io.fabric8.bridge.internal.AbstractConnectorTestSupport;
import io.fabric8.service.FabricServiceImpl;
import io.fabric8.zookeeper.ZkDefs;
import org.junit.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.mock.MockServiceReference;

import javax.jms.ConnectionFactory;

import java.util.Hashtable;

/**
 * @author Dhiraj Bokde
 */
public class ZkManagedGatewayServiceFactoryTest extends AbstractConnectorTestSupport {

    private static final String CONNECTION_FACTORY_CLASS_NAME = ConnectionFactory.class.getName();
    private static final String DESTINATION_RESOLVER_CLASS_NAME = DestinationResolver.class.getName();
    private static final String REMOTE_FACTORY_FILTER = "(" + Constants.SERVICE_PID + "=remoteCF" + ")";

    private static ClassPathXmlApplicationContext applicationContextZkServer;
    // TODO modify test to connect to bridge and verify that messages can be exchanged
//    private static ClassPathXmlApplicationContext bridgeContext;

    private static FabricService fabricService;
    private static BundleContext bundleContext;
    private static final String TEST_PID = "io.fabric8.gateway.test-gateway";
    private static final String SERVICE_PROPERTY = "service";

    private ZkManagedGatewayServiceFactory serviceFactory;

    @BeforeClass
    public static void setUpClass() {
        applicationContextZkServer = new ClassPathXmlApplicationContext("test-zkserver.xml");

        fabricService = applicationContextZkServer.getBean(FabricService.class);

        bundleContext = new MockBundleContext() {

            PooledConnectionFactory localConnectionFactory = new PooledConnectionFactory(TEST_LOCAL_BROKER_URL);
            PooledConnectionFactory remoteConnectionFactory = new PooledConnectionFactory(TEST_REMOTE_BROKER_URL);

            @Override
            public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
                Hashtable<String, Object> properties = new Hashtable<String, Object>();
                if (CONNECTION_FACTORY_CLASS_NAME.equals(clazz)) {
                    if (REMOTE_FACTORY_FILTER.equals(filter)) {
                        properties.put(SERVICE_PROPERTY, remoteConnectionFactory);
                    } else  {
                        return null;
                    }
                } else if (DESTINATION_RESOLVER_CLASS_NAME.equals(clazz)) {
                    properties.put(SERVICE_PROPERTY, new DynamicDestinationResolver());
                } else {
                    return null;
                }
                return new ServiceReference[] { new MockServiceReference(null, properties, null) };
            }

            @Override
            public Object getService(ServiceReference reference) {
                return reference.getProperty(SERVICE_PROPERTY);
            }
        };

        // setup test bridge
//        bridgeContext = new ClassPathXmlApplicationContext("test-zkbridge-context.xml");
    }

    @AfterClass
    public static void tearDownClass() {
//        bridgeContext.destroy();
        applicationContextZkServer.destroy();
    }

    @Before
    public void setUp() {
        serviceFactory = new ZkManagedGatewayServiceFactory();
        serviceFactory.setZooKeeper(((FabricServiceImpl)fabricService).getCurator());
        serviceFactory.setFabricService(fabricService);
        serviceFactory.setBundleContext(bundleContext);
    }

    @After
    public void tearDown() throws Exception {
        serviceFactory.destroy();
        serviceFactory = null;
    }

    @Test
    public void testInit() throws Exception {
        serviceFactory.init();
    }

    @Test
    public void testBrokerUrlGatewayUpdate() throws Exception {
        // start
        serviceFactory.init();

        // create a simple broker URL based gateway
        Hashtable<String, String> properties = getDefaultConfig();
        properties.put("localBroker.brokerUrl", TEST_REMOTE_BROKER_URL);
        properties.put("exportedBroker.brokerUrl", TEST_REMOTE_BROKER_URL);

        serviceFactory.updated(TEST_PID, properties);

        // TODO assert that the gateway was started
    }

    @Test
    public void testRefsGatewayUpdate() throws Exception {
        // start
        serviceFactory.init();

        // create a simple OSGi references based gateway
        Hashtable<String, String> properties = getDefaultConfig();
        properties.put("localBroker.connectionFactoryRef", "remoteCF");
        properties.put("exportedBroker.connectionFactoryRef", "remoteCF");

        properties.put("localBroker.destinationResolverRef", "localResolver");
        properties.put("exportedBroker.destinationResolverRef", "localResolver");

        serviceFactory.updated(TEST_PID, properties);

        // TODO assert that the gateway was started
    }

    private Hashtable<String, String> getDefaultConfig() {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("versionName", ZkDefs.DEFAULT_VERSION);
        properties.put("profileName", "test-gateway");
        properties.put("inboundDestinationsRef", "upstream");
        properties.put("outboundDestinationsRef", "downstream");
        return properties;
    }

    @Test
    public void testDeleted() throws Exception {
        testBrokerUrlGatewayUpdate();

        serviceFactory.deleted(TEST_PID);
    }

}
