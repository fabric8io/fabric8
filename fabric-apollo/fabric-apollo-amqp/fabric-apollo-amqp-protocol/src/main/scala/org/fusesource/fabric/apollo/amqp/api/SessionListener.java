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
 * A callback used by a Connection to communicate session creation/destruction
 */
public interface SessionListener {

    /**
     * Called when a new session is being created on a connection
     * @param connection The connection where the session is being created
     * @param session The session being created
     */
    public void sessionCreated(Connection connection, Session session);

    /**
     * Called when a session is being released from a connection
     * @param connection The connection releasing the session
     * @param session The session being released
     */
    public void sessionReleased(Connection connection, Session session);
}
