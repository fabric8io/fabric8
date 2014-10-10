package io.fabric8.mq.consumer;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Date;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static int port;
    
    private static String queueName;
    
    public static void main(String args[]) {
        try {
            try {
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
                
                queueName = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_INTERVAL");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_QUEUENAME", "50") : result;
                        return result;
                    }
                });
                
            } catch (Throwable e) {
                LOG.warn("Failed to look up System properties for host and port", e);
            }

            if (port <= 0) {
                port = 61616;
            }
            
            if (queueName == null) {
            	queueName = "TEST.FOO";
            }
            
            // create a camel route to consume messages from our queue
            org.apache.camel.main.Main main = new org.apache.camel.main.Main();
            
            main.bind("activemq", ActiveMQComponent.activeMQComponent("tcp://localhost:"+port));
            main.enableHangupSupport();
            
            main.addRouteBuilder(new RouteBuilder() {
                public void configure() {
                    
            		from("activemq:"+queueName).process(new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            LOG.info("Received message at " + new Date());
                        }
                    });
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