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
package io.fabric8.bridge;

import io.fabric8.bridge.internal.AbstractConnectorTestSupport;
import io.fabric8.bridge.model.BridgeDestinationsConfig;
import io.fabric8.bridge.model.BridgedDestination;
import io.fabric8.bridge.model.BrokerConfig;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jms.core.MessageCreator;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

public class BridgeConnectorTest extends AbstractConnectorTestSupport {

	private static final String INBOUND_SUFFIX = ".inbound";
	private BridgeConnector connector;

	@Before
	public void setUp() throws Exception {
		connector = new BridgeConnector();

		BrokerConfig localBrokerConfig = new BrokerConfig();
		localBrokerConfig.setBrokerUrl(TEST_LOCAL_BROKER_URL);
		connector.setLocalBrokerConfig(localBrokerConfig );

		BrokerConfig remoteBrokerConfig = new BrokerConfig();
		remoteBrokerConfig.setBrokerUrl(TEST_REMOTE_BROKER_URL);
		connector.setRemoteBrokerConfig(remoteBrokerConfig );
		
		BridgeDestinationsConfig outboundDestinations = new BridgeDestinationsConfig();
		outboundDestinations.setDestinations(createNewDestinations(null, null));
		connector.setOutboundDestinations(outboundDestinations );
		
		BridgeDestinationsConfig inboundDestinations = new BridgeDestinationsConfig();
		connector.setInboundDestinations(inboundDestinations);
	}

