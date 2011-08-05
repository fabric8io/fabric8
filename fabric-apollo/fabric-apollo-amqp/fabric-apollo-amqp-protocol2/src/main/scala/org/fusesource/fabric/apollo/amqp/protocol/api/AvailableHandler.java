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
 *
 */
public interface AvailableHandler {

    /**
     * Called by a sender when updating the peer with the sender's flow
     * state, the amount returned is the amount of message units available
     * at the sender to be sent
     * @param sender
     * @return
     */
    public int getAvailable(Sender sender);

}
