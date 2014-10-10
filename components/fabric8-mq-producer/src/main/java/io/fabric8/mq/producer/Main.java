package io.fabric8.mq.producer;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Date;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static int port;

    private static int interval;
    
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
                
                String intervalStr = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String result = System.getenv("AMQ_INTERVAL");
                        result = (result == null || result.isEmpty()) ? System.getProperty("org.apache.activemq.AMQ_INTERVAL", "50") : result;
                        return result;
                    }
                });
                if (intervalStr != null && intervalStr.length() > 0) {
                	interval = Integer.parseInt(intervalStr);
                }


            } catch (Throwable e) {
                LOG.warn("Failed to look up System properties for host and port", e);
            }

            if (port <= 0) {
                port = 61616;
            }
            
            if (queueName == null) {
            	queueName = "TEST.FOO";
            }
            
            if (interval <= 0) {
            	interval = 50;
            }
            
            // Create a ConnectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:"+port);
     
            // Create a Connection
            Connection connection = connectionFactory.createConnection();
            connection.start();
     
            // Create a Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
     
            // Create the destination Queue
            Destination destination = session.createQueue(queueName);
     
            // Create a MessageProducer from the Session to the Topic or Queue
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
     
            // Create a messages
            Message sentMessage = session.createMessage();
     
            
            try {
            	while (true) {
               	 // Tell the producer to send the message
                   producer.send(sentMessage);
                   LOG.info("Sending a message to Fabric8 MQ at " + new Date());
                   Thread.sleep(interval);
               }
               
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
        } catch (Throwable e) {
            LOG.error("Failed to connect to Fabric8MQ", e);
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

    public static int getPort() {
        return port;
    }

    public static int getInterval() {
        return interval;
    }
    
    public static String getQueueName() {
        return queueName;
    }
}