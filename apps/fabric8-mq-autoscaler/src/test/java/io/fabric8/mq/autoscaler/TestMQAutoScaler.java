/*
 *
 *  * Copyright 2005-2014 Red Hat, Inc.
 *  * Red Hat licenses this file to you under the Apache License, version
 *  * 2.0 (the "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  * implied.  See the License for the specific language governing
 *  * permissions and limitations under the License.
 *
 */

package io.fabric8.mq.autoscaler;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.transport.TransportListener;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static io.fabric8.mq.autoscaler.EnvUtils.getEnv;
                       
                       /*
 *
 *  * Copyright 2005-2014 Red Hat, Inc.
 *  * Red Hat licenses this file to you under the Apache License, version
 *  * 2.0 (the "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  * implied.  See the License for the specific language governing
 *  * permissions and limitations under the License.
 *  
 */

public class TestMQAutoScaler extends Thread {

    private String brokerURL = "tcp://localhost:61616";
    private int numberOfConsumers = 2;
    private int numberOfProducers = 2;
    private int numberOfMessagesInABatch = 1000;
    private int numberOfDestinations = 5;
    private int rumTimeMinutes = 10;
    private String destinationName = "test.scaling";

    private AtomicLong producerMessageCount = new AtomicLong();
    private AtomicLong consumerMessageCount = new AtomicLong();

    private Map<String, Connection> consumerConnections = new HashMap<>();
    private Map<String, Connection> producerConnections = new HashMap<>();
    private ExecutorService executorService;

    private List<ActiveMQDestination> destinations = new ArrayList<>();

    public int getRumTimeMinutes() {
        return rumTimeMinutes;
    }

    public void setRumTimeMinutes(int rumTimeMinutes) {
        this.rumTimeMinutes = rumTimeMinutes;
    }

    public String getBrokerURL() {
        return brokerURL;
    }

    public void setBrokerURL(String brokerURL) {
        this.brokerURL = brokerURL;
    }

    public int getNumberOfConsumers() {
        return numberOfConsumers;
    }

    public void setNumberOfConsumers(int numberOfConsumers) {
        this.numberOfConsumers = numberOfConsumers;
    }

    public int getNumberOfProducers() {
        return numberOfProducers;
    }

    public void setNumberOfProducers(int numberOfProducers) {
        this.numberOfProducers = numberOfProducers;
    }

    public int getNumberOfMessagesInABatch() {
        return numberOfMessagesInABatch;
    }

