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
package org.fusesource.fabric.zookeeper.internal;

import java.net.ServerSocket;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeoutException;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.server.ServerStats;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.linkedin.util.clock.Timespan;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OsgiZkClientTest {
    
    int port;
    ZKServerFactoryBean serverFactory;
    BundleContext bundleContext;
    ServiceRegistration zkClientRegistration;
    ServiceRegistration managedServiceRegistration;
    ServiceRegistration serverStatsRegistration;
    OsgiZkClient client = new OsgiZkClient();
    
    @Before
    public void setUp() throws Exception {
        ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        bundleContext = createMock(BundleContext.class);
        zkClientRegistration = createMock(ServiceRegistration.class);
        managedServiceRegistration = createMock(ServiceRegistration.class);
        serverStatsRegistration = createMock(ServiceRegistration.class);

        serverFactory = new ZKServerFactoryBean();
        serverFactory.setBundleContext(bundleContext);

        client = new OsgiZkClient();
        client.setBundleContext(bundleContext);
    }
    
    @After
    public void tearDown() throws Exception {
        reset(bundleContext, serverStatsRegistration, managedServiceRegistration, zkClientRegistration);
        zkClientRegistration.unregister();
        managedServiceRegistration.unregister();
        replay(bundleContext, serverStatsRegistration, managedServiceRegistration, zkClientRegistration);

        
        client.destroy();
        serverFactory.destroy();

        verify(bundleContext, serverStatsRegistration, managedServiceRegistration, zkClientRegistration);
        reset(bundleContext, serverStatsRegistration, managedServiceRegistration, zkClientRegistration);
    }

    @Test
    public void testZk() throws Exception {
        reset(bundleContext, serverStatsRegistration, managedServiceRegistration, zkClientRegistration);
        expect(bundleContext.registerService(eq(IZKClient.class.getName()), same(client), (Dictionary) anyObject())).andReturn(zkClientRegistration);
        expect(bundleContext.registerService(eq(ManagedService.class.getName()), same(client), (Dictionary) anyObject())).andReturn(managedServiceRegistration);

        replay(bundleContext, serverStatsRegistration, managedServiceRegistration, zkClientRegistration);
        client.init();
        verify(bundleContext, serverStatsRegistration, managedServiceRegistration, zkClientRegistration);

        assertFalse(client.isConfigured());
        assertFalse(client.isConnected());
        try {
            client.getChildren("/");
        } catch (IllegalStateException e) {
            // expected, as no zookeeper configured
        }

        reset(bundleContext, serverStatsRegistration, managedServiceRegistration, zkClientRegistration);
        zkClientRegistration.setProperties((Dictionary) anyObject());
        replay(bundleContext, serverStatsRegistration, managedServiceRegistration, zkClientRegistration);

        Hashtable properties = new Hashtable();
        properties.put("zookeeper.url", "localhost:" + port);
        client.updated(properties);

        verify(bundleContext, serverStatsRegistration, managedServiceRegistration, zkClientRegistration);

        assertTrue(client.isConfigured());
        assertFalse(client.isConnected());

        try {
            client.waitForConnected(Timespan.parse("2s"));
            fail("Expected a timeout exception");
        } catch (TimeoutException e) {
            // expected
        }
        
        createServer();

        try {
            client.waitForConnected(Timespan.parse("10s"));
        } catch (TimeoutException e) {
            fail("Did not expect a timeout exception");
        }

        assertTrue(client.isConfigured());
        assertTrue(client.isConnected());
    }

    protected void createServer() throws Exception {
        reset(bundleContext, serverStatsRegistration, managedServiceRegistration, zkClientRegistration);
        expect(bundleContext.registerService(eq(ServerStats.Provider.class.getName()), anyObject(), (Dictionary) anyObject())).andReturn(serverStatsRegistration);
        replay(bundleContext, serverStatsRegistration, managedServiceRegistration, zkClientRegistration);

        Hashtable properties = new Hashtable();
        properties.put("tickTime", "2000");
        properties.put("initLimit", "10");
        properties.put("syncLimit", "5");
        properties.put("dataDir", "target/data/zookeeper/" + System.currentTimeMillis());
        properties.put("clientPort", Integer.toString(port));
        serverFactory.updated("pid", properties);

        verify(bundleContext, serverStatsRegistration, managedServiceRegistration, zkClientRegistration);
    }

}
