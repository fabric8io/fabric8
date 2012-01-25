/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.example.simple;

import org.fusesource.fabric.apollo.amqp.api.Connection;
import org.fusesource.fabric.apollo.amqp.api.Message;
import org.fusesource.fabric.apollo.amqp.api.Sender;
import org.fusesource.fabric.apollo.amqp.api.Session;
import org.fusesource.hawtbuf.Buffer;

import static org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory.createAmqpLong;
import static org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory.createAmqpSymbol;

/**
 * Simple AMQP sender that sends a message and waits for message
 * acknowledgement before sending the next message
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
        println("Connected, sending %s messages", count);
        final Session session = connection.createSession();
        final Sender sender = session.createSender();

        sender.getSourceOptionsMap().put(createAmqpSymbol("batch-size"), createAmqpLong(batch_size));

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

        configureMessageTasks(session, sender, current_count, message);

        println("Sending message %s : %s", current_count, message.getBodyPart(0));
        sender.put(message);
    }

    public void configureMessageTasks(final Session session, final Sender sender, final int current_count, final Message message) {
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
    }

    @Override
    public void printHelp() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
