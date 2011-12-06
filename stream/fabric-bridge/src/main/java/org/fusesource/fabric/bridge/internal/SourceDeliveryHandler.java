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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

/**
 * @author Dhiraj Bokde
 *
 */
public class SourceDeliveryHandler extends AbstractDeliveryHandler {
	
	static final Logger LOG = LoggerFactory.getLogger(SourceDeliveryHandler.class);
	
	private String destinationName;
	
	private boolean pubSubDomain;
	
	private Destination stagingDestination;

	protected ProducerCallback<Message> createProducerCallback(List<Message> messages) {
		return new SourceProducerCallback(messages);
	}

	protected MessageProducer createMessageProducer(Session localSession)
			throws JMSException {
		return localSession.createProducer(stagingDestination);
	}

	protected JmsTemplate createJmsTemplate() {
		JmsTemplate jmsTemplate = new JmsTemplate(getTargetConnectionFactory());
		jmsTemplate.setSessionAcknowledgeMode(getDispatchPolicy().getRemoteAcknowledgeMode());
		jmsTemplate.setSessionTransacted(getDispatchPolicy().isRemoteSessionTransacted());
		jmsTemplate.setDefaultDestination(stagingDestination);
		return jmsTemplate;
	}

	public String getDestinationName() {
		return destinationName;
	}

	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	public void setStagingDestination(Destination stagingDestination) {
		this.stagingDestination = stagingDestination;
	}

	private class SourceProducerCallback implements ProducerCallback<Message> {
	
		private final List<Message> messages;
	
		public SourceProducerCallback(List<Message> messages) {
			this.messages = messages;
		}
	
		@Override
		public Message doInJms(Session sendSession, MessageProducer messageProducer)
				throws JMSException {
			
			Message currentMessage = null;
			try {
				Map<String, Object> properties = new HashMap<String, Object>();
				for (final Message message : messages) {
					
					currentMessage = message;
					if (isReuseMessage()) {
	
						// make message properties writable by clearing and resetting them
						getMessageProperties(currentMessage, properties);
						currentMessage.clearProperties();
						setMessageProperties(currentMessage, properties);
						properties.clear();
						
					} else {
	
						currentMessage = copyMessage(currentMessage, sendSession);
					
					}
	
					// allow tunneling messages through bridge
					if (currentMessage.getStringProperty(getDestinationNameHeader()) == null) {
						currentMessage.setStringProperty(getDestinationNameHeader(), destinationName);
						currentMessage.setBooleanProperty(getDestinationTypeHeader(), pubSubDomain);
						// TODO are there other properties to set??
					}
					
					// convert message before send
					if (getDispatchPolicy().getMessageConverter() != null) {
						currentMessage = getDispatchPolicy().getMessageConverter().convert(currentMessage);
					}
	
					// TODO set delivery mode, priority and time to live??
					messageProducer.send(currentMessage);
				}
				
				// commit sendSession if necessary
				if (!isReuseSession()) {
					JmsUtils.commitIfNecessary(sendSession);
				}
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("Sent " + messages.size() + " messages to " + stagingDestination + " in session " + sendSession);
				}
				
				return null;
				
			} catch (JMSException exception) {
				LOG.error ("Error sending message " + currentMessage + ": " + exception.getMessage(), exception);
				throw exception;
			}
		}
	
	}

}
