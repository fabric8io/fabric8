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

import org.fusesource.fabric.apollo.amqp.codec.api.AnnotatedMessage;
import org.fusesource.fabric.apollo.amqp.codec.api.BareMessage;
import org.fusesource.fabric.apollo.amqp.codec.types.SenderSettleMode;
import org.fusesource.hawtbuf.Buffer;

/**
 *
 */
public interface Sender extends Link {

    /**
     * Returns whether or not link credit is available to send messages
     * @return
     */
    public boolean full();

    /**
     * Send a message over this link, returns false if the message cannot
     * be sent at this time due to lack of link credit
     * @param message
     * @return
     */
    public boolean offer(Buffer message);

    /**
     * Send a message over this link, returns false if the message cannot
     * be sent at this time due to lack of link credit
     * @param message
     * @return
     */
    public boolean offer(AnnotatedMessage message);

    /**
     * Send a message over this link, returns false if the message cannot
     * be sent at this time due to lack of link credit
     * @param message
     * @return
     */
    public boolean offer(BareMessage message);

    /**
     * Sets the tagger used to set delivery tags for outgoing messages
     * @param tagger
     */
    public void setTagger(DeliveryTagger tagger);

    /**
     * Sets the AvailableHandler that will be called when there is a need
     * to update the peer with this sender's flow state
     * @param handler
     */
    public void setAvailableHandler(AvailableHandler handler);

    /**
     * Sets the acknowledgement handler for this sender
     * @param handler
     */
    public void setAckHandler(AckHandler handler);

    /**
     * Sets the settle mode for this sender
     * @param mode
     */
    public void setSettleMode(SenderSettleMode mode);

    /**
     * Sets the task that the sender will call when there is sufficient
     * link credit to send messages
     * @param refiller
     */
    public void refiller(Runnable refiller);

    /**
     * Gets the current link credit available to this sender
     * @return
     */
    public int getLinkCredit();
}
