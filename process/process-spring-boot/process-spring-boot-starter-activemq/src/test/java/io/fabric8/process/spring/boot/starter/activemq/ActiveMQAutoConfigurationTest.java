/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.process.spring.boot.starter.activemq;

import org.apache.activemq.command.ActiveMQQueue;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableAutoConfiguration
@SpringApplicationConfiguration(classes = ActiveMQAutoConfigurationTest.class)
public class ActiveMQAutoConfigurationTest extends Assert {

    @Autowired
    ConnectionFactory connectionFactory;

    @Autowired
    JmsTemplate jmsTemplate;

    @Test
    public void shouldCreateJmsConnectionFactory() {
        assertNotNull(connectionFactory);
    }

    @Test
    public void shouldInjectFabricConnectionFactoryIntoJmsTemplate() throws JMSException {
        assertSame(connectionFactory, jmsTemplate.getConnectionFactory());
    }

    @Test
    public void shouldSendMessageToTheQueue() throws JMSException {
        // Given
        Destination queue = new ActiveMQQueue("queue");
        String message = "message";

        // When
        jmsTemplate.convertAndSend(queue, message);
        String messageReceived = (String) jmsTemplate.receiveAndConvert(queue);

        // Then
        assertEquals(message, messageReceived);
    }

}