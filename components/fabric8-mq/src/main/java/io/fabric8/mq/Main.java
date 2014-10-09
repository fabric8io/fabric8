package io.fabric8.mq;

import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.jmx.ManagementContext;
import org.apache.activemq.leveldb.LevelDBStoreFactory;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static String brokerName;
    private static String dataDirectory;
    private static int port;

    public static void main(String args[]) {
        try {
            try {
                brokerName = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_BROKER_NAME");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_BROKER_NAME", "AMQ_Broker") : result;
                        return result;
                    }
                });
                String portStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_PORT");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_PORT", "61616") : result;
                        return result;
                    }
                });
                if (portStr != null && portStr.length() > 0) {
                    port = Integer.parseInt(portStr);
                }
                dataDirectory = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_DATA_DIRECTORY");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_DATA_DIRECTORY", "data") : result;
                        return result;
                    }
                });

            } catch (Throwable e) {
                LOG.warn("Failed to look up System properties for host and port", e);
            }

            if (port <= 0) {
                port = 61616;
            }
            if (brokerName == null) {
                brokerName = "default";
            }
            if (dataDirectory == null) {
                dataDirectory = "data";
            }
            BrokerService brokerService = new BrokerService();
            brokerService.setBrokerName(brokerName);
            brokerService.setDataDirectory(dataDirectory);

            //we create our own ManagementContext - so ActiveMQ doesn't create a needless JMX Connector
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ManagementContext managementContext = new ManagementContext(server);
            managementContext.setCreateConnector(false);

            brokerService.setManagementContext(managementContext);

            List<BrokerPlugin> list = new ArrayList<>();
            list.add(new StatisticsBrokerPlugin());

            //ToDo - uncomment this when we move to ActiveMQ 5.11
            //list.add(new CamelRoutesBrokerPlugin());
            BrokerPlugin[] plugins = new BrokerPlugin[list.size()];
            list.toArray(plugins);
            brokerService.setPlugins(plugins);

            LevelDBStoreFactory persistenceFactory = new LevelDBStoreFactory();
            persistenceFactory.setDirectory(new File(getDataDirectory()));
            persistenceFactory.setSync(false);
            brokerService.setPersistenceFactory(persistenceFactory);

            //set max available memory to the broker

            long maxMemory = Runtime.getRuntime().maxMemory();
            long brokerMemory = (long) (maxMemory * 0.7);

            brokerService.getSystemUsage().getMemoryUsage().setLimit(brokerMemory);
            String connector = "tcp://localhost:" + port;
            System.out.println("Starting broker on " + connector);
            brokerService.addConnector(connector);

            brokerService.start();

            waitUntilStop();
        } catch (Throwable e) {
            LOG.error("Failed to Start Fabric8MQ", e);
        }
    }

    protected static void waitUntilStop() {
        Object lock = new Object();
        while (true) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    public static String getBrokerName() {
        return brokerName;
    }

    public static String getDataDirectory() {
        return dataDirectory;
    }

    public static int getPort() {
        return port;
    }
}