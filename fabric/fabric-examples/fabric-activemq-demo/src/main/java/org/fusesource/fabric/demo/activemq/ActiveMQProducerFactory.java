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

import org.apache.felix.scr.annotations.*;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.fusesource.mq.ActiveMQService;
import org.fusesource.mq.ProducerThread;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;
import java.util.Map;

@Component(name = "org.fusesource.fabric.example.mq.producer", description = "ActiveMQ Producer Factory", immediate = true, configurationFactory = true)
public class ActiveMQProducerFactory extends AbstractComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ActiveMQProducerFactory.class);
    ProducerThread producer;
    ActiveMQService producerService;
    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Activate
    void activate(ComponentContext context, Map<String, String> properties) throws Exception {
       updated(properties);
       activateComponent();
    }

    @Modified
    public void updated(Map<String, String> properties) throws Exception {
        try {
            String brokerUrl = (String) properties.get("brokerUrl");
            if (brokerUrl == null) {
                brokerUrl = "discover:(fabric:default)";
            }
            String password = (String)properties.get("password");
            if (password == null) {
                password = fabricService.get().getZookeeperPassword();
            }
            producerService = new ActiveMQService((String)properties.get("username"), password, brokerUrl);
            producerService.setMaxAttempts(10);
            producerService.start();
            String destination = (String) properties.get("destination");
            producer = new ProducerThread(producerService, destination);
            producer.setSleep(500);
            producer.start();
            LOG.info("Producer started");
        } catch (JMSException e) {
            throw new Exception("Cannot start producer", e);
        }
    }

    @Deactivate
    void deactivate() {
      destroy();
    }

    public void destroy() {
        if (producer != null) {
            producer.setRunning(false);
            producerService.stop();
        }
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(null);
    }
}
