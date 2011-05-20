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

import org.fusesource.fabric.apollo.amqp.api.*;
import org.fusesource.hawtbuf.Buffer;

import static org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory.createAmqpLong;
import static org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory.createAmqpSymbol;

/**
 *
 */
public class Receive extends Client implements MessageListener {

    int received = 0;

    public static void main(String ... args) {
        new Receive(args).go();
    }

    public Receive(String ... args) {
        super(args);
    }

    @Override
    public void printHelp() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onConnect(final Connection connection) {
        final Session session = connection.createSession();
        final Receiver receiver = session.createReceiver();
        receiver.setOnDetach(new Runnable() {
            public void run() {
                session.end(new Runnable() {
                    public void run() {
                        connection.close();
                    }
                });
            }
        });

        receiver.getSourceOptionsMap().put(createAmqpSymbol("batch-size"), createAmqpLong(batch_size));

        receiver.setAddress(address);
        receiver.setListener(this);
        receiver.attach(new Runnable() {
            public void run() {
                println("Attached receiver...");
                receiver.addLinkCredit(batch_size);
            }
        });
    }

    @Override
    public boolean full() {
        return false;
    }

    @Override
    public boolean offer(Receiver receiver, Message message) {
        received++;
        Buffer msg = (Buffer)message.getBodyPart(0);
        println("Received message #%s : %s", received, msg.ascii());
        if (!message.getSettled()) {
            receiver.settle(message, Outcome.ACCEPTED);
        }
        if (received >= count) {
            receiver.detach();
        }
        Long credit = receiver.getAvailableLinkCredit();
        if (credit != null && credit < 1) {
            receiver.addLinkCredit(batch_size - credit);
        }
        return true;
    }

    @Override
    public void refiller(Runnable refiller) {

    }

    @Override
    public long needLinkCredit(long available) {
        return 0;
    }
}