    public void setNumberOfMessagesInABatch(int numberOfMessagesInABatch) {
        this.numberOfMessagesInABatch = numberOfMessagesInABatch;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public int getNumberOfDestinations() {
        return numberOfDestinations;
    }

    public void setNumberOfDestinations(int numberOfDestinations) {
        this.numberOfDestinations = numberOfDestinations;
    }

    public void init() throws Exception {

        String url = "failover:(" + getBrokerURL() + ")";
        for (int i = 0; i < getNumberOfDestinations(); i++) {
            String destinationName = getDestinationName() + "." + i;
            destinations.add(new ActiveMQQueue(destinationName));
        }

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(url);
        for (ActiveMQDestination destination : destinations) {
            createConsumers(factory, destination);
            createProducers(factory, destination);
        }

        executorService = Executors.newFixedThreadPool(destinations.size() * producerConnections.size());
    }

    public void shutDown() {
        if (executorService != null) {
            executorService.shutdown();
        }
        destroyProducers();
        destroyConsumers();
    }

    public void run() {

        try {
            long currentTime = System.currentTimeMillis();
            long totalTime = System.currentTimeMillis() + (getRumTimeMinutes() * 60 * 1000);
            final AtomicInteger batch = new AtomicInteger(1);

            while (currentTime < totalTime) {
                for (final Connection producer : producerConnections.values()) {
                    final CountDownLatch latch = new CountDownLatch(producerConnections.size());
                    Runnable worker = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                for (ActiveMQDestination destination : destinations) {
                                    runBatch(producer, destination, batch.get(), numberOfMessagesInABatch);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                latch.countDown();
                            }
                        }
                    };
                    executorService.submit(worker);
                }
                batch.incrementAndGet();
                Thread.sleep(5000);
                System.err.println("Total sent = " + producerMessageCount + " Total received = " + consumerMessageCount);
                currentTime = System.currentTimeMillis();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void createConsumers(ActiveMQConnectionFactory factory, ActiveMQDestination destination) throws Exception {
        for (int i = 0; i < numberOfConsumers; i++) {
            String name = destination.getPhysicalName() + ".Consumer:" + i;
            ActiveMQConnection connection = createConsumer(factory, name, destination);
            consumerConnections.put(name, connection);
        }
    }

    public void destroyConsumers() {
        for (Connection connection : consumerConnections.values()) {
            try {
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        consumerConnections.clear();
    }

    public void createProducers(ActiveMQConnectionFactory factory, ActiveMQDestination destination) throws Exception {
        for (int i = 0; i < numberOfConsumers; i++) {
            String name = destination.getPhysicalName() + ".Producer:" + i;
            ActiveMQConnection connection = createProducer(factory, name, destination);
            producerConnections.put(name, connection);
        }
    }

    public void destroyProducers() {
        for (Connection connection : producerConnections.values()) {
            try {
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        producerConnections.clear();
    }

    private ActiveMQConnection createConsumer(ActiveMQConnectionFactory connectionFactory, String name, ActiveMQDestination destination) throws JMSException {
        ActiveMQConnection connection = (ActiveMQConnection) connectionFactory.createConnection();
        connection.addTransportListener(new TestTransportListener(name));
        connection.setClientID(name);
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                consumerMessageCount.incrementAndGet();
            }
        });

        return connection;
    }

    private ActiveMQConnection createProducer(ActiveMQConnectionFactory connectionFactory, String name, ActiveMQDestination destination) throws JMSException {
        ActiveMQConnection connection = (ActiveMQConnection) connectionFactory.createConnection();
        connection.addTransportListener(new TestTransportListener(name));
        connection.setClientID(name);
        connection.start();
        return connection;
    }

    private void runBatch(Connection connection, ActiveMQDestination destination, int batch, int total) throws Exception {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(destination);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        for (int i = 0; i < total; i++) {
            TextMessage textMessage = session.createTextMessage(connection.getClientID() + "batch:" + batch + " test message " + i);
            producer.send(textMessage);
            producerMessageCount.incrementAndGet();
            Thread.sleep(1000);
        }
        session.close();
    }

    private class TestTransportListener implements TransportListener {
        private final String name;

        TestTransportListener(String name) {
            this.name = name;
        }

        /*
* TransportListener implementation
*/
        @Override
        public void onCommand(Object o) {
            //ignore
        }

        @Override
        public void onException(IOException e) {
            System.err.println(name + " Got an exception " + e);
        }

        @Override
        public void transportInterupted() {
            System.err.println(name + " transport interrupted");
        }

        @Override
        public void transportResumed() {
            System.err.println(name + " transport resumed");
        }
    }

    public static void main(String[] args) {
        try {

            String AMQ_PORT = getEnv("AMQ_PORT", 6161).trim();
            //String AMQ_HOST = getEnv("AMQ_HOST", "dockerhost").trim();
            String AMQ_HOST = "localhost";
            String DESTINATIONS = getEnv("DESTINATIONS", 10).trim();
            String RUNNING_TIME = getEnv("RUNNING_TIME", 1).trim();

            TestMQAutoScaler testMQAutoScaler = new TestMQAutoScaler();

            testMQAutoScaler.setBrokerURL("tcp://" + AMQ_HOST + ":" + AMQ_PORT);
            testMQAutoScaler.setNumberOfDestinations(Integer.valueOf(DESTINATIONS));
            testMQAutoScaler.setRumTimeMinutes(Integer.valueOf(RUNNING_TIME));
            testMQAutoScaler.init();
            testMQAutoScaler.start();

            testMQAutoScaler.join();
            testMQAutoScaler.shutDown();
            System.err.println("");
            System.err.println("");
            System.err.println("Finished.");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
