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

package org.fusesource.fabric.apollo.amqp.protocol.api;

/**
 * A callback used to notify when new messages arrive at a receiver
 *
 * @author
 */
public interface MessageHandler<T> {

    /**
     * Called by the Receiver to check if this listener can accept a message
     *
     * @return
     */
    public boolean full();

    /**
     * Called when a new message arrives at the Receiver
     *
     * @param receiver the Receiver the messaged arrived on
     * @param deliveryId the deliver ID of the message
     * @param message
     * @return whether or not more messages can be accepted by this listener
     */
    public boolean offer(Receiver receiver, long deliveryId, T message);

    /**
     * Called to supply a refiller task when this listener is ready to accept more
     * messages.  Link credit should be added before this refiller task is run
     *
     * @param refiller
     */
    public void refiller(Runnable refiller);

}
