/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.mq.leveldb;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ConnectionControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.File;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.Assert.*;

public class LevelDBFastEnqueueTest {
    
    static final Logger LOG = LoggerFactory.getLogger(LevelDBFastEnqueueTest.class);
    BrokerService broker;
    ActiveMQConnectionFactory connectionFactory;
    LevelDBStore store;
    Destination destination = new ActiveMQQueue("Test");
    String payloadString = new String(new byte[6*1024]);
    boolean useBytesMessage= true;
    final int parallelProducer = 20;
    Vector<Exception> exceptions = new Vector<Exception>();
    long toSend = 100000;

    // use with:
    // -Xmx4g -Dorg.apache.kahadb.journal.appender.WRITE_STAT_WINDOW=10000 -Dorg.apache.kahadb.journal.CALLER_BUFFER_APPENDER=true
    @Test
    public void testPublishNoConsumer() throws Exception {

        startBroker(true, 10);

        final AtomicLong sharedCount = new AtomicLong(toSend);
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i=0; i< parallelProducer; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        publishMessages(sharedCount, 0);
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.MINUTES);
        assertTrue("Producers done in time", executorService.isTerminated());
        assertTrue("No exceptions: " + exceptions, exceptions.isEmpty());
        long totalSent  = toSend * payloadString.length();

        double duration =  System.currentTimeMillis() - start;
        LOG.info("Duration:                " + duration + "ms");
        LOG.info("Rate:                       " + (toSend * 1000/duration) + "m/s");
        LOG.info("Total send:             " + totalSent);
        LOG.info("Total journal write: " + store.getLogAppendPosition());
        LOG.info("Journal writes %:    " + store.getLogAppendPosition() / (double)totalSent * 100 + "%");

        stopBroker();
        restartBroker(0, 1200000);
        consumeMessages(toSend);
    }

    @Test
    public void testPublishNoConsumerNoCheckpoint() throws Exception {

        toSend = 100;
        startBroker(true, 0);

        final AtomicLong sharedCount = new AtomicLong(toSend);
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i=0; i< parallelProducer; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        publishMessages(sharedCount, 0);
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.MINUTES);
        assertTrue("Producers done in time", executorService.isTerminated());
        assertTrue("No exceptions: " + exceptions, exceptions.isEmpty());
        long totalSent  = toSend * payloadString.length();

        broker.getAdminView().gc();


        double duration =  System.currentTimeMillis() - start;
        LOG.info("Duration:                " + duration + "ms");
        LOG.info("Rate:                       " + (toSend * 1000/duration) + "m/s");
        LOG.info("Total send:             " + totalSent);
        LOG.info("Total journal write: " + store.getLogAppendPosition());
        LOG.info("Journal writes %:    " + store.getLogAppendPosition() / (double)totalSent * 100 + "%");
        stopBroker();

        restartBroker(0, 0);
        consumeMessages(toSend);
    }

    private void consumeMessages(long count) throws Exception {
        ActiveMQConnection connection = (ActiveMQConnection) connectionFactory.createConnection();
        connection.setWatchTopicAdvisories(false);
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer = session.createConsumer(destination);
        for (int i=0; i<count; i++) {
            assertNotNull("got message "+ i, consumer.receive(10000));
        }
        assertNull("none left over", consumer.receive(2000));
    }

    private void restartBroker(int restartDelay, int checkpoint) throws Exception {
        stopBroker();
        TimeUnit.MILLISECONDS.sleep(restartDelay);
        startBroker(false, checkpoint);
    }

    @Before
    public void setProps() {
//        System.setProperty(Journal.CALLER_BUFFER_APPENDER, Boolean.toString(true));
//        System.setProperty(FileAppender.PROPERTY_LOG_WRITE_STAT_WINDOW, "10000");
    }

    @After
    public void stopBroker() throws Exception {
        if (broker != null) {
            broker.stop();
            broker.waitUntilStopped();
        }
//        System.clearProperty(Journal.CALLER_BUFFER_APPENDER);
//        System.clearProperty(FileAppender.PROPERTY_LOG_WRITE_STAT_WINDOW);
    }

    final double sampleRate = 100000;
    private void publishMessages(AtomicLong count, int expiry) throws Exception {
        ActiveMQConnection connection = (ActiveMQConnection) connectionFactory.createConnection();
        connection.setWatchTopicAdvisories(false);
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        MessageProducer producer = session.createProducer(destination);
        Long start = System.currentTimeMillis();
        long i = 0l;
        byte[] bytes = payloadString.getBytes();
        while ( (i=count.getAndDecrement()) > 0) {
            Message message = null;
            if (useBytesMessage) {
                message = session.createBytesMessage();
                ((BytesMessage) message).writeBytes(bytes);
            } else {
                message = session.createTextMessage(payloadString);
            }
            producer.send(message, DeliveryMode.PERSISTENT, 5, expiry);
            if (i != toSend && i%sampleRate == 0) {
                long now = System.currentTimeMillis();
                LOG.info("Remainder: " + i + ", rate: " + sampleRate * 1000 / (now - start) + "m/s" );
                start = now;
            }
        }
        connection.syncSendPacket(new ConnectionControl());
        connection.close();
    }

    public void startBroker(boolean deleteAllMessages, int checkPointPeriod) throws Exception {
        broker = new BrokerService();
        broker.setDeleteAllMessagesOnStartup(deleteAllMessages);
        store = createStore();
        broker.setPersistenceAdapter(store);
        broker.addConnector("tcp://0.0.0.0:0");
        broker.start();
        String options = "?jms.watchTopicAdvisories=false&jms.useAsyncSend=true&jms.alwaysSessionAsync=false&jms.dispatchAsync=false&socketBufferSize=131072&ioBufferSize=16384&wireFormat.tightEncodingEnabled=false&wireFormat.cacheSize=8192";
        connectionFactory = new ActiveMQConnectionFactory(broker.getTransportConnectors().get(0).getConnectUri() + options);
    }

    protected LevelDBStore createStore() {
        LevelDBStore store = new LevelDBStore();
        store.setDirectory(new File("target/activemq-data/leveldb"));
        return store;
    }

}