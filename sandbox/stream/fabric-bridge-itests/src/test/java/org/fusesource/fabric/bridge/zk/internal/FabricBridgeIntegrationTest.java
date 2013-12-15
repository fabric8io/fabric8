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

import org.apache.activemq.pool.AmqJNDIPooledConnectionFactory;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.testing.AbstractIntegrationTest;
import org.apache.karaf.testing.Helper;
import io.fabric8.api.FabricService;
import io.fabric8.zookeeper.ZkDefs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import io.fabric8.zookeeper.IZKClient;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedServiceFactory;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

import javax.jms.ConnectionFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Hashtable;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.*;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.modifyBundle;

/**
 * @author Dhiraj Bokde
 */
@RunWith(JUnit4TestRunner.class)
public class FabricBridgeIntegrationTest extends AbstractIntegrationTest {

    protected static final int TEST_BATCH_SIZE = 10;
    private static final String LOCAL_URL = "vm://local?broker.persistent=false&broker.brokerName=local&broker.useJmx=false";
    protected static final String TEST_LOCAL_BROKER_URL = LOCAL_URL + "&jms.prefetchPolicy.queuePrefetch=" + TEST_BATCH_SIZE;

    private static final String REMOTE_URL = "vm://remote?broker.persistent=false&broker.brokerName=remote&broker.useJmx=false";
    protected static final String TEST_REMOTE_BROKER_URL = REMOTE_URL + "&jms.prefetchPolicy.queuePrefetch=" + TEST_BATCH_SIZE;

    private static final String TEST_BRIDGE_PID = "io.fabric8.bridge";
    private static final String TEST_GATEWAY_PID = "io.fabric8.gateway";
    private static final int BUF_SIZE = 1024;
    private static final int EOF = -1;
    private static final String FABRIC_BRIDGE_ZOOKEEPER_BUNDLE = "io.fabric8.bridge.fabric-bridge-zookeeper";
    private static final String FABRIC_COMMANDS_BUNDLE = "io.fabric8.fabric-commands";
    private static final String FABRIC_ZOOKEEPER_BUNDLE = "io.fabric8.fabric-zookeeper";

    private ConfigurationAdmin configurationAdmin;
    private org.osgi.service.cm.Configuration configuration;
    private String location;
    private Bundle bundle;
    private CommandProcessor commandProcessor;

