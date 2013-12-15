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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageEOFException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.BlobMessage;
import org.apache.activemq.blob.BlobDownloader;
import org.apache.activemq.command.ActiveMQBlobMessage;
import org.apache.activemq.command.ActiveMQMessage;
import io.fabric8.bridge.model.DispatchPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.ProducerCallback;
import org.springframework.jms.listener.SessionAwareMessageListener;

/**
 * @author Dhiraj Bokde
 *
 */
public abstract class AbstractDeliveryHandler implements
		SessionAwareBatchMessageListener<Message>,
		SessionAwareMessageListener<Message> {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractDeliveryHandler.class);
	private String destinationNameHeader;
	private String destinationTypeHeader;
	private ConnectionFactory targetConnectionFactory;
	private boolean reuseSession;
	private boolean reuseMessage;
	private DispatchPolicy dispatchPolicy;

	private JmsTemplate jmsTemplate;

	public final void onMessages(List<Message> messages, Session sourceSession)
			throws JMSException {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Sending batch containing " + messages.size()
					+ " messages");
		}
		doSend(messages, sourceSession);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Send complete for batch containing " + messages.size()
					+ " messages");
		}
	}

	@Override
	public final void onMessage(final Message message,
			final Session sourceSession) throws JMSException {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Sending message " + message);
		}

		List<Message> messages = new LinkedList<Message>();
		messages.add(message);
		this.doSend(messages, sourceSession);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Send complete for message " + message);
		}

	}

	protected void doSend(List<Message> messages, Session localSession) throws JMSException {
		MessageProducer messageProducer = null;
	
		try {
			
			ProducerCallback<Message> producerCallback = createProducerCallback(messages);

			if (isReuseSession()) {
				messageProducer = createMessageProducer(localSession);
				producerCallback.doInJms(localSession, messageProducer);
			} else {
				if (jmsTemplate == null) {
					jmsTemplate = createJmsTemplate();
				}
				jmsTemplate.execute(producerCallback);
			}
	
		} finally {
			if (messageProducer != null) {
				try {
					messageProducer.close();
				} catch (JMSException e) {}
			}
		}
	}

	protected abstract JmsTemplate createJmsTemplate();
	
	protected abstract MessageProducer createMessageProducer(Session localSession) throws JMSException;
	
	protected abstract ProducerCallback<Message> createProducerCallback(List<Message> messages);

	protected void setMessageProperties(Message message,
			Map<String, Object> properties) throws JMSException {
		for (Entry<String, Object> entry : properties.entrySet()) {
			message.setObjectProperty(entry.getKey(), entry.getValue());
		}
	}

	protected Message copyMessage(Message message, Session session)
			throws JMSException {

		// create the right message type using the session
		Message copyMessage = null;
		if (message instanceof BytesMessage) {
			BytesMessage bytesMsg = (BytesMessage) message;
			bytesMsg.reset();
			BytesMessage msg = session.createBytesMessage();
			try {
				for (;;) {
					// Reads a byte from the message stream until the stream
					// is empty
					msg.writeByte(bytesMsg.readByte());
				}
			} catch (MessageEOFException e) {
				// if an end of message stream as expected
			} catch (JMSException e) {
			}

			copyMessage = msg;
		} else if (message instanceof MapMessage) {
			MapMessage mapMsg = (MapMessage) message;
			MapMessage msg = session.createMapMessage();
			@SuppressWarnings("rawtypes")
			Enumeration iter = mapMsg.getMapNames();

			while (iter.hasMoreElements()) {
				String name = iter.nextElement().toString();
				msg.setObject(name, mapMsg.getObject(name));
			}

			copyMessage = msg;
		} else if (message instanceof ObjectMessage) {
			ObjectMessage objMsg = (ObjectMessage) message;
			ObjectMessage msg = session.createObjectMessage();
			msg.setObject(objMsg.getObject());
			copyMessage = msg;
		} else if (message instanceof StreamMessage) {
			StreamMessage streamMessage = (StreamMessage) message;
			streamMessage.reset();
			StreamMessage msg = session.createStreamMessage();
			Object obj = null;

			try {
				while ((obj = streamMessage.readObject()) != null) {
					msg.writeObject(obj);
				}
			} catch (MessageEOFException e) {
				// if an end of message stream as expected
			} catch (JMSException e) {
			}

			copyMessage = msg;
		} else if (message instanceof TextMessage) {
			TextMessage textMsg = (TextMessage) message;
			TextMessage msg = session.createTextMessage();
			msg.setText(textMsg.getText());
			copyMessage = msg;
		} else if (message instanceof BlobMessage) {
			BlobMessage blobMessage = (BlobMessage) message;
			ActiveMQBlobMessage msg = new ActiveMQBlobMessage();
			ActiveMQConnection connection = ((ActiveMQSession) session)
					.getConnection();
			msg.setConnection(connection);
			msg.setBlobDownloader(new BlobDownloader(connection
					.getBlobTransferPolicy()));
			try {
				msg.setURL(blobMessage.getURL());
			} catch (MalformedURLException e) {

			}
			copyMessage = msg;
		} else {
			copyMessage = session.createMessage();
		}

		// lets populate the standard JMS message headers
		copyMessage.setJMSCorrelationID(message.getJMSCorrelationID());
		copyMessage.setJMSDeliveryMode(message.getJMSDeliveryMode());
		copyMessage.setJMSDestination(message.getJMSDestination());
		copyMessage.setJMSExpiration(message.getJMSExpiration());
		copyMessage.setJMSMessageID(message.getJMSMessageID());
		copyMessage.setJMSPriority(message.getJMSPriority());
		copyMessage.setJMSRedelivered(message.getJMSRedelivered());
		copyMessage.setJMSTimestamp(message.getJMSTimestamp());

		copyMessage.setJMSReplyTo(message.getJMSReplyTo());
		copyMessage.setJMSType(message.getJMSType());

		// this works around a bug in the ActiveMQ property handling
		if (copyMessage instanceof ActiveMQMessage) {
			try {
				((ActiveMQMessage) copyMessage).setProperty("JMSXGroupID",
						message.getStringProperty("JMSXGroupID"));
			} catch (IOException e) {
				throw new JMSException(e.getMessage());
			}
		}

		// copy properties
		@SuppressWarnings("rawtypes")
		Enumeration names;
		names = message.getPropertyNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement().toString();
			Object value = message.getObjectProperty(name);
			copyMessage.setObjectProperty(name, value);
		}

		return copyMessage;
	}

	protected void getMessageProperties(Message message, Map<String, Object> map)
			throws JMSException {
		@SuppressWarnings("rawtypes")
		Enumeration names;
		names = message.getPropertyNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement().toString();
			Object value = message.getObjectProperty(name);
			map.put(name, value);
		}
	}

	public final void setDestinationNameHeader(String destinationNameHeader) {
		this.destinationNameHeader = destinationNameHeader;
	}

	public final String getDestinationNameHeader() {
		return destinationNameHeader;
	}

	public final void setDestinationTypeHeader(String destinationTypeHeader) {
		this.destinationTypeHeader = destinationTypeHeader;
	}

	public final String getDestinationTypeHeader() {
		return destinationTypeHeader;
	}
	
	public final void setTargetConnectionFactory(
			ConnectionFactory targetConnectionFactory) {
		this.targetConnectionFactory = targetConnectionFactory;
	}

	public final ConnectionFactory getTargetConnectionFactory() {
		return targetConnectionFactory;
	}

	public final void setReuseSession(boolean reuseSession) {
		this.reuseSession = reuseSession;
	}

	public final boolean isReuseSession() {
		return reuseSession;
	}

	public final void setReuseMessage(boolean reuseMessage) {
		this.reuseMessage = reuseMessage;
	}

	public final boolean isReuseMessage() {
		return reuseMessage;
	}

	public final void setDispatchPolicy(DispatchPolicy dispatchPolicy) {
		this.dispatchPolicy = dispatchPolicy;
	}

	public final DispatchPolicy getDispatchPolicy() {
		return dispatchPolicy;
	}

}