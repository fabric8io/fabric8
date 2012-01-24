/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
