/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.fabric.bridge.internal.AbstractConnectorTestSupport;
import org.fusesource.fabric.bridge.model.BridgeDestinationsConfig;
import org.fusesource.fabric.bridge.model.BrokerConfig;
import org.fusesource.fabric.bridge.model.RemoteBridge;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jms.core.MessageCreator;

public class GatewayConnectorTest extends AbstractConnectorTestSupport {

	private static final String TEST_REMOTE2_BROKER_URL = TEST_REMOTE_BROKER_URL.replace("remote", "remote2");
	private static final String OUTBOUND_SUFFIX = ".outbound";
	private GatewayConnector connector;

	@Before
	public void setUp() throws Exception {
		connector = new GatewayConnector();
		BrokerConfig localBrokerConfig = new BrokerConfig();
		localBrokerConfig.setBrokerUrl(TEST_LOCAL_BROKER_URL);
		connector.setLocalBrokerConfig(localBrokerConfig);
		
		BridgeDestinationsConfig inboundDestinations = new BridgeDestinationsConfig();
		connector.setInboundDestinations(inboundDestinations);
		
		BridgeDestinationsConfig outboundDestinations = new BridgeDestinationsConfig();
		outboundDestinations.setDestinations(createNewDestinations(null, null));
		connector.setOutboundDestinations(outboundDestinations);
		
		List<RemoteBridge> remoteBridges = new ArrayList<RemoteBridge>();
		RemoteBridge remoteBridge = new RemoteBridge();
		BrokerConfig remoteBrokerConfig = new BrokerConfig();
		remoteBrokerConfig.setBrokerUrl(TEST_REMOTE_BROKER_URL);
		remoteBridge.setRemoteBrokerConfig(remoteBrokerConfig);
		
		remoteBridges.add(remoteBridge);
		connector.setRemoteBridges(remoteBridges);
	}

	@After
	public void tearDown() throws Exception {
		connector.destroy();
		connector = null;
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMissingLocalBrokerConfig() throws Exception {
		connector.setLocalBrokerConfig(null);
		connector.afterPropertiesSet();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMissingDestinations() throws Exception {
		connector.setInboundDestinations(null);
		connector.setOutboundDestinations(null);
		connector.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		connector.afterPropertiesSet();
	}

	@Test
	public void testStart() throws Exception {
		connector.afterPropertiesSet();
		connector.start();
	}

	@Test
	public void testStop() throws Exception {
		connector.afterPropertiesSet();
		connector.stop();
	}

	@Test
	public void testDestroy() throws Exception {
		connector.afterPropertiesSet();
		connector.destroy();
	}

	@Test
	public void testGetDestinationsConfig() throws Exception {
		// before start
		connector.getDestinationsConfig();
		
		// after start
		connector.afterPropertiesSet();
		connector.start();
		connector.getDestinationsConfig();
	
		// after stop
		connector.stop();
		connector.getDestinationsConfig();
	
		// after destroy
		connector.destroy();
		connector.getDestinationsConfig();
	}

	@Test
	public void testSetDestinationsConfig() throws Exception {
		connector.afterPropertiesSet();
		// before start
		connector.setDestinationsConfig(connector.getDestinationsConfig());
		
		// after start
		connector.start();
		connector.setDestinationsConfig(connector.getDestinationsConfig());
	
		// after stop
		connector.stop();
		connector.setDestinationsConfig(connector.getDestinationsConfig());
	
		// after destroy
		connector.destroy();
		connector.setDestinationsConfig(connector.getDestinationsConfig());
	}

	@Test
	public void testAddDestinations() throws Exception {
		connector.afterPropertiesSet();
		connector.start();
		
		connector.addDestinations(createNewDestinations(OUTBOUND_SUFFIX, null));
	}

	@Test
	public void testRemoveDestinations() throws Exception {
		connector.afterPropertiesSet();
		connector.start();
		
		connector.removeDestinations(connector.getOutboundDestinations().getDestinations());
	}
	
	@Test
	public void testDispatch() throws Exception {
		connector.afterPropertiesSet();
		connector.start();
		
		// send messages to source destinations
		for (String sourceName : TEST_SOURCES) {
			sendMessages(TEST_LOCAL_BROKER_URL, sourceName, TEST_NUM_MESSAGES, null);
		}

		// check if we received the expected number of messages on staging queue
		receiveMessages(TEST_REMOTE_BROKER_URL, BridgeDestinationsConfig.DEFAULT_STAGING_QUEUE_NAME, TEST_NUM_MESSAGES * TEST_SOURCES.length, new BaseMatcher<TextMessage>() {

			@Override
			public boolean matches(Object message) {
				boolean retVal = false;
				try {
					retVal = ((TextMessage)message).getStringProperty(BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER).matches("source[1-3]");
				} catch (JMSException e) {
					fail(e.getMessage());
				}
				return retVal;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("TextMessage containing " + BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER + " property");
			}
		});
		
		sendMessages(TEST_LOCAL_BROKER_URL, BridgeDestinationsConfig.DEFAULT_STAGING_QUEUE_NAME, TEST_SOURCES.length * TEST_BATCH_SIZE, new MessageCreator() {
			
			int index = 0;
			
			@Override
			public Message createMessage(Session session) throws JMSException {
				TextMessage message = session.createTextMessage("Test Message " + index);
				message.setStringProperty(BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER, TEST_SOURCES[index] + OUTBOUND_SUFFIX);
				index = ++index < TEST_SOURCES.length ? index : 0;
				return message;
			}
		});
		
		for (final String destinationName : TEST_SOURCES) {
			receiveMessages(TEST_LOCAL_BROKER_URL, destinationName + OUTBOUND_SUFFIX, TEST_BATCH_SIZE, new BaseMatcher<Message>() {

				@Override
				public boolean matches(Object message) {
					boolean retVal = false;
					try {
						retVal = (destinationName + OUTBOUND_SUFFIX).matches(((TextMessage)message).getStringProperty(BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER));
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
	
	@Test
	public void testAddRemoteBridge() throws Exception {
		connector.afterPropertiesSet();
		connector.start();
		
		RemoteBridge remoteBridge = new RemoteBridge();
		
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setBrokerUrl(TEST_REMOTE2_BROKER_URL);
		remoteBridge.setRemoteBrokerConfig(brokerConfig);

		connector.addRemoteBridge(remoteBridge);
        assertTrue("Remote bridge not added", connector.getRemoteBridges().contains(remoteBridge));
	}
	
	@Test
	public void testRemoveRemoteBridge() throws Exception {
		connector.afterPropertiesSet();
		connector.start();

        RemoteBridge remoteBridge = connector.getRemoteBridges().get(0);
        connector.removeRemoteBridge(remoteBridge);
        assertFalse("Remote bridge not removed", connector.getRemoteBridges().contains(remoteBridge));
	}

}
