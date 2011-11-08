package org.fusesource.fabric.itests;

import de.kalpatec.pojosr.framework.launch.BundleDescriptor;
import de.kalpatec.pojosr.framework.launch.ClasspathScanner;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistry;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistryFactory;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.internal.ZKServerFactoryBean;
import org.junit.Test;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class FabricServiceImplTest {

    private static PojoServiceRegistry registry = null;
    private ZKServerFactoryBean zk;

    public static PojoServiceRegistry getRegistry() throws Exception {
        if (registry == null) {
            final Logger log = LoggerFactory.getLogger("Test Registry");
            System.setProperty("org.osgi.framework.storage", "target/osgi/" + System.currentTimeMillis());
            System.setProperty("karaf.name", "root");

            List<BundleDescriptor> bundles = new ClasspathScanner().scanForBundles();

            log.info("Located following bundles on classpath : ");
            for (BundleDescriptor desc : bundles) {
                log.info("Bundle : {}", desc);
            }

            Map config = new HashMap();
            config.put(PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS, bundles);
            ServiceLoader<PojoServiceRegistryFactory> loader = ServiceLoader.load(PojoServiceRegistryFactory.class);
            registry = loader.iterator().next().newPojoServiceRegistry(config);
            registry.addServiceListener(new ServiceListener() {
                @Override
                public void serviceChanged(ServiceEvent event) {
                    log.info("Service changed : " + event.toString());
                }
            });
            Thread.sleep(5000);
        }
        return registry;
    }

    public static <T> T getService(Class<T> type, String filter, PojoServiceRegistry registry) {
        try {
            ServiceReference[] ref = registry.getServiceReferences(type.getName(), filter);
            if (ref == null) {
                return null;
            }
            if (ref.length > 1 || ref.length == 0) {
                return null;
            }
            return type.cast(registry.getService(ref[0]));
        } catch (InvalidSyntaxException e) {
            return null;
        }
    }

    public static <T> T getService(Class<T> type, PojoServiceRegistry registry) {
        ServiceReference ref = registry.getServiceReference(type.getName());
        if (ref == null) {
            return null;
        }
        return type.cast(registry.getService(ref));
    }

    public static ConfigurationAdmin getConfigAdmin(PojoServiceRegistry registry) {
        return getService(ConfigurationAdmin.class, registry);
    }

    @Test
    public void testFabricServiceImpl() throws Exception {
        Logger log = LoggerFactory.getLogger("test");
        PojoServiceRegistry registry = getRegistry();
        BundleContext bc = registry.getBundleContext();

        for (Bundle b : bc.getBundles()) {
            log.info(String.format("Found bundle : %s in state %s", b.getSymbolicName(), b.getState()));
        }

        for (ServiceReference ref : bc.getAllServiceReferences(null, null)) {
            log.info(String.format("Found Service reference : %s", ref.toString()));
        }

        ConfigurationAdmin ca = getConfigAdmin(registry);
        assertNotNull(ca);

        Configuration cfgServer = ca.createFactoryConfiguration("org.fusesource.fabric.zookeeper.server");
        Properties props = new Properties();
        props.put("tickTime", "2000");
        props.put("initLimit", "10");
        props.put("syncLimit", "5");
        props.put("dataDir", "target/zookeeper");
        props.put("clientPort", "2181");
        cfgServer.setBundleLocation(null);
        cfgServer.update(props);

        Configuration cfgClient = ca.getConfiguration("org.fusesource.fabric.zookeeper");
        props = new Properties();
        props.put("zookeeper.url", "localhost:2181");
        cfgClient.setBundleLocation(null);
        cfgClient.update(props);

        ManagedServiceFactory msf = getService(ManagedServiceFactory.class, "(service.pid=org.fusesource.fabric.zookeeper.server)", registry);
        assertNotNull(msf);

        IZKClient client = getService(IZKClient.class, registry);
        assertNotNull(client);

        Thread.sleep(1000);

        assertNotNull(client.exists(ZkPath.AGENT_ALIVE.getPath(System.getProperty("karaf.name"))));
    }
}
