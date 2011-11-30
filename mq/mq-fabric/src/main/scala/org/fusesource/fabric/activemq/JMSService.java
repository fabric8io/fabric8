/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.activemq;

import javax.jms.*;

public interface JMSService {

    public ConnectionFactory getConnectionFactory();

    public Connection getDefaultConnection();

    public Session getDefaultSession();

    public MessageProducer createProducer(String destination) throws JMSException;

    public MessageConsumer createConsumer(String destination) throws JMSException;

    public TextMessage createTextMessage(String text) throws JMSException;

    public void start() throws JMSException;

    public void stop() throws JMSException;

}
