/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import javax.jms.Connection;
import javax.jms.Queue;
import javax.jms.Session;

public class Demo implements BundleActivator {

    public void start(BundleContext bundleContext) throws Exception {
        System.out.println("Starting ActiveMQ Demo");

        System.out.println("Starting url " + "'discovery:(fabric:default)'");
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("discovery:(fabric:default)");
        Connection conn = factory.createConnection();
        conn.start();
        System.out.println("Connection started");
        Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = sess.createQueue("TEST");

        ProducerThread producer = new ProducerThread(sess, queue);
        producer.setSleep(500);
        producer.start();

        System.out.println("Producer Started");

        ConsumerThread consumer = new ConsumerThread(sess, queue);
        consumer.start();

        System.out.println("Consumer Started");
    }

    public void stop(BundleContext bundleContext) throws Exception {
       System.out.println("Stopping ActiveMQ Demo");
    }
}
