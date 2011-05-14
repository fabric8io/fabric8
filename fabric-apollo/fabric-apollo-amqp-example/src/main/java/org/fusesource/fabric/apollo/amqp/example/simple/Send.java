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
    public void onConnect(final Connection connection) {
        System.out.println("Connected, sending messages");
        final Session session = connection.createSession();
        final Sender sender = session.createSender();
        sender.setOnDetach(new Runnable() {
            public void run() {
                session.end(new Runnable() {
                    public void run() {
                        connection.close();
                    }
                });
            }
        });
        sender.setAddress(address);
        sender.attach(new Runnable() {
            public void run() {
                onAttach(connection, session, sender);
            }
        });
    }

    public void onAttach(final Connection connection, final Session session, final Sender sender) {
        sendMessage(session, sender, 1);
    }

    public void sendMessage(final Session session, final Sender sender, final int current_count) {
        Message message = session.createMessage();
        message.addBodyPart(Buffer.ascii(message_prefix + current_count));

        if (current_count >= count) {
            message.onAck(new Runnable() {
                public void run() {
                    sender.detach();
                }
            });
        } else {
            message.onAck(new Runnable() {
                public void run() {
                    sendMessage(session, sender, current_count + 1);
                }
            });
        }
        System.out.println("Sending message " + current_count);
        sender.put(message);
    }

    @Override
    public void printHelp() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
