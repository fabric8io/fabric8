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

/**
 *
 * A callback used to notify when a new incoming or outgoing link is either attached to or detached from a session
 *
 * @author Stan Lewis
 *
 */
public interface LinkListener {

    /**
     * Called when a remote sender is attaching to this session
     * @param session the session the remote sender is attaching to
     * @param receiver the receiver created by the session to receive messages
     */
    public void senderAttaching(Session session, Receiver receiver);

    /**
     * Called when a remote receiver is attaching to this session
     * @param session the session the remote receiver is attaching to
     * @param sender the sender created by the session to send messages
     */
    public void receiverAttaching(Session session, Sender sender);

    /**
     * Called when a remote sender is detaching from this session
     * @param session the session the remote sender is detaching from
     * @param receiver the receiver that had been created by the session to receive messages
     */
    public void senderDetaching(Session session, Receiver receiver);

    /**
     * Called when a remote receiver is detaching from this session
     * @param session the session the remote receiver is detaching from
     * @param sender the sender that had been created by the session to send messages
     */
    public void receiverDetaching(Session session, Sender sender);

}
