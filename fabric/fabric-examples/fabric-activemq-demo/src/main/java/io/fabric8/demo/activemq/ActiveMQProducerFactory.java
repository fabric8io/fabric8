/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.demo.activemq;

import java.util.Map;
import javax.jms.JMSException;

import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.mq.ActiveMQService;
import io.fabric8.mq.ProducerThread;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "io.fabric8.example.mq.producer", label = "ActiveMQ Producer Factory", configurationFactory = true, immediate = true, metatype = true)
public class ActiveMQProducerFactory extends AbstractComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ActiveMQProducerFactory.class);

    public static final String DEFAULT_DESTINATION = "example";

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
        LOG.info("Stopping producer");
        if (producer != null) {
            producer.setRunning(false);
            producerService.stop();
        }
        LOG.info("Producer stopped");
    }

    private void updateInternal(Map<String, ?> configuration) throws Exception {
        try {
            LOG.info("Starting producer");
            producerService = new ActiveMQService(connectionFactory);
            producerService.setMaxAttempts(10);
            producerService.start();
            String destination = (String) configuration.get("destination");
            if (destination == null) {
                destination = DEFAULT_DESTINATION;
            }
            producer = new ProducerThread(producerService, destination);
            producer.setSleep(500);
            producer.start();
            LOG.info("Producer started");
        } catch (JMSException e) {
            throw new Exception("Cannot start producer", e);
        }
    }
}
