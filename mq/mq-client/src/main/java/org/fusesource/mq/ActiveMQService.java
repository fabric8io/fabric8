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
package org.fusesource.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQTopic;

import javax.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveMQService implements JMSService {

    private static final Logger LOG = LoggerFactory.getLogger(ActiveMQService.class);

    Connection defaultConnection;
    Session defaultSession;
    boolean transacted = false;
    int maxAttempts = 1;

    boolean started = false;  //TODO use atomic boolean

    private ActiveMQConnectionFactory connectionFactory;
    private String clientId;


    public ActiveMQService(String user, String password, String brokerUrl) {
        this(new ActiveMQConnectionFactory(user, password, brokerUrl));
    }

    public ActiveMQService(String brokerUrl) {
        this(null, null, brokerUrl);
    }

    public ActiveMQService(ActiveMQConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public Connection getDefaultConnection() {
        return defaultConnection;
    }

    public Session getDefaultSession() {
        return defaultSession;
    }

    public MessageProducer createProducer(String destination) throws JMSException {
        return defaultSession.createProducer(ActiveMQDestination.createDestination(destination, ActiveMQDestination.QUEUE_TYPE));
    }

    public MessageConsumer createConsumer(String destination) throws JMSException {
        if (clientId != null) {
            return defaultSession.createDurableSubscriber((ActiveMQTopic)ActiveMQDestination.createDestination(destination, ActiveMQDestination.TOPIC_TYPE), "fuseSub");
        } else {
            return defaultSession.createConsumer(ActiveMQDestination.createDestination(destination, ActiveMQDestination.QUEUE_TYPE));
        }
    }

    public TextMessage createTextMessage(String text) throws JMSException {
        return defaultSession.createTextMessage(text);
    }

    public BytesMessage createBytesMessage(byte[] payload) throws JMSException {
        BytesMessage message = defaultSession.createBytesMessage();
        message.writeBytes(payload);
        return message;
    }

    public void start() throws JMSException {
        int attempts = 0;
        JMSException lastException = null;
        while (!started && attempts++ < maxAttempts) {
            try {
                defaultConnection = connectionFactory.createConnection();
                if (clientId != null) {
                    defaultConnection.setClientID(clientId);
                }
                defaultConnection.start();
                defaultSession = defaultConnection.createSession(transacted,
                        transacted ? Session.SESSION_TRANSACTED : Session.AUTO_ACKNOWLEDGE);
                started = true;
            } catch (JMSException e) {
                lastException = e;
                LOG.warn("Could not start a connection", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {}
            }
        }
        
        if (!started) {
            throw lastException;
        }
    }

    public void stop() {
        if (started) {
            if (defaultConnection != null) {
                try {
                    LOG.info("Closed JMS connection");
                    defaultConnection.close();
                } catch (JMSException ignored) {
                    LOG.info("Exception closing JMS exception", ignored);
                }
            }
        }
        started = false;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }
}
