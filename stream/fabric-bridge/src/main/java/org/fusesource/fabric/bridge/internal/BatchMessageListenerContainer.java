/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge.internal;

import java.util.LinkedList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ClassUtils;

/**
 * @author Dhiraj Bokde
 *
 * @org.apache.xbean.XBean
 * 
 */
public class BatchMessageListenerContainer extends
		DefaultMessageListenerContainer {
	
	public static final long DEFAULT_BATCH_TIMEOUT = 1000L;

	public static final long DEFAULT_BATCH_SIZE = 100L;

	public static final String DEFAULT_THREAD_NAME_PREFIX =
		ClassUtils.getShortName(BatchMessageListenerContainer.class) + "-";

	private volatile Object batchMessageListener;

	private long batchSize = DEFAULT_BATCH_SIZE;
	
	private long batchTimeout = DEFAULT_BATCH_TIMEOUT;

	// Easiest way to get to the message consumer.
	private ThreadLocal<MessageConsumer> currentConsumer = new ThreadLocal<MessageConsumer>();
	
	public BatchMessageListenerContainer() {
		super();
		
		// set the default listener
		super.setMessageListener(new SessionAwareMessageListenerDelegate()); 
	}

	@Override
	protected boolean doReceiveAndExecute(Object invoker, Session session,
			MessageConsumer consumer, TransactionStatus status)
			throws JMSException {
		
		// save the message consumer in TLS
		currentConsumer.set(consumer);
		
		return super.doReceiveAndExecute(invoker, session, consumer, status);
	}
	
	@Override
	public void setMessageListener(Object messageListener) {
		throw new UnsupportedOperationException("Use the batchMessageListener property instead");
	}

	@Override
	protected void validateConfiguration() {
		super.validateConfiguration();
		if (batchMessageListener == null
				|| !(batchMessageListener instanceof SessionAwareBatchMessageListener)) {
			throw new IllegalArgumentException(
					"An object of type SessionAwareBatchMessageListener must be provided for the batchMessageListener property");
		}
		if (batchSize <= 0 || batchTimeout <= 0) {
			throw new IllegalArgumentException(
					"Properties batchSize and batchTimeout must have positive non-zero values");
		}
	}

	@Override
	protected TaskExecutor createDefaultTaskExecutor() {
		String beanName = getBeanName();
		String threadNamePrefix = (beanName != null ? beanName + "-" : DEFAULT_THREAD_NAME_PREFIX);
		return new SimpleAsyncTaskExecutor(threadNamePrefix);
	}

	public Object getBatchMessageListener() {
		return batchMessageListener;
	}

	public void setBatchMessageListener(Object batchMessageListener) {
		if (!(batchMessageListener instanceof SessionAwareBatchMessageListener)) {
			throw new IllegalArgumentException(
					"Message listener needs to be of type [" + SessionAwareBatchMessageListener.class.getName() + "]");
		}
		this.batchMessageListener = batchMessageListener;
	}

	public long getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(long batchSize) {
		this.batchSize = batchSize;
	}

	public long getBatchTimeout() {
		return batchTimeout;
	}

	public void setBatchTimeout(long batchTimeout) {
		this.batchTimeout = batchTimeout;
	}

	private final class SessionAwareMessageListenerDelegate implements
			SessionAwareMessageListener<Message> {
		@Override
		public void onMessage(final Message firstMessage, final Session session)
				throws JMSException {
			// pull messages in a batch from the TLS consumer and call batchMessageListener
			final long startTimeMillis = System.currentTimeMillis();
			
			final List<Message> messages = new LinkedList<Message>();
			messages.add(firstMessage);
	
			final MessageConsumer consumer = currentConsumer.get();
			currentConsumer.remove();
			
			int msgsReceived = 1;
			// use size and timeout together, the limit that's hit first, wins
			while (msgsReceived < batchSize
					&& (System.currentTimeMillis() - startTimeMillis) < batchTimeout) {
				final Message message = receiveMessage(consumer);
				if (message != null) {
					messages.add(message);
					msgsReceived++;
				}
			}
			
			// delegate to the batchMessageListener
			@SuppressWarnings("unchecked")
			SessionAwareBatchMessageListener<Message> lsnr = (SessionAwareBatchMessageListener<Message>)batchMessageListener;
			if (logger.isDebugEnabled()) {
				logger.debug("Received [" + msgsReceived + "] messages in a batch from consumer [" +
						consumer + "] of " + (session.getTransacted() ? "transactional " : "") + "session [" +
						session + "]");
			}
			lsnr.onMessages(messages, session);
		}
	}
	
}
