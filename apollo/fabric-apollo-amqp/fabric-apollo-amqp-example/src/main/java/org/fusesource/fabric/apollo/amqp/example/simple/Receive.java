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
