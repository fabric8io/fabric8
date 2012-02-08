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

package org.fusesource.fabric.apollo.amqp.example.simple;

import org.fusesource.fabric.apollo.amqp.api.Message;
import org.fusesource.fabric.apollo.amqp.api.Sender;
import org.fusesource.fabric.apollo.amqp.api.Session;

/**
 * Simple AMQP sender that submits a batch of messages and waits
 * for the last message in the batch to be acknowledged before
 * sending more messages.
 */
public class BatchSend extends Send {

    private long current_batch;

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

