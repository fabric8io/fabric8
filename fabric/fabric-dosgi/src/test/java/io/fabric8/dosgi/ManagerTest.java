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
package io.fabric8.dosgi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import io.fabric8.dosgi.impl.Manager;
import io.fabric8.zookeeper.spring.ZKServerFactoryBean;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.easymock.EasyMock.*;

public class ManagerTest {

    @Test
    public void testManager() throws Exception {

        ZKServerFactoryBean zkServerFactoryBean = null;

        try {

            int zooKeeperPort = getFreePort();
            int serverPort = getFreePort();

            zkServerFactoryBean = new ZKServerFactoryBean();
            zkServerFactoryBean.setPurge(true);
            zkServerFactoryBean.setClientPortAddress(new InetSocketAddress("localhost", zooKeeperPort));
            zkServerFactoryBean.afterPropertiesSet();

            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                    .connectString("localhost:" + zooKeeperPort)
                    .retryPolicy(new RetryOneTime(1000))
                    .connectionTimeoutMs(60000);

            CuratorFramework curator = builder.build();
            curator.start();
            curator.getZookeeperClient().blockUntilConnectedOrTimedOut();

            BundleContext bundleContext = createMock(BundleContext.class);
            ServiceRegistration registration = createMock(ServiceRegistration.class);
            Manager manager = new Manager(bundleContext, curator, "tcp://localhost:" + serverPort, "localhost", TimeUnit.MINUTES.toMillis(5));

            bundleContext.addServiceListener(manager, "(service.exported.interfaces=*)");
            expect(bundleContext.getProperty("org.osgi.framework.uuid")).andReturn("the-framework-uuid");
            expect(bundleContext.registerService(
                    EasyMock.<String[]>anyObject(),
                    same(manager),
                    EasyMock.<Dictionary>same(null))).andReturn(registration);
            expect(bundleContext.getServiceReferences((String) null, "(service.exported.interfaces=*)")).andReturn(null);

            replay(bundleContext, registration);

            manager.init();

            verify(bundleContext, registration);

            reset(bundleContext, registration);

            BundleContext expBundleContext = createMock(BundleContext.class);
            Bundle expBundle = createMock(Bundle.class);
            ServiceReference reference = createMock(ServiceReference.class);
            final Properties props = new Properties();
            props.put(Constants.OBJECTCLASS, new String[]{ConfigurationAdmin.class.getName()});
            expect(reference.getProperty(EasyMock.<String>anyObject())).andAnswer(new IAnswer<Object>() {
                public Object answer() throws Throwable {
                    return props.get(EasyMock.getCurrentArguments()[0]);
                }
            }).anyTimes();
            expect(reference.getPropertyKeys()).andReturn(props.keySet().toArray(new String[0]));
            expect(reference.getBundle()).andReturn(expBundle).anyTimes();
            expect(expBundle.getBundleContext()).andReturn(expBundleContext).anyTimes();
            expect(expBundle.getState()).andReturn(Bundle.ACTIVE).anyTimes();

            replay(bundleContext, registration, reference, expBundleContext, expBundle);

            manager.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference));
            Thread.sleep(1000);

            verify(bundleContext, registration, reference, expBundleContext, expBundle);

        }
        finally {
            try {
                zkServerFactoryBean.destroy();
            } catch (Throwable t) { }
        }
    }

    static int getFreePort() throws IOException {
        ServerSocket sock = new ServerSocket();
        try {
            sock.bind(new InetSocketAddress(0));
            return sock.getLocalPort();
        } finally {
            sock.close();
        }
    }
}
