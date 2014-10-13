package io.fabric8.mq.consumer;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerMain {
    private static final Logger LOG = LoggerFactory.getLogger(ConsumerMain.class);
    public static final String DEFAULT_HOST = "127.0.0.1";

    private static String host;
    private static int port;
    
    private static int prefetch;
    
    private static String queueName;
    
    private static int slowConsumerMillis;
    
    public static void main(String args[]) {
        try {
            try {
                host = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_HOST");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_HOST", DEFAULT_HOST) : result;
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
                
                String slowConsumerMillisStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
	                @Override
	                public String run() {
	                	String result = System.getenv("AMQ_SLOW_CONSUMER_MILLIS");
	                	result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_SLOW_CONSUMER_MILLIS", "0") : result;
	                	return result;
	                }
                });
                if (slowConsumerMillisStr != null && slowConsumerMillisStr.length() > 0) {
                	slowConsumerMillis = Integer.parseInt(slowConsumerMillisStr);
                }
                
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
            
            // create a camel route to consume messages from our queue
            org.apache.camel.main.Main main = new org.apache.camel.main.Main();
            String brokerURL = "tcp://" + host + ":" + port + "?jms.prefetchPolicy.all=" + prefetch;
            System.out.println("Connecting to brokerURL " + brokerURL);
            main.bind("activemq", ActiveMQComponent.activeMQComponent(brokerURL));
            main.enableHangupSupport();
            
            main.addRouteBuilder(new RouteBuilder() {
                public void configure() {
                    
            		from("activemq:"+queueName).
            		process(new Processor() {
            			public void process(Exchange exchange) throws Exception {
            				Thread.sleep(slowConsumerMillis);
            			}
            		}).
            		log("Received message at ${date:now:HH:mm:SS:sss}");
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
    
    public static int getSlowConsumerMillis() {
    	return slowConsumerMillis;
    }
}