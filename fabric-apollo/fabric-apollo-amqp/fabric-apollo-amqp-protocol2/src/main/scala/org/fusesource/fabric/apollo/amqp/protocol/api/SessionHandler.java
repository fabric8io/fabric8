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
 * A callback used by a Connection to communicate session creation/destruction
 */
public interface SessionHandler {

    /**
     * Called when a new session is being created on a connection
     *
     * @param session    The session being created
     */
    public void sessionCreated(Session session);

    /**
     * Called when a session is being released from a connection
     *
     * @param session    The session being released
     */
    public void sessionReleased(Session session);
}
