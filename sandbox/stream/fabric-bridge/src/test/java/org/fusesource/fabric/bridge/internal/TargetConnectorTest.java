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


import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import io.fabric8.bridge.internal.TargetConnector;
import io.fabric8.bridge.model.BridgeDestinationsConfig;
import io.fabric8.bridge.model.BrokerConfig;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jms.core.MessageCreator;

/**
 * @author Dhiraj Bokde
 *
 */
public class TargetConnectorTest extends AbstractConnectorTestSupport {

	private TargetConnector connector;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		connector = new TargetConnector();
		
		// default configuration uses the same broker as source and target
		connector.setRemoteBrokerConfig(null);
		
		BrokerConfig targetBrokerConfig = new BrokerConfig();
		targetBrokerConfig.setBrokerUrl(TEST_LOCAL_BROKER_URL);
		connector.setLocalBrokerConfig(targetBrokerConfig );
		
		BridgeDestinationsConfig targetDestinations = new BridgeDestinationsConfig();
		connector.setInboundDestinations(targetDestinations);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		connector.destroy();
		connector = null;
	}
	
	@Test
	public void testAfterPropertiesSet() throws Exception {
		connector.afterPropertiesSet();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMissingTargetBrokerConfig() throws Exception {
		connector.setLocalBrokerConfig(null);
		
		connector.afterPropertiesSet();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testMissingTargetDestinations() throws Exception {
		connector.setInboundDestinations(null);
		
		connector.afterPropertiesSet();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testMissingSourceBrokerConfig() throws Exception {
		connector.getInboundDestinations().setDefaultStagingLocation(false);

		connector.afterPropertiesSet();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testIgnoredSourceBrokerConfig() throws Exception {
		BrokerConfig sourceBrokerConfig = new BrokerConfig();
		sourceBrokerConfig.setBrokerUrl(TEST_LOCAL_BROKER_URL);
		connector.setRemoteBrokerConfig(sourceBrokerConfig);
		
		connector.afterPropertiesSet();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidSourceBrokerConfig() throws Exception {
		connector.getInboundDestinations().setDefaultStagingLocation(false);
		BrokerConfig sourceBrokerConfig = new BrokerConfig();
		connector.setRemoteBrokerConfig(sourceBrokerConfig);
		
		connector.afterPropertiesSet();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testInvalidTargetBrokerConfig() throws Exception {
		connector.getLocalBrokerConfig().setBrokerUrl(null);
		
		connector.afterPropertiesSet();
	}

	@Test
	public void testStart() throws Exception {
		connector.afterPropertiesSet();
		connector.start();
		
		// test re-start
		connector.start();
	}
	
	@Test
	public void testStop() {
		// test stop without starting
		connector.stop();
		
		// start then stop
		connector.start();
		connector.stop();
	}

	@Test
	public void testDestroy() throws Exception {
		connector.afterPropertiesSet();
		// test destroy without starting
		connector.destroy();
		
		// start, stop, destroy
		connector.start();
		connector.stop();
		connector.destroy();
		
		// start, destroy
		connector.start();
		connector.destroy();
	}

	@Test
	public void testGetDestinationsConfig() {
		assertNotNull("Null destinations config", connector.getDestinationsConfig());
	}

	@Test
	public void testSetDestinationsConfig() throws Exception {
		connector.afterPropertiesSet();
		// before start
		connector.setDestinationsConfig(connector.getInboundDestinations());
		
		// after start
		connector.start();
		connector.setDestinationsConfig(connector.getInboundDestinations());
		
		// after stop
		connector.stop();
		connector.setDestinationsConfig(connector.getInboundDestinations());
		
		// after destroy
		connector.destroy();
		connector.setDestinationsConfig(connector.getInboundDestinations());
	}

	@Test
	public void testDispatch() throws Exception {
		connector.afterPropertiesSet();
		connector.start();
		
		sendMessages(TEST_LOCAL_BROKER_URL, BridgeDestinationsConfig.DEFAULT_STAGING_QUEUE_NAME, TEST_SOURCES.length * TEST_BATCH_SIZE, new MessageCreator() {
			
			int index = 0;
			
			@Override
			public Message createMessage(Session session) throws JMSException {
				TextMessage message = session.createTextMessage("Test Message " + index);
				message.setStringProperty(BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER, TEST_SOURCES[index]);
				index = ++index < TEST_SOURCES.length ? index : 0;
				return message;
			}
		});
		
		for (final String destinationName : TEST_SOURCES) {
			receiveMessages(TEST_LOCAL_BROKER_URL, destinationName, TEST_BATCH_SIZE, new BaseMatcher<Message>() {

				@Override
				public boolean matches(Object message) {
					boolean retVal = false;
					try {
						retVal = destinationName.matches(((TextMessage)message).getStringProperty(BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER));
					} catch (JMSException e) {
						fail(e.getMessage());
					}
					return retVal;
				}

				@Override
				public void describeTo(Description desc) {
					desc.appendText("Message contains property " + BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER);
				}
			});
		}
	}

}
