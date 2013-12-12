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

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dhiraj Bokde
 *
 */
public class BatchMessageListenerContainerTest extends AbstractConnectorTestSupport {

	static final Logger LOG = LoggerFactory.getLogger(BatchMessageListenerContainerTest.class);
	
	private BatchMessageListenerContainer listenerContainer;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		LOG.debug("Creating listener container");
		listenerContainer = new BatchMessageListenerContainer();
		listenerContainer.setAutoStartup(false);
	
		listenerContainer.setConnectionFactory(new ActiveMQConnectionFactory(TEST_LOCAL_BROKER_URL));
	
		listenerContainer.setConcurrentConsumers(TEST_NUM_BATCHES);
		listenerContainer.setDestinationName(TEST_QUEUE);
		listenerContainer.setReceiveTimeout(TEST_RECEIVE_TIMEOUT);
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		LOG.debug("Destroying listener container");
		listenerContainer.stop();
		listenerContainer.destroy();
		listenerContainer = null;
	}

	/**
	 * Test method for {@link io.fabric8.bridge.internal.BatchMessageListenerContainer#setBatchMessageListener(SessionAwareBatchMessageListener)}.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testSetBatchMessageListener() {
		listenerContainer.setBatchMessageListener(new Object());
	}
	
	/**
	 * Test method for {@link io.fabric8.bridge.internal.BatchMessageListenerContainer#setBatchSize(long)}.
	 */
	@Test
	public void testSetBatchSize() {
		// collect results
		final CountDownLatch countDownLatch = new CountDownLatch(TEST_NUM_BATCHES);
		final ConcurrentLinkedQueue<Integer> blockingQueue = new ConcurrentLinkedQueue<Integer>();
		
		// configure bmlc
		listenerContainer.setBatchSize(TEST_BATCH_SIZE);
		// set batch time out to a large value
		listenerContainer.setBatchTimeout(TEST_PSEUDO_DISABLE);
		listenerContainer.setBatchMessageListener(new SessionAwareBatchMessageListener<Message>() {
		
			public void onMessages(List<Message> messages, Session session)
					throws JMSException {
				countDownLatch.countDown();
				final int nMsgs = messages.size();
				blockingQueue.add(nMsgs);
				logBatch("Batch size [", nMsgs, session);
			}
		});
		listenerContainer.afterPropertiesSet();
		
		// send messages
		sendMessages(TEST_LOCAL_BROKER_URL, TEST_QUEUE, TEST_NUM_MESSAGES, null);
		
		// start bmlc
		listenerContainer.start();
		
		// wait for batches to finish
		try {
			assertTrue("Test timed out", countDownLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS));
		} catch (InterruptedException e) {
			fail("Batch test interrupted");
		}
		
		int nResults = blockingQueue.size();
		for (int i = 0; i < nResults; i++) {
			assertTrue("Batch size exceeded", blockingQueue.poll() <= TEST_BATCH_SIZE);
		}
	}

	/**
	 * Test method for {@link io.fabric8.bridge.internal.BatchMessageListenerContainer#setBatchTimeout(long)}.
	 */
	@Test
	public void testSetBatchTimeout() {
		// collect results
		final CountDownLatch countDownLatch = new CountDownLatch(TEST_NUM_BATCHES);
		final ConcurrentLinkedQueue<Integer> blockingQueue = new ConcurrentLinkedQueue<Integer>();
		
		// configure bmlc
		listenerContainer.setBatchTimeout(TEST_BATCH_TIMEOUT);
		// set batch size to a large value
		listenerContainer.setBatchSize(TEST_PSEUDO_DISABLE);
		listenerContainer.setBatchMessageListener(new SessionAwareBatchMessageListener<Message>() {

			public void onMessages(List<Message> messages, Session session)
					throws JMSException {
				countDownLatch.countDown();
				final int nMsgs = messages.size();
				int batchDuration = (int) (messages.get(nMsgs - 1).getJMSTimestamp() - 
						messages.get(0).getJMSTimestamp());
				blockingQueue.add(batchDuration);
				logBatch("Batch duration [", batchDuration, session);
			}
		});
		listenerContainer.afterPropertiesSet();
		
		// start bmlc before sending messages
		listenerContainer.start();
		
		sendMessages(TEST_LOCAL_BROKER_URL, TEST_QUEUE, TEST_NUM_MESSAGES, null);
		
		// wait for batches to finish
		try {
			assertTrue("Test timed out", countDownLatch.await(TEST_TIMEOUT, TimeUnit.SECONDS));
		} catch (InterruptedException e) {
			fail("Batch test interrupted");
		}
		
		int nResults = blockingQueue.size();
		for (int i = 0; i < nResults; i++) {
			assertTrue("Batch exceeded timeout", blockingQueue.poll() <= TEST_BATCH_TIMEOUT);
		}
	}

	private void logBatch(final String resultType, final int value, Session session) {
		StringBuffer infoMsg = new StringBuffer(resultType);
		infoMsg.append(value);
		infoMsg.append("] in Session[");
		infoMsg.append(session.toString());
		infoMsg.append("]");
		LOG.info(infoMsg.toString());
	}

}
