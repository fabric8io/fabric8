package io.fabric8.mq.consumer;

import java.security.AccessController;
import java.security.PrivilegedAction;

import io.fabric8.utils.Systems;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.dataset.SimpleDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerMain {
    private static final Logger LOG = LoggerFactory.getLogger(ConsumerMain.class);

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final String DEFAULT_PORT = "61616";

    private static String host;

    private static int port;
    
    private static int prefetch;
    
    private static String queueName;

    public static void main(String args[]) {
        try {
            String serviceName = Systems.getEnvVarOrSystemProperty("AMQ_SERVICE_ID", "AMQ_SERVICE_ID", "FABRIC8MQ").toUpperCase() + "_SERVICE";
            String hostEnvVar = serviceName + "_HOST";
            String portEnvVar = serviceName + "_PORT";
            try {

                host = Systems.getEnvVarOrSystemProperty(hostEnvVar, hostEnvVar, DEFAULT_HOST);
                String portStr = Systems.getEnvVarOrSystemProperty(portEnvVar, hostEnvVar, DEFAULT_PORT);
                if (portStr != null && portStr.length() > 0) {
                    port = Integer.parseInt(portStr);
                }
                if (portStr != null && portStr.length() > 0) {
                    port = Integer.parseInt(portStr);
                }
                String prefetchStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_PREFETCH");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_PREFETCH", "1000") : result;
                        return result;
                    }
                });
                if (prefetchStr != null && prefetchStr.length() > 0) {
                	prefetch = Integer.parseInt(prefetchStr);
                }
                
                queueName = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_QUEUENAME");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_QUEUENAME", "TEST.FOO") : result;
                        return result;
                    }
                });
                
            } catch (Throwable e) {
                LOG.warn("Failed to look up System properties for host and port", e);
            }

            if (host == null || host.length() == 0) {
                host = DEFAULT_HOST;
            }

            if (port <= 0) {
                port = 61616;
            }
            
            if (prefetch <= 0) {
            	prefetch = 1000;
            }
            
            if (queueName == null) {
            	queueName = "TEST.FOO";
            }

            System.out.println("Using broker host " + host + " from $" + hostEnvVar + " and port " + port + " from $" + portEnvVar);

            // create a camel route to consume messages from our queue
            org.apache.camel.main.Main main = new org.apache.camel.main.Main();
            String brokerURL = "tcp://" + host + ":" + port + "?jms.prefetchPolicy.all=" + prefetch;
            System.out.println("Connecting to brokerURL " + brokerURL);
            main.bind("activemq", ActiveMQComponent.activeMQComponent(brokerURL));
            main.bind("myDataSet", new SimpleDataSet());
            main.enableHangupSupport();
            
            main.addRouteBuilder(new RouteBuilder() {
                public void configure() {
                    
            		from("activemq:"+queueName).
            		to("dataset:myDataSet");
                }
            });
            
            main.run(args);

        } catch (Throwable e) {
            LOG.error("Failed to connect to Fabric8MQ", e);
        }
    }

    public static int getPort() {
        return port;
    }

    public static String getQueueName() {
        return queueName;
    }
}
