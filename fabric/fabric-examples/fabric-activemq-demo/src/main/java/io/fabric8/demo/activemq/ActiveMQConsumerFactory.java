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
package io.fabric8.demo.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.felix.scr.annotations.*;
import io.fabric8.api.scr.AbstractComponent;
import org.fusesource.mq.ActiveMQService;
import org.fusesource.mq.ConsumerThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;

import java.util.Map;

@Component(name = "io.fabric8.example.mq.consumer", label = "ActiveMQ Consumer Factory", configurationFactory = true, immediate = true, metatype = true)
public class ActiveMQConsumerFactory extends AbstractComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ActiveMQProducerFactory.class);
    ConsumerThread consumer;
    ActiveMQService consumerService;
    @Reference(referenceInterface = ActiveMQConnectionFactory.class)
    private ActiveMQConnectionFactory connectionFactory;

    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        updateInternal(configuration);
        activateComponent();
    }

    @Modified
    void modified(Map<String, ?> configuration) throws Exception {
        deactivateInternal();
        updateInternal(configuration);
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        deactivateInternal();
    }

    protected void deactivateInternal() {
        if (consumer != null) {
            consumer.setRunning(false);
            consumerService.stop();
        }
    }

    private void updateInternal(Map<String, ?> configuration) throws Exception {
        try {
            consumerService = new ActiveMQService(connectionFactory);
            consumerService.setMaxAttempts(10);
            consumerService.start();
            String destination = (String) configuration.get("destination");
            consumer = new ConsumerThread(consumerService, destination);
            consumer.start();
            LOG.info("Consumer started");
        } catch (JMSException e) {
            throw new Exception("Cannot start consumer", e);
        }
    }
}
