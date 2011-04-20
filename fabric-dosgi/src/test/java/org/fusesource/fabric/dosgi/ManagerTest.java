package org.fusesource.fabric.dosgi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.fusesource.fabric.dosgi.impl.Manager;
import org.fusesource.fabric.zookeeper.spring.ZKServerFactoryBean;
import org.junit.Test;
import org.linkedin.util.clock.Timespan;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.linkedin.zookeeper.client.ZKClient;
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
            zkServerFactoryBean.setClientPortAddress(new InetSocketAddress("localhost", zooKeeperPort));
            zkServerFactoryBean.afterPropertiesSet();


            ZKClient zooKeeper = new ZKClient("localhost:" + zooKeeperPort, Timespan.ONE_MINUTE, null);
            final AtomicBoolean connected = new AtomicBoolean(false);
            zooKeeper.registerListener(new LifecycleListener() {
                public void onConnected() {
                    synchronized (connected) {
                        connected.set(true);
                        connected.notifyAll();
                    }
                }

                public void onDisconnected() {
                    synchronized (connected) {
                        connected.set(false);
                        connected.notifyAll();
                    }
                }
            });
            zooKeeper.start();

            synchronized (connected) {
                while (!zooKeeper.isConnected()) {
                    connected.wait();
                }
            }

            BundleContext bundleContext = createMock(BundleContext.class);
            ServiceRegistration registration = createMock(ServiceRegistration.class);
            Manager manager = new Manager(bundleContext, zooKeeper, "tcp://localhost:" + serverPort);

            bundleContext.addServiceListener(manager, "(service.exported.interfaces=*)");
            expect(bundleContext.getProperty("org.osgi.framework.uuid")).andReturn("the-framework-uuid");
            expect(bundleContext.registerService(
                    EasyMock.<String[]>anyObject(),
                    same(manager),
                    EasyMock.<Dictionary>same(null))).andReturn(registration);
            expect(bundleContext.getServiceReferences(null, "(service.exported.interfaces=*)")).andReturn(null);

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
