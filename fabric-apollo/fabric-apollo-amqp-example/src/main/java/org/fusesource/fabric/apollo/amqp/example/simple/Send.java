/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.example.simple;

import org.fusesource.fabric.apollo.amqp.api.AmqpConnectionFactory;
import org.fusesource.fabric.apollo.amqp.api.Connection;
import org.fusesource.fabric.apollo.amqp.api.Sender;
import org.fusesource.fabric.apollo.amqp.api.Session;

/**
 *
 */
public class Send extends Client {

    public static void main(String ... args) {
        new Send(args).go();
    }

    public Send(String ... args) {
        super(args);
    }

    @Override
    public void go() {
        final Connection connection = AmqpConnectionFactory.create();
        connection.setOnClose(new Runnable() {
            public void run() {
                exit_latch.countDown();
            }
        });
        connection.connect(getConnectionURI(), new Runnable() {
            public void run() {
                final Session session = connection.createSession();
                final Sender sender = session.createSender();
                sender.setAddress(address);

                sender.attach(new Runnable() {
                    public void run() {


                    }
                });
            }
        });

    }

    @Override
    public void printHelp() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
