package org.fusesource.fabric.itests;

import de.kalpatec.pojosr.framework.launch.BundleDescriptor;
import de.kalpatec.pojosr.framework.launch.ClasspathScanner;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistry;
import de.kalpatec.pojosr.framework.launch.PojoServiceRegistryFactory;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.ZooKeeperClusterService;
import org.fusesource.fabric.zookeeper.internal.ZKServerFactoryBean;
import org.junit.Test;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class FabricServiceImplTest {

    public static int DEFAULT_TIMEOUT = 10000;

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
                    ServiceReference ref = event.getServiceReference();
                    Object service = registry.getService(ref);
                    switch (event.getType()) {
                        case ServiceEvent.MODIFIED:
                            log.info("Service modified : " + service);
                            break;
                        case ServiceEvent.MODIFIED_ENDMATCH:
                            log.info("Service modified endmatch : " + service);
                            break;
                        case ServiceEvent.REGISTERED:
                            log.info("Service registering : " + service);
                            break;
                        case ServiceEvent.UNREGISTERING:
                            log.info("Service unregistering : " + service);
                            break;
                        default:
                            log.info("Unknown event : " + event.getType() + " service : " + service);
                    }
                }
            });
            Thread.sleep(10000);
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
            log.info(String.format("Found bundle : %s with version %s in state %s", b.getSymbolicName(), b.getVersion(), b.getState()));
        }

        for (ServiceReference ref : bc.getAllServiceReferences(null, null)) {
            log.info(String.format("Found Service reference : %s", ref.toString()));
        }

        /*
        log.info("Getting zk server managed service factory ref");
        ManagedServiceFactory msf = getService(ManagedServiceFactory.class, "(service.pid=org.fusesource.fabric.zookeeper.server)", registry);
        assertNotNull(msf);
        log.info("done");
        */

        /*
        log.info("Getting config admin");
        ConfigurationAdmin ca = getConfigAdmin(registry);
        assertNotNull(ca);
        log.info("done");

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
        //props.put("zookeeper.url", "localhost:2181");
        cfgClient.setBundleLocation(null);
        cfgClient.update(props);
        */

        /*
        log.info("Getting server stats provider service");
        ServerStats.Provider server = getService(ServerStats.Provider.class, registry);
        assertNotNull(server);
        log.info("done");

        log.info("Getting zk client service");
        IZKClient client = getService(IZKClient.class, registry);
        assertNotNull(client);
        log.info("done");
        */
//        log.info("Waiting for a wicked long time...\n\n\n\n");
        //log.info("Done waiting...\n\n\n\n\n");

        log.info("Getting zk cluster service");
        ZooKeeperClusterService clusterService = getService(ZooKeeperClusterService.class, registry);
        assertNotNull(clusterService);
        log.info("done");

        List<String> agents = new ArrayList<String>();
        agents.add("root");
        log.info("Creating zk cluster config");
        clusterService.createCluster(agents);

        Thread.sleep(10000);

        log.info("Getting fabric service");
        FabricService service = getService(FabricService.class, registry);
        assertNotNull(service);
        log.info("Done");

        Thread.sleep(10000);
        log.info("Profiles :");
        for (Profile profile : service.getDefaultVersion().getProfiles()) {
            log.info("Profile : " + profile.getId());
        }

//        log.info("Waiting a bit...\n\n");
//        Thread.sleep(20000);

//        log.info("Exiting...\n\n");
        /*

        log.info("Waiting a bit more...\n\n");
        Thread.sleep(10000);

        log.info("Waiting even more...\n\n");
        Thread.sleep(10000);

        */

        //assertNotNull(client.exists(ZkPath.AGENT_ALIVE.getPath(System.getProperty("karaf.name"))));
    }
}
