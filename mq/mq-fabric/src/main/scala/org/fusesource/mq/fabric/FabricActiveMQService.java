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
package org.fusesource.mq.fabric;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQDestination;

import javax.jms.*;

public class FabricActiveMQService implements JMSService {

    ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("discovery:(fabric:default)");
    Connection defaultConnection;
    Session defaultSession;
    boolean transacted = false;
    int ackMode = Session.AUTO_ACKNOWLEDGE;

    boolean started = false;  //TODO use atomic boolean


    public FabricActiveMQService() {
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
        if (!started) {
            defaultConnection = connectionFactory.createConnection();
            defaultConnection.start();
            defaultSession = defaultConnection.createSession(transacted, ackMode);
        }
        started = true;
    }

    public void stop() throws JMSException {
        if (started) {
            if (defaultConnection != null) {
                defaultConnection.stop();
            }
        }
        started = false;
    }
}
