/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
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
