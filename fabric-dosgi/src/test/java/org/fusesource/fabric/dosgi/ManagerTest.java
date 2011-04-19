package org.fusesource.fabric.dosgi;

import java.net.InetSocketAddress;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.easymock.EasyMock.*;

public class ManagerTest {

    @Test
    public void testManager() throws Exception {

        ZKServerFactoryBean zkServerFactoryBean = null;

        try {

            zkServerFactoryBean = new ZKServerFactoryBean();
            zkServerFactoryBean.setClientPortAddress(new InetSocketAddress("localhost", 2180));
            zkServerFactoryBean.afterPropertiesSet();


            ZKClient zooKeeper = new ZKClient("localhost:2180", Timespan.ONE_MINUTE, null);
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
            Manager manager = new Manager(bundleContext, zooKeeper);

            bundleContext.addServiceListener(manager, "(service.exported.interfaces=*)");
            expect(bundleContext.getProperty("org.osgi.framework.uuid")).andReturn("the-framework-uuid");
            expect(bundleContext.registerService(ListenerHook.class.getName(), manager, null)).andReturn(registration);
            expect(bundleContext.getServiceReferences(null, "(service.exported.interfaces=*)")).andReturn(null);

            replay(bundleContext, registration);

            manager.init();

            verify(bundleContext, registration);

            reset(bundleContext, registration);

            ServiceReference reference = createMock(ServiceReference.class);
            final Properties props = new Properties();
            props.put(Constants.OBJECTCLASS, new String[] { ConfigurationAdmin.class.getName() });
            expect(reference.getProperty(EasyMock.<String>anyObject())).andAnswer(new IAnswer<Object>() {
                public Object answer() throws Throwable {
                    return props.get(EasyMock.getCurrentArguments()[0]);
                }
            }).anyTimes();
            expect(reference.getPropertyKeys()).andReturn(props.keySet().toArray(new String[0]));

            replay(bundleContext, registration, reference);

            manager.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference));
            Thread.sleep(1000);

            verify(bundleContext, registration, reference);

        }
        finally {
            try {
                zkServerFactoryBean.destroy();
            } catch (Throwable t) { }
        }
    }

}
