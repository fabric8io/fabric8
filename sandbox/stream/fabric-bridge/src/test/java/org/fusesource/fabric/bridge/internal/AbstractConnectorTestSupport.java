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
package io.fabric8.bridge.internal;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import io.fabric8.bridge.model.BridgedDestination;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

/**
 * @author Dhiraj Bokde
 *
 */
public abstract class AbstractConnectorTestSupport extends Assert {

	private static final String LOCAL_URL = "vm://local?broker.persistent=false&broker.brokerName=local";
	protected static final String TEST_QUEUE = "fabric.bridge.testQueue";
	protected static final int TEST_BATCH_SIZE = 10;
	protected static final String TEST_LOCAL_BROKER_URL = LOCAL_URL + "&jms.prefetchPolicy.queuePrefetch=" + TEST_BATCH_SIZE;
	
	private static final String REMOTE_URL = "vm://remote?broker.persistent=false&broker.brokerName=remote";
	protected static final String TEST_REMOTE_BROKER_URL = REMOTE_URL + "&jms.prefetchPolicy.queuePrefetch=" + TEST_BATCH_SIZE;

	protected static final int TEST_NUM_BATCHES = 10;
	protected static final int TEST_NUM_MESSAGES = TEST_BATCH_SIZE * TEST_NUM_BATCHES;
	protected static final int TEST_TIMEOUT = 15;
	protected static final long TEST_BATCH_TIMEOUT = 1000L;
	protected static final long TEST_PSEUDO_DISABLE = 5000L;
	protected static final long TEST_RECEIVE_TIMEOUT = 1L;
	protected static final String[] TEST_SOURCES = { "source1", "source2", "source3" };
	private static Connection localConnection;
	private static Connection remoteConnection;
	
	@BeforeClass
	public static void createStaticConnections() throws JMSException {
		localConnection = new ActiveMQConnectionFactory(TEST_LOCAL_BROKER_URL).createConnection();
		remoteConnection = new ActiveMQConnectionFactory(TEST_REMOTE_BROKER_URL).createConnection();
	}
	
	@AfterClass
	public static void stopStaticConnections() throws JMSException {
		localConnection.close();
		remoteConnection.close();
	}

	private static final MessageCreator DEFAULT_MESSAGE_CREATOR = new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage("Test Message");
			}
		};

	protected void sendMessages(String brokerUrl, String destinationName, int numMessages, MessageCreator creator) {
		PooledConnectionFactory connectionFactory = new PooledConnectionFactory(brokerUrl);
		JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		
		MessageCreator msgCreator = creator != null ? creator : DEFAULT_MESSAGE_CREATOR;
		for (int i = 0; i < numMessages; i++) {
			jmsTemplate.send(destinationName, msgCreator);
		}
		connectionFactory.stop();
	}

	protected void receiveMessages(String brokerUrl, String destinationName, int numMessages, Matcher<? extends Message> matcher) {
		PooledConnectionFactory connectionFactory = new PooledConnectionFactory(brokerUrl);
		JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		jmsTemplate.setReceiveTimeout(TEST_TIMEOUT * 1000L);

		int nMessages = 0;
		for (int i = 0; i < numMessages; i++) {
			Message message = jmsTemplate.receive(destinationName);
			if (message == null) {
				break;
			}
			if (matcher != null && !matcher.matches(message)) {
				fail("Unexpected message " + message);
			}
			nMessages++;
		}
		connectionFactory.stop();
		
		assertEquals("Number of received messages on " + destinationName + " do not match", numMessages, nMessages);
	}

	protected List<BridgedDestination> createNewDestinations(String suffix, String targetSuffix) {
		List<BridgedDestination> newDestinations = new ArrayList<BridgedDestination>();
		for (String name : TEST_SOURCES) {
			BridgedDestination dest = new BridgedDestination();
			dest.setName((suffix != null) ? (name + suffix) : name);
			if (targetSuffix != null) {
				dest.setTargetName(name + targetSuffix);
			}
			newDestinations.add(dest);
		}
		return newDestinations ;
	}

}