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
 * A callback used to notify when a new incoming or outgoing link is either attached to or detached from a session
 *
 * @author Stan Lewis
 */
public interface LinkHandler {

    /**
     * @param session the session the remote link is attaching to
     * @param peer
     */
    public void linkAttaching(Session session, Link peer);

    /**
     * @param session the session the remote link is detaching from
     * @param peer
     */
    public void linkDetaching(Session session, Link peer);

}
