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


import io.fabric8.bridge.MessageConverter;
import io.fabric8.bridge.model.BridgeDestinationsConfig;
import io.fabric8.bridge.model.BridgedDestination;
import io.fabric8.bridge.model.BrokerConfig;
import io.fabric8.bridge.model.DispatchPolicy;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Dhiraj Bokde
 */
public class SourceConnectorTest extends AbstractConnectorTestSupport {

	private static final Logger LOG = LoggerFactory.getLogger(SourceConnectorTest.class);
	private static final String TEST_HEADER = SourceConnectorTest.class.getName()+".testHeader";
	private static final String TEST_VALUE = SourceConnectorTest.class.getName()+".testValue";

	private SourceConnector connector;

	@Before
	public void setUp() throws Exception {
		connector = new SourceConnector();

		BrokerConfig sourceBrokerConfig = new BrokerConfig();
		sourceBrokerConfig.setBrokerUrl(TEST_LOCAL_BROKER_URL);
		connector.setLocalBrokerConfig(sourceBrokerConfig);

		BrokerConfig targetBrokerConfig = new BrokerConfig();
		targetBrokerConfig.setBrokerUrl(TEST_REMOTE_BROKER_URL);
		connector.setRemoteBrokerConfig(targetBrokerConfig );

		BridgeDestinationsConfig destinationsConfig = new BridgeDestinationsConfig();
		destinationsConfig.setDestinations(createNewDestinations(null, null));
		connector.setOutboundDestinations(destinationsConfig);
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
		testAfterPropertiesSet();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMissingLocalDestinations() throws Exception {
		connector.setOutboundDestinations(null);
		testAfterPropertiesSet();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMissingRemoteBrokerConfig() throws Exception {
		connector.setRemoteBrokerConfig(null);
		testAfterPropertiesSet();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testIgnoredRemoteBrokerConfig() throws Exception {
		connector.getOutboundDestinations().setDefaultStagingLocation(false);
		testAfterPropertiesSet();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testInvalidLocalBrokerConfig() throws Exception {
		connector.getLocalBrokerConfig().setBrokerUrl(null);
		testAfterPropertiesSet();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testInvalidRemoteBrokerConfig() throws Exception {
		connector.getRemoteBrokerConfig().setBrokerUrl(null);
		testAfterPropertiesSet();
	}

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidNoStagingQueue() throws Exception {
        connector.getRemoteBrokerConfig().setBrokerUrl(null);
        connector.getOutboundDestinations().setUseStagingQueue(false);
        testAfterPropertiesSet();
    }

	@Test
	public void testStart() throws Exception {
		connector.afterPropertiesSet();
		connector.start();

		// test restart
		connector.start();
	}

	@Test
	public void testDispatch() throws Exception {
		// start connector
		connector.afterPropertiesSet();
		connector.start();

		final long startNanos = System.nanoTime();
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

		final long stopNanos = System.nanoTime();
		LOG.info("Test took " + TimeUnit.NANOSECONDS.toMillis(stopNanos - startNanos) + " milliseconds");
	}

	@Test
	public void testDispatchWithMessageConverter() throws Exception {
		connector.getOutboundDestinations().getDispatchPolicy().setMessageConverter(new MessageConverter() {
			@Override
			public Message convert(Message message) throws JMSException {
				message.setStringProperty(TEST_HEADER, TEST_VALUE);
				return message;
			}
		});

		// start connector
		connector.afterPropertiesSet();
		connector.start();

		final long startNanos = System.nanoTime();
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
					TextMessage textMessage = (TextMessage)message;
					retVal = textMessage.getStringProperty(BridgeDestinationsConfig.DEFAULT_DESTINATION_NAME_HEADER).matches("source[1-3]")
						&& TEST_VALUE.equals(textMessage.getStringProperty(TEST_HEADER));
				} catch (JMSException e) {
					fail(e.getMessage());
				}
				return retVal;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("TextMessage containing " + TEST_HEADER + " property");
			}
		});

		final long stopNanos = System.nanoTime();
		LOG.info("Test took " + TimeUnit.NANOSECONDS.toMillis(stopNanos - startNanos) + " milliseconds");
	}

	@Test
	public void testDispatchWithDefaultListener() throws Exception {

		// configure all test destinations to use DefaultMessageListenerContainer
		DispatchPolicy dispatchPolicy = new DispatchPolicy();
		dispatchPolicy.setBatchSize(0);
		dispatchPolicy.setBatchTimeout(0);
		for (BridgedDestination destination :
			connector.getDestinationsConfig().getDestinations()) {
			destination.setDispatchPolicy(dispatchPolicy);
		}

		testDispatch();
	}

	@Test
	public void testDispatchWithConcurrentConsumers() throws Exception {

		connector.getOutboundDestinations().getDispatchPolicy().setConcurrentConsumers(TEST_NUM_BATCHES);
		testDispatch();

	}

	@Test
	public void testDispatchWithLocalBroker() throws Exception {

		connector.getOutboundDestinations().setDefaultStagingLocation(false);
		connector.setRemoteBrokerConfig(null);
		// start connector
		connector.afterPropertiesSet();
		connector.start();

		final long startNanos = System.nanoTime();
		// send messages
		for (String sourceName : TEST_SOURCES) {
			sendMessages(TEST_LOCAL_BROKER_URL, sourceName, TEST_NUM_MESSAGES, null);
		}
		// check if we received the expected number of messages on staging queue
		receiveMessages(TEST_LOCAL_BROKER_URL, BridgeDestinationsConfig.DEFAULT_STAGING_QUEUE_NAME, TEST_NUM_MESSAGES * TEST_SOURCES.length, new BaseMatcher<TextMessage>() {

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

		final long stopNanos = System.nanoTime();
		LOG.info("Test took " + TimeUnit.NANOSECONDS.toMillis(stopNanos - startNanos) + " milliseconds");

	}

    @Test
    public void testDispatchWithoutRemoteStagingQueue() throws Exception {
        // disable staging queue
        connector.getDestinationsConfig().setUseStagingQueue(false);

		// start connector
		connector.afterPropertiesSet();
		connector.start();

		final long startNanos = System.nanoTime();
		// send messages
		for (String sourceName : TEST_SOURCES) {
			sendMessages(TEST_LOCAL_BROKER_URL, sourceName, TEST_NUM_MESSAGES, null);
		}
		// check if we received the expected number of messages on every target queue
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

		final long stopNanos = System.nanoTime();
		LOG.info("Test took " + TimeUnit.NANOSECONDS.toMillis(stopNanos - startNanos) + " milliseconds");
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
		connector.setDestinationsConfig(connector.getOutboundDestinations());

		// after start
		connector.start();
		connector.setDestinationsConfig(connector.getOutboundDestinations());

		// after stop
		connector.stop();
		connector.setDestinationsConfig(connector.getOutboundDestinations());

		// after destroy
		connector.destroy();
		connector.setDestinationsConfig(connector.getOutboundDestinations());
	}

	@Test
	public void testAddDestinations() throws Exception {
		connector.afterPropertiesSet();
		// before start
		connector.addDestinations(createNewDestinations(".1", null));

		// after start
		connector.start();
		connector.addDestinations(createNewDestinations(".2", null));

		// after stop
		connector.stop();
		connector.addDestinations(createNewDestinations(".3", null));

		// after destroy
		connector.destroy();
		connector.addDestinations(createNewDestinations(".4", null));
	}

	@Test
	public void testRemoveDestinations() throws Exception {
		List<BridgedDestination> defaultDestinations = createNewDestinations(null, null);
		// before start
		connector.afterPropertiesSet();
		connector.removeDestinations(defaultDestinations);
	}

	@Test
	public void testRemoveDestinationsAfterStart() throws Exception {
		List<BridgedDestination> defaultDestinations = createNewDestinations(null, null);
		// after start
		connector.afterPropertiesSet();
		connector.start();
		connector.removeDestinations(defaultDestinations);
	}

	@Test
	public void testRemoveDestinationsAfterStop() throws Exception {
		List<BridgedDestination> defaultDestinations = createNewDestinations(null, null);
		// after stop
		connector.afterPropertiesSet();
		connector.start();
		connector.stop();
		connector.removeDestinations(defaultDestinations);
	}

	@Test
	public void testRemoveDestinationsAfterDestroy() throws Exception {
		List<BridgedDestination> defaultDestinations = createNewDestinations(null, null);
		// after destroy
		connector.afterPropertiesSet();
		connector.start();
		connector.destroy();
		connector.removeDestinations(defaultDestinations);
	}

}
