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

import java.util.concurrent.TimeUnit;
import org.fusesource.fabric.apollo.amqp.api.*;
import org.fusesource.hawtbuf.Buffer;

/**
 * Simple AMQP sender that submits a batch of messages and waits
 * for the last message in the batch to be acknowledged before
 * sending more messages.
 */
public class BatchSend extends Send {

    protected int batch_size;

    private int current_batch;

    //private long enqueue_delay = 0;

    public static void main(String ... args) {
        new BatchSend(args).go();
    }

    public BatchSend(String ... args) {
        super(args);
        current_batch = batch_size;
        println("Using batch_size : %s and count : %s", batch_size, count);
    }

    @Override
    public void parseArgs(String ... args) {
        super.parseArgs(args);
        batch_size = 10;
        for(String arg : args) {
            if (arg.startsWith("--batch_size")) {
                batch_size = Integer.parseInt(arg.split("=")[1]);
            }
        }
    }

    @Override
    public void configureMessageTasks(final Session session, final Sender sender, final int current_count, final Message message) {

        current_batch--;

        if (current_batch > 0 && !(current_count >= count)) {
            message.onPut(new Runnable() {
                public void run() {
                    /*
                     * TODO - Decide if this delay is really wanted...
                    session.getConnection().getDispatchQueue().executeAfter(enqueue_delay, TimeUnit.MILLISECONDS, new Runnable() {
                        public void run() {
                            sendMessage(session, sender, current_count + 1);
                        }
                    });
                    */
                    sendMessage(session, sender, current_count + 1);
                }
            });
        } else {
            current_batch = batch_size;
            message.onAck(new Runnable() {
                public void run() {
                    sendMessage(session, sender, current_count + 1);
                }
            });
        }

        if (current_count >= count) {
            message.onAck(new Runnable() {
                public void run() {
                    sender.detach();
                }
            });
        }
    }
}