    @Configuration
    public static Option[] configuration() throws Exception {
        return combine(
            // Default karaf environment
            Helper.getDefaultOptions(
                // this is how you set the default log level when using pax logging (logProfile)
                Helper.setLogLevel("INFO")),

            // add karaf features
            Helper.loadKarafStandardFeatures("obr", "wrapper"),

            // karaf commands won't run without this
            new Customizer() {
                @Override
                public InputStream customizeTestProbe(InputStream testProbe) {
                    return modifyBundle(testProbe)
                            .set(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional")
                            .build();
                }
            },            // add bridge features

            scanFeatures(
                    maven().groupId("io.fabric8").artifactId("fuse-fabric").type("xml").classifier("features").versionAsInProject(),
                    "fabric-commands", "fabric-bridge-zookeeper"
            ),

            workingDirectory("target/paxrunner/features/"),

//            vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=6006" ),
            // If you wnat to debug the OSGi modules add the following system property when your run the test
            // -Dpax-runner-vm-options="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
            System.getProperty("pax-runner-vm-options")!=null ? vmOption(System.getProperty("pax-runner-vm-options")) : null,

            waitForFrameworkStartup(),

            // Test on felix
            felix().version("3.0.2")
        );
    }

    @After
    public void tearDown() throws Exception {
        // shutdown fabric-bridge
        getInstalledBundle(FABRIC_BRIDGE_ZOOKEEPER_BUNDLE).stop();

        // shutdown ZK
        getInstalledBundle(FABRIC_ZOOKEEPER_BUNDLE).stop();
    }

    @Before
    public void setUp() throws Exception {

        // get config admin
        configurationAdmin = getOsgiService(ConfigurationAdmin.class, DEFAULT_TIMEOUT);

        // assert fabric-commands was started
        Thread.sleep(DEFAULT_TIMEOUT);
        assertNotNull("fabric-commands bundle not started",
            getInstalledBundle(FABRIC_COMMANDS_BUNDLE));

        commandProcessor = getOsgiService(CommandProcessor.class, DEFAULT_TIMEOUT);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CommandSession session = commandProcessor.createSession(System.in, new PrintStream(bos), System.err);
        try {
            // create test cluster
            assertNotNull("FabricService not found", getOsgiService(FabricService.class));
            session.execute("fabric:create --clean root");

            // wait for zookeeper service to become available
            Thread.sleep(DEFAULT_TIMEOUT);
            IZKClient zooKeeper = getOsgiService(IZKClient.class, DEFAULT_TIMEOUT);

            // assert fabric-bridge-zookeeper was started
            bundle = getInstalledBundle(FABRIC_BRIDGE_ZOOKEEPER_BUNDLE);
            assertNotNull("Bridge ZK bundle not started", bundle);
            location = bundle.getLocation();

            // wait for service factories to start
            assertNotNull("Fabric Bridge service factory not started",
                getOsgiService(ManagedServiceFactory.class,
                    "(" + Constants.SERVICE_PID + "=" + TEST_BRIDGE_PID + ")", DEFAULT_TIMEOUT));
            assertNotNull("Fabric Gateway service factory not started",
                getOsgiService(ManagedServiceFactory.class,
                    "(" + Constants.SERVICE_PID + "=" + TEST_GATEWAY_PID + ")", DEFAULT_TIMEOUT));

            // create test gateway profile
            bos.reset();
            session.execute("fabric:profile-list");
            if (!bos.toString().contains("test-gateway")) {
                session.execute("fabric:profile-create --parents default test-gateway");
            }

            // add test destinations config
            session.execute("zk:create -o -r /fabric/configs/versions/base/profiles/default/io.fabric8.bridge.bridgeDestinationsConfig.upstream.xml " +
                getDestinationsConfig("upstream.xml"));
            session.execute("zk:create -o -r /fabric/configs/versions/base/profiles/default/io.fabric8.bridge.bridgeDestinationsConfig.downstream.xml " +
                getDestinationsConfig("downstream.xml"));
        } finally {
            session.close();
        }
    }

    private String getDestinationsConfig(String destinationsName) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(destinationsName);
        assertNotNull(destinationsName + " not found", stream);
        byte[] buffer = new byte[BUF_SIZE];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        String retVal = null;
        try {
            int nBytes;
            while (EOF != (nBytes = stream.read(buffer))) {
                bos.write(buffer, 0, nBytes);
            }
            // remove newlines and surround with `'`
            retVal = "'" + bos.toString("UTF-8").replace("\n", "") + "'";
        } catch (IOException e) {
            fail("Failed to read " + destinationsName + " : " + e.getMessage());
        }

        return retVal;
    }

