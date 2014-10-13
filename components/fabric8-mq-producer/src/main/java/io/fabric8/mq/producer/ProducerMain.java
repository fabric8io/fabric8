package io.fabric8.mq.producer;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

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

    private static String host;

    private static int port;

    private static int interval;
    
    private static String queueName;
    
    private static int messageSize;

    private static long messageCount;
    
    private static String messageBody;

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
            
    		char[] chars = new char[messageSize];
    		Arrays.fill(chars, 'a');
    		
    		messageBody = new String(chars);
    		
            // create a camel route to produce messages to our queue
            org.apache.camel.main.Main main = new org.apache.camel.main.Main();

            String brokerURL = "tcp://" + host + ":" + port;
            System.out.println("Connecting to brokerURL: " + brokerURL);

            main.bind("activemq", ActiveMQComponent.activeMQComponent(brokerURL));
            main.bind("myDataSet", createDataSet());
            main.bind("messageBody", getMessageBody());
            
            main.enableHangupSupport();
            
            main.addRouteBuilder(new RouteBuilder() {
                public void configure() {
                    
                	from("dataset:myDataSet?produceDelay="+interval).
                	process(new Processor() {
                		public void process(Exchange exchange) throws Exception {

                			exchange.getIn().setBody(getMessageBody());
                		}
                	}).
                	log("Sending message at ${date:now:HH:mm:SS:sss}").
                	to("activemq:"+queueName);
                }
            });
            
            main.run(args);
        } catch (Throwable e) {
            LOG.error("Failed to connect to Fabric8MQ", e);
        }
    }

	static DataSet createDataSet() {
		SimpleDataSet dataSet = new SimpleDataSet();
		dataSet.setSize(messageCount);
		
		return dataSet;
	}
	
	public static String getMessageBody (){
		return messageBody;
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