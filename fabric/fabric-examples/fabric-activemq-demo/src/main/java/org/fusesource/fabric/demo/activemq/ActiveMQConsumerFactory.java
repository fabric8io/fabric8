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
package org.fusesource.fabric.demo.activemq;

import org.fusesource.mq.ConsumerThread;
import org.fusesource.mq.ActiveMQService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import java.util.Dictionary;

public class ActiveMQConsumerFactory implements ManagedServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ActiveMQProducerFactory.class);
    ConsumerThread consumer;

    @Override
    public String getName() {
        return "ActiveMQ Consumer Factory";
    }

    @Override
    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        try {
            String brokerUrl = (String) properties.get("brokerUrl");
            if (brokerUrl == null) {
                brokerUrl = "discover:(fabric:default)";
            }
            ActiveMQService consumerService = new ActiveMQService(brokerUrl);
            consumerService.setMaxAttempts(10);
            consumerService.start();
            String destination = (String) properties.get("destination");
            consumer = new ConsumerThread(consumerService, destination);
            consumer.start();
            LOG.info("Consumer " + pid + " started");
        } catch (JMSException e) {
            throw new ConfigurationException(null, "Cannot start consumer", e);
        }
    }

    @Override
    public void deleted(String pid) {
       destroy();
    }

    public void destroy() {
        if (consumer != null) {
            consumer.setRunning(false);
        }
    }
}
