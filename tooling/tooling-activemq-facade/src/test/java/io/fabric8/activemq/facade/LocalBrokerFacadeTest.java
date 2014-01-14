/*
 * Copyright 2010 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.fabric8.activemq.facade;

import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Destination;

import io.fabric8.itests.paxexam.support.Provision;
import io.fabric8.itests.paxexam.support.WaitForConditionTask;
import org.apache.activemq.broker.jmx.ProducerViewMBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

import static org.junit.Assert.*;

public class LocalBrokerFacadeTest extends EmbeddedBrokerTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(LocalBrokerFacadeTest.class);

    protected final String TOPIC_NAME = "LocalBrokerFacadeTest.Topic";
    protected final String QUEUE_NAME = "LocalBrokerFacadeTest.Queue";

    protected LocalBrokerFacade brokerFacade;

    @Before
    public void setUp() throws Exception {
        if (broker == null) {
            broker = createBroker();
        }
        startBroker();

        connectionFactory = createConnectionFactory();
        connection = createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        brokerFacade = new LocalBrokerFacade(broker);
    }

    @After
    public void tearDown() throws Exception {
        if (broker != null) {
            broker.stop();
            broker.waitUntilStopped();
        }
    }

    @Test
    public void testConstructor() throws Exception {
        assertNotNull(brokerFacade);
        assertEquals("localhost", brokerFacade.getBrokerName());
    }

    @Test
    public void testGetQueueProducers() throws Exception {
        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 0 == brokerFacade.getQueueProducers(QUEUE_NAME).size();
            }
        }, 30000L));

        Destination destination = session.createQueue(QUEUE_NAME);
        MessageProducer producer1 = session.createProducer(destination);
        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 1 == brokerFacade.getQueueProducers(QUEUE_NAME).size();
            }
        }, 30000L));

        producer1.close();
        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 0 == brokerFacade.getQueueProducers(QUEUE_NAME).size();
            }
        }, 30000L));
        MessageProducer producer2 = session.createProducer(destination);
        MessageProducer producer3 = session.createProducer(destination);
        MessageProducer producer4 = session.createProducer(destination);

        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 3 == brokerFacade.getQueueProducers(QUEUE_NAME).size();
            }
        }, 30000L));
        producer2.close();
        producer3.close();
        producer4.close();
        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 0 == brokerFacade.getQueueProducers(QUEUE_NAME).size();
            }
        }, 30000L));
    }

    @Test
    public void testGetQueueProducersWithDynamicProducers() throws Exception {
        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 0 == brokerFacade.getQueueProducers(QUEUE_NAME).size();
            }
        }, 30000L));

        Destination destination = session.createQueue(QUEUE_NAME);
        MessageProducer producer1 = session.createProducer(destination);

        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 1 == brokerFacade.getQueueProducers(QUEUE_NAME).size();
            }
        }, 30000L));

        MessageProducer producer2 = session.createProducer(null);
        producer2.send(destination, session.createTextMessage("Dynamic Producer Made Me."));

        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 2 == brokerFacade.getQueueProducers(QUEUE_NAME).size();
            }
        }, 30000L));

        for(ProducerViewMBean bean : brokerFacade.getQueueProducers(QUEUE_NAME)) {
            LOG.debug("Got bean for producer " + bean.getSessionId() + " on dest = " + bean.getDestinationName());
        }

        producer1.close();
        producer2.close();

        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 0 == brokerFacade.getQueueProducers(QUEUE_NAME).size();
            }
        }, 30000L));
    }

    @Test
    public void testGetTopicProducers() throws Exception {
        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 0 == brokerFacade.getTopicProducers(TOPIC_NAME).size();
            }
        }, 30000L));

        Destination destination = session.createTopic(TOPIC_NAME);
        MessageProducer producer1 = session.createProducer(destination);

        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 1 == brokerFacade.getTopicProducers(TOPIC_NAME).size();
            }
        }, 30000L));
        producer1.close();

        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 0 == brokerFacade.getTopicProducers(TOPIC_NAME).size();
            }
        }, 30000L));

        MessageProducer producer2 = session.createProducer(destination);
        MessageProducer producer3 = session.createProducer(destination);
        MessageProducer producer4 = session.createProducer(destination);

        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 3 == brokerFacade.getTopicProducers(TOPIC_NAME).size();
            }
        }, 30000L));
        producer2.close();
        producer3.close();
        producer4.close();

        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 0 == brokerFacade.getTopicProducers(TOPIC_NAME).size();
            }
        }, 30000L));
    }


    @Test
    public void testGetTopicProducersWithDynamicProducers() throws Exception {
        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 0 == brokerFacade.getTopicProducers(TOPIC_NAME).size();
            }
        }, 30000L));

        Destination destination = session.createTopic(TOPIC_NAME);
        MessageProducer producer1 = session.createProducer(destination);

        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 1 == brokerFacade.getTopicProducers(TOPIC_NAME).size();
            }
        }, 30000L));

        MessageProducer producer2 = session.createProducer(null);
        producer2.send(destination, session.createTextMessage("Dynamic Producer Made Me."));

        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 2 == brokerFacade.getTopicProducers(TOPIC_NAME).size();
            }
        }, 30000L));

        producer1.close();
        producer2.close();

        Provision.waitForCondition(new WaitForConditionTask(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return 1 == brokerFacade.getTopicProducers(TOPIC_NAME).size();
            }
        }, 30000L));
    }
}
