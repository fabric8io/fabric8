/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.io.fabric8.quickstarts.activemq.springboot;

import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

/**
 */
public class ActiveMQConfiguration implements InitializingBean {
    @Autowired
    ConnectionFactory connectionFactory;

    @Autowired
    JmsTemplate jmsTemplate;

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("" + this + " is starting up!");

        Destination queue = new ActiveMQQueue("QuickStarts.ActiveMQ.Spring.Boot");
        String message = "message";

        jmsTemplate.convertAndSend(queue, message);
        System.out.println("Sent message to queue: " + queue);

    }
}

