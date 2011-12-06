/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.demo.activemq;

import org.fusesource.fabric.activemq.JMSService;
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
