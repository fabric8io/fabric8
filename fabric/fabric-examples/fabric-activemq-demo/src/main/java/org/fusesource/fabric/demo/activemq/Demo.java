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
import org.fusesource.mq.JMSService;
import org.fusesource.mq.ProducerThread;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Demo {

    private JMSService service;
    private BundleContext bundleContext;

    public void registerJmsService(ServiceReference reference) throws Exception {
        System.out.println("Starting ActiveMQ Demo");
        service = (JMSService)bundleContext.getService(reference);
        service.start();

        ProducerThread producer = new ProducerThread(service, "queue://TEST");
        producer.setSleep(500);
        producer.start();

        System.out.println("Producer Started");

        ConsumerThread consumer = new ConsumerThread(service, "queue://TEST");
        consumer.start();

        System.out.println("Consumer Started");

    }


    public void unregisterJmsService(ServiceReference reference) throws Exception {
        System.out.println("Stopping ActiveMQ Demo");
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