    @Test
    public void testBrokerUrlGatewayUpdate() throws Exception {
        // create a simple broker URL based gateway
        Hashtable<String, String> properties = getDefaultGatewayConfig();
        properties.put("localBroker.brokerUrl", TEST_REMOTE_BROKER_URL);
        properties.put("exportedBroker.brokerUrl", TEST_REMOTE_BROKER_URL);

        updateConfig(TEST_GATEWAY_PID, properties);

        // assert that the gateway was started
        Thread.sleep(DEFAULT_TIMEOUT);
        assertNotNull("Gateway not started",
            configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + configuration.getPid() + ")"));
    }

    @Test
    public void testRefsGatewayUpdate() throws Exception {
        registerTestServices();

        // create a simple OSGi service references based gateway
        Hashtable<String, String> properties = getDefaultGatewayConfig();
        properties.put("localBroker.connectionFactoryRef", "remoteCF");
        properties.put("exportedBroker.connectionFactoryRef", "remoteCF");

        properties.put("localBroker.destinationResolverRef", "localResolver");
        properties.put("exportedBroker.destinationResolverRef", "localResolver");

        updateConfig(TEST_GATEWAY_PID, properties);

        // assert that the gateway was started
        Thread.sleep(DEFAULT_TIMEOUT);
        assertNotNull("Gateway not started",
            configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + configuration.getPid() + ")"));
    }

    // create required OSGi service references
    private void registerTestServices() {
        Hashtable<String, String> serviceProps = new Hashtable<String, String>();
        serviceProps.put(Constants.SERVICE_PID, "localCF");
        bundleContext.registerService(ConnectionFactory.class.getName(), new AmqJNDIPooledConnectionFactory(TEST_LOCAL_BROKER_URL), serviceProps);
        serviceProps.put(Constants.SERVICE_PID, "remoteCF");
        bundleContext.registerService(ConnectionFactory.class.getName(), new AmqJNDIPooledConnectionFactory(TEST_REMOTE_BROKER_URL), serviceProps);
        serviceProps.put(Constants.SERVICE_PID, "localResolver");
        bundleContext.registerService(DestinationResolver.class.getName(), new DynamicDestinationResolver(), serviceProps);
    }

    @Test
    public void testGatewayDelete() throws Exception {
        testBrokerUrlGatewayUpdate();

        final String configurationPid = configuration.getPid();
        configuration.delete();

        // assert that the gateway was destroyed
        Thread.sleep(DEFAULT_TIMEOUT);
        assertNull("Gateway not destroyed",
            configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + configurationPid + ")"));
    }

    private Hashtable<String, String> getDefaultGatewayConfig() {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("versionName", ZkDefs.DEFAULT_VERSION);
        properties.put("profileName", "test-gateway");
        properties.put("inboundDestinationsRef", "upstream");
        properties.put("outboundDestinationsRef", "downstream");
        return properties;
    }

    @Test
    public void testBrokerUrlBridgeUpdate() throws Exception {
        // start the test gateway first
        testBrokerUrlGatewayUpdate();

        // create a simple broker URL based bridge
        Hashtable<String, String> properties = getDefaultBridgeConfig();
        properties.put("localBroker.brokerUrl", TEST_LOCAL_BROKER_URL);
        properties.put("exportedBroker.brokerUrl", TEST_LOCAL_BROKER_URL);

        updateConfig(TEST_BRIDGE_PID, properties);

        // assert that the bridge was started
        Thread.sleep(DEFAULT_TIMEOUT);
        assertNotNull("Bridge not started",
            configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + configuration.getPid() + ")"));
    }

    @Test
    public void testRefsBridgeUpdate() throws Exception {
        registerTestServices();

        // start the test gateway first
        testBrokerUrlGatewayUpdate();

        // create a simple broker URL based bridge
        Hashtable<String, String> properties = getDefaultBridgeConfig();
        properties.put("localBroker.connectionFactoryRef", "localCF");
        properties.put("exportedBroker.connectionFactoryRef", "localCF");

        properties.put("localBroker.destinationResolverRef", "localResolver");
        properties.put("exportedBroker.destinationResolverRef", "localResolver");

        updateConfig(TEST_BRIDGE_PID, properties);

        // assert that the bridge was started
        Thread.sleep(DEFAULT_TIMEOUT);
        assertNotNull("Bridge not started",
            configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + configuration.getPid() + ")"));
    }

    @Test
    public void testBridgeDelete() throws Exception {
        testBrokerUrlBridgeUpdate();

        final String configurationPid = configuration.getPid();
        configuration.delete();

        // verify that the bridge was destroyed
        Thread.sleep(DEFAULT_TIMEOUT);
        assertNull("Bridge not destroyed",
            configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + configurationPid + ")"));
    }

    private void updateConfig(String factoryPid, Hashtable<String, String> properties) throws IOException {
        configuration = configurationAdmin.createFactoryConfiguration(factoryPid, location);
        configuration.update(properties);
    }

    private Hashtable<String, String> getDefaultBridgeConfig() {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("versionName", ZkDefs.DEFAULT_VERSION);
        properties.put("gatewayProfileName", "test-gateway");
        properties.put("gatewayConnectRetries", "5");
        properties.put("gatewayStartupDelay", "3");
        properties.put("inboundDestinationsRef", "downstream");
        properties.put("outboundDestinationsRef", "upstream");
        return properties;
    }

}
