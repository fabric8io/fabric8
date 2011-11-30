/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.api;

import org.fusesource.fabric.apollo.amqp.codec.api.BareMessage;

/**
 *
 * A callback used to notify when new messages arrive at a receiver
 *
 * @author
 *
 */
public interface MessageListener {

    /**
     * Called when the Receiver wants to check if this listener can accept a message
     * @return
     */
    public boolean full();

    /**
     * Called when a new message arrives at the Receiver
     * @param receiver the Receiver the messaged arrived on
     * @param message
     * @return whether or not more messages can be accepted by this listener
     */
    public boolean offer(Receiver receiver, BareMessage message);

    /**
     * Called to supply a refiller task when this listener is ready to accept more messages
     * @param refiller
     */
    public void refiller(Runnable refiller);

    /**
     * Called by the Receiver when messages are available but the source has insufficient link credit to send those messages
     * @param available the number of messages available
     * @return the amount of link credit to supply to the source
     */
    public long needLinkCredit(long available);


}
