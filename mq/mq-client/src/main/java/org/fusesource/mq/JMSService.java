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

import javax.jms.*;

public interface JMSService {

    public ConnectionFactory getConnectionFactory();

    public Connection getDefaultConnection();

    public Session getDefaultSession();

    public MessageProducer createProducer(String destination) throws JMSException;

    public MessageConsumer createConsumer(String destination) throws JMSException;

    public TextMessage createTextMessage(String text) throws JMSException;

    public BytesMessage createBytesMessage(byte[] payload) throws JMSException;

    public void start() throws JMSException;

    public void stop();

}
