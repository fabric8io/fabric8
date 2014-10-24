package io.fabric8.mq.producer;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

import io.fabric8.common.util.Systems;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.dataset.DataSet;
import org.apache.camel.component.dataset.SimpleDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerMain {
    private static final Logger LOG = LoggerFactory.getLogger(ProducerMain.class);
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final String DEFAULT_PORT = "61616";

    private static String host;

    private static int port;

    private static int interval;
    
    private static String queueName;
    
    private static int messageSize;

    private static long messageCount;

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

                queueName = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_QUEUENAME");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_QUEUENAME", "TEST.FOO") : result;
                        return result;
                    }
                });
                
                String intervalStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_INTERVAL");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_INTERVAL", "0") : result;
                        return result;
                    }
                });
                if (intervalStr != null && intervalStr.length() > 0) {
                	interval = Integer.parseInt(intervalStr);
                }

                String messageSizeInBytesStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
                	@Override
                	public String run() {
                		String result = System.getenv("AMQ_MESSAGE_SIZE_BYTES");
                		result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_MESSAGE_SIZE_BYTES", "1024") : result;
                		return result;
                	}
                });
                if (messageSizeInBytesStr != null && messageSizeInBytesStr.length() > 0) {
                	messageSize = Integer.parseInt(messageSizeInBytesStr);
                }

                String messageCountStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
                	@Override
                	public String run() {
                		String result = System.getenv("AMQ_MESSAGE_COUNT_LONG");
                		result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_MESSAGE_COUNT_LONG", "10000") : result;
                		return result;
                	}
                });
                if (messageCountStr != null && messageCountStr.length() > 0) {
                	messageCount= Long.parseLong(messageCountStr);
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

            if (queueName == null) {
            	queueName = "TEST.FOO";
            }
            
            if (messageSize <= 1) {
            	messageSize = 1024;
            }

            if (messageCount <= 0) {
            	messageCount = 10000;
            }
    		
            System.out.println("Using broker host " + host + " from $" + hostEnvVar + " and port " + port + " from $" + portEnvVar);

            // create a camel route to produce messages to our queue
            org.apache.camel.main.Main main = new org.apache.camel.main.Main();

            String brokerURL = "tcp://" + host + ":" + port;
            System.out.println("Connecting to brokerURL: " + brokerURL);

            main.bind("activemq", ActiveMQComponent.activeMQComponent(brokerURL));
            main.bind("myDataSet", createDataSet());
            
            main.enableHangupSupport();
            
            main.addRouteBuilder(new RouteBuilder() {
                public void configure() {
                    
                	from("dataset:myDataSet?produceDelay="+interval).
                	to("activemq:"+queueName);
                }
            });
            
            main.run(args);
        } catch (Throwable e) {
            LOG.error("Failed to connect to Fabric8 MQ", e);
        }
    }

	static DataSet createDataSet() {

        char[] chars = new char[messageSize];
        Arrays.fill(chars, 'a');
            
        String messageBody = new String(chars);

        SimpleDataSet dataSet = new SimpleDataSet();
		dataSet.setSize(messageCount);
		dataSet.setDefaultBody(messageBody);

		return dataSet;
	}
    
    public static int getPort() {
        return port;
    }

    public static int getInterval() {
        return interval;
    }
    
    public static String getQueueName() {
        return queueName;
    }

	public static int getMessageSize() {
		return messageSize;
	}

	public static long getMessageCount() {
		return messageCount;
	}
}