	@After
	public void tearDown() throws Exception {
		connector.destroy();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		connector.afterPropertiesSet();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMissingLocalBrokerConfig() throws Exception {
		connector.setLocalBrokerConfig(null);
		connector.afterPropertiesSet();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMissingDestinations() throws Exception {
		connector.setOutboundDestinations(null);
		connector.setInboundDestinations(null);
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
		connector.setDestinationsConfig(connector.getOutboundDestinations());
		
		// after stop
		connector.stop();
		connector.setDestinationsConfig(connector.getInboundDestinations());
		
		// after destroy
		connector.destroy();
		connector.setDestinationsConfig(connector.getInboundDestinations());
	}

	@Test
	public void testAddDestinations() throws Exception {

		connector.afterPropertiesSet();
		connector.start();
		
		connector.addDestinations(createNewDestinations(INBOUND_SUFFIX, null));

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
		
		// send messages to source destinations and verify they made it to target broker
		// send messages
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

		// send messages to staging queue on source broker and verify they made it to source destinations
		sendMessages(TEST_LOCAL_BROKER_URL, BridgeDestinationsConfig.DEFAULT_STAGING_QUEUE_NAME, TEST_SOURCES.length * TEST_BATCH_SIZE, new MessageCreator() {
			
			int index = 0;
			
			@Override
			public Message createMessage(Session session) throws JMSException {
				TextMessage message = session.createTextMessage("Test Message " + index);
				message.setStringProperty(BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER, TEST_SOURCES[index] + INBOUND_SUFFIX);
				index = ++index < TEST_SOURCES.length ? index : 0;
				return message;
			}
		});
		
		for (final String destinationName : TEST_SOURCES) {
			receiveMessages(TEST_LOCAL_BROKER_URL, destinationName + INBOUND_SUFFIX, TEST_BATCH_SIZE, new BaseMatcher<Message>() {

				@Override
				public boolean matches(Object message) {
					boolean retVal = false;
					try {
						retVal = (destinationName + INBOUND_SUFFIX).matches(((TextMessage)message).getStringProperty(BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER));
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
	public void testBidirectionalDispatch() throws Exception {
		// change target destination names for outbound destinations
		for (BridgedDestination destination : connector.getOutboundDestinations().getDestinations()) {
			destination.setTargetName(destination.getName() + INBOUND_SUFFIX);
		}
		connector.afterPropertiesSet();
		connector.start();

		// setup another connector on target broker to roundtrip the messages back to the source with inbound suffix
		final BridgeConnector targetConnector = new BridgeConnector();

		BrokerConfig localBrokerConfig = new BrokerConfig();
		localBrokerConfig.setBrokerUrl(TEST_REMOTE_BROKER_URL);
		targetConnector.setLocalBrokerConfig(localBrokerConfig );
		
		BrokerConfig remoteBrokerConfig = new BrokerConfig();
		remoteBrokerConfig.setBrokerUrl(TEST_LOCAL_BROKER_URL);
		targetConnector.setRemoteBrokerConfig(remoteBrokerConfig );
		
		BridgeDestinationsConfig inboundDestinations = new BridgeDestinationsConfig();
		targetConnector.setInboundDestinations(inboundDestinations );
		
		BridgeDestinationsConfig outboundDestinations = new BridgeDestinationsConfig();
		outboundDestinations.setDestinations(createNewDestinations(INBOUND_SUFFIX, null));
		targetConnector.setOutboundDestinations(outboundDestinations);
		
		targetConnector.afterPropertiesSet();
		targetConnector.start();

		// send messages to source destinations and verify they made it back to INBOUND_SUFFIX destinations
		for (String sourceName : TEST_SOURCES) {
			sendMessages(TEST_LOCAL_BROKER_URL, sourceName, TEST_BATCH_SIZE, null);
		}

		for (final String destinationName : TEST_SOURCES) {
			receiveMessages(TEST_LOCAL_BROKER_URL, destinationName + INBOUND_SUFFIX, TEST_BATCH_SIZE, new BaseMatcher<Message>() {

				@Override
				public boolean matches(Object message) {
					boolean retVal = false;
					try {
						retVal = (destinationName + INBOUND_SUFFIX).matches(((TextMessage)message).getStringProperty(BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER));
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
		
		targetConnector.destroy();
	}

    @Test
    public void testDispatchWithoutRemoteStagingQueue() throws Exception {
        connector.getOutboundDestinations().setUseStagingQueue(false);
        connector.afterPropertiesSet();
        connector.start();

        // send messages to source destinations and verify they made it to target broker
        // send messages
        for (String sourceName : TEST_SOURCES) {
            sendMessages(TEST_LOCAL_BROKER_URL, sourceName, TEST_NUM_MESSAGES, null);
        }
        // check if we received the expected number of messages on individual queues
        for (final String sourceName : TEST_SOURCES) {
            receiveMessages(TEST_REMOTE_BROKER_URL, sourceName, TEST_NUM_MESSAGES, new BaseMatcher<TextMessage>() {

                @Override
                public boolean matches(Object message) {
                    boolean retVal = false;
                    try {
                        retVal = ((TextMessage)message).getStringProperty(BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER).matches(sourceName);
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
        }

        // send messages to staging queue on source broker and verify they made it to source destinations
        sendMessages(TEST_LOCAL_BROKER_URL, BridgeDestinationsConfig.DEFAULT_STAGING_QUEUE_NAME, TEST_SOURCES.length * TEST_BATCH_SIZE, new MessageCreator() {

            int index = 0;

            @Override
            public Message createMessage(Session session) throws JMSException {
                TextMessage message = session.createTextMessage("Test Message " + index);
                message.setStringProperty(BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER, TEST_SOURCES[index] + INBOUND_SUFFIX);
                index = ++index < TEST_SOURCES.length ? index : 0;
                return message;
            }
        });

        for (final String destinationName : TEST_SOURCES) {
            receiveMessages(TEST_LOCAL_BROKER_URL, destinationName + INBOUND_SUFFIX, TEST_BATCH_SIZE, new BaseMatcher<Message>() {

                @Override
                public boolean matches(Object message) {
                    boolean retVal = false;
                    try {
                        retVal = (destinationName + INBOUND_SUFFIX).matches(((TextMessage)message).getStringProperty(BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER));
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
