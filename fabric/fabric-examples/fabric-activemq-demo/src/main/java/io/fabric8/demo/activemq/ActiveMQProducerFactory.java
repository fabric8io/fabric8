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
import org.fusesource.mq.ProducerThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;

import java.util.Map;

@Component(name = "io.fabric8.example.mq.producer", label = "ActiveMQ Producer Factory", configurationFactory = true, immediate = true, metatype = true)
public class ActiveMQProducerFactory extends AbstractComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ActiveMQProducerFactory.class);
    ProducerThread producer;
    ActiveMQService producerService;
    @Reference(referenceInterface = ActiveMQConnectionFactory.class)
    private ActiveMQConnectionFactory connectionFactory;

    @Activate
    void activate(Map<String, ?> properties) throws Exception {
        updateInternal(properties);
        activateComponent();
    }

    @Modified
    public void modified(Map<String, ?> configuration) throws Exception {
        deactivateInternal();
        updateInternal(configuration);
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        deactivateInternal();
    }

    protected void deactivateInternal() {
        if (producer != null) {
            producer.setRunning(false);
            producerService.stop();
        }
    }

    private void updateInternal(Map<String, ?> configuration) throws Exception {
        try {
            producerService = new ActiveMQService(connectionFactory);
            producerService.setMaxAttempts(10);
            producerService.start();
            String destination = (String) configuration.get("destination");
            producer = new ProducerThread(producerService, destination);
            producer.setSleep(500);
            producer.start();
            LOG.info("Producer started");
        } catch (JMSException e) {
            throw new Exception("Cannot start producer", e);
        }
    }
}
