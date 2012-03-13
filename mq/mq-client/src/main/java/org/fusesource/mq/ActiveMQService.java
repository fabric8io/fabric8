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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;

public class ActiveMQService implements JMSService {

    private static final Log LOG = LogFactory.getLog(ActiveMQService.class);

    Connection defaultConnection;
    Session defaultSession;
    boolean transacted = false;
    int ackMode = Session.AUTO_ACKNOWLEDGE;
    int maxAttempts = 1;

    boolean started = false;  //TODO use atomic boolean

    private ActiveMQConnectionFactory connectionFactory;


    public ActiveMQService(String brokerUrl) {
        connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
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
        return defaultSession.createConsumer(ActiveMQDestination.createDestination(destination, ActiveMQDestination.QUEUE_TYPE));
    }

    public TextMessage createTextMessage(String text) throws JMSException {
        return defaultSession.createTextMessage(text);
    }

    public void start() throws JMSException {
        int attempts = 0;
        JMSException lastException = null;
        while (!started && attempts++ < maxAttempts) {
            try {
                defaultConnection = connectionFactory.createConnection();
                defaultConnection.start();
                defaultSession = defaultConnection.createSession(transacted, ackMode);
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
                    defaultConnection.close();
                } catch (JMSException ignored) {
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
}
