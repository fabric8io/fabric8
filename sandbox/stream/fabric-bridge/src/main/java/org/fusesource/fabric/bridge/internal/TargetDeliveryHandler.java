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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.ProducerCallback;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * @author Dhiraj Bokde
 *
 */
public class TargetDeliveryHandler extends AbstractDeliveryHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(TargetDeliveryHandler.class);
	
	private final ConcurrentMap<String, FutureTask<Destination>> destinationMap = new ConcurrentHashMap<String, FutureTask<Destination>>();

	private DestinationResolver destinationResolver;

	@Override
	protected JmsTemplate createJmsTemplate() {
		JmsTemplate jmsTemplate = new JmsTemplate(getTargetConnectionFactory());
		jmsTemplate.setSessionAcknowledgeMode(getDispatchPolicy().getLocalAcknowledgeMode());
		jmsTemplate.setSessionTransacted(getDispatchPolicy().isLocalSessionTransacted());
		return jmsTemplate;
	}

	@Override
	protected MessageProducer createMessageProducer(Session localSession)
			throws JMSException {
		return localSession.createProducer(null);
	}

	@Override
	protected ProducerCallback<Message> createProducerCallback(
			List<Message> messages) {
		return new TargetProducerCallback(messages);
	}

	private Destination getTargetDestination(final Session lookupSession,
			final String destinationName, final boolean pubSubDomain) throws JMSException {
		Destination targetDestination;
		try {
			FutureTask<Destination> value = new FutureTask<Destination>(new Callable<Destination>() {
				@Override
				public Destination call() throws Exception {
					return destinationResolver.resolveDestinationName(lookupSession, destinationName, pubSubDomain);
				}
			});
			FutureTask<Destination> futureDestination = destinationMap.putIfAbsent(destinationName, value);
			if (futureDestination == null) {
				futureDestination = value;
				futureDestination.run();
			}
			targetDestination = futureDestination.get();
		} catch (InterruptedException e) {
			throw new JMSException("InterruptedException while getting target destination: " + e.getMessage());
		} catch (ExecutionException e) {
			throw new JMSException("ExecutionException while getting target destination: " + e.getMessage());
		}
		return targetDestination;
	}

	public final void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	public class TargetProducerCallback implements ProducerCallback<Message> {
	
		private final List<Message> messages;
	
		public TargetProducerCallback(List<Message> messages) {
			this.messages = messages;
		}
	
		@Override
		public Message doInJms(Session sendSession, MessageProducer messageProducer)
				throws JMSException {
			Message currentMessage = null;
			Map<String, Object> properties = new HashMap<String, Object>();
			try {
				for (final Message message : messages) {
					currentMessage = message;
	
					// resolve destination using policy
					// TODO we probably need to get other properties??
					final Destination targetDestination = getTargetDestination(
							sendSession, currentMessage.getStringProperty(getDestinationNameHeader()), currentMessage.getBooleanProperty(getDestinationTypeHeader()));
					
					if (isReuseMessage()) {
						// make message properties writable by clearing and resetting them
						getMessageProperties(currentMessage, properties);
						currentMessage.clearProperties();
						setMessageProperties(currentMessage, properties);
						properties.clear();
					} else {
						currentMessage = copyMessage(currentMessage, sendSession);
					}
	
					// convert message before send
					if (getDispatchPolicy().getMessageConverter() != null) {
						currentMessage = getDispatchPolicy().getMessageConverter().convert(currentMessage);
					}
	
					// TODO set delivery mode, priority and time to live??
					messageProducer.send(targetDestination, currentMessage);
	
				}
				
				// commit sendSession if necessary
				if (!isReuseSession()) {
					JmsUtils.commitIfNecessary(sendSession);
				}
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("Sent " + messages.size() + " messages in session " + sendSession);
				}
	
				return null;
				
			} catch (JMSException exception) {
				LOG.error ("Error sending message " + currentMessage + ": " + exception.getMessage(), exception);
				throw exception;
	
			}
		}
	
	}

}
