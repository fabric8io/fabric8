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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.fusesource.fabric.activemq.JMSService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import javax.jms.Connection;
import javax.jms.Queue;
import javax.jms.Session;

public class Demo implements BundleActivator {

    JMSService service;

    public void start(BundleContext bundleContext) throws Exception {
        System.out.println("Starting ActiveMQ Demo");

        ServiceTracker tracker = new ServiceTracker(bundleContext, JMSService.class.getName(), null);
        tracker.open();

        service = (JMSService)tracker.getService();
        service.start();


        ProducerThread producer = new ProducerThread(service, "queue://TEST");
        producer.setSleep(500);
        producer.start();

        System.out.println("Producer Started");

        ConsumerThread consumer = new ConsumerThread(service, "queue://TEST");
        consumer.start();

        System.out.println("Consumer Started");
    }

    public void stop(BundleContext bundleContext) throws Exception {
       System.out.println("Stopping ActiveMQ Demo");
    }
}
