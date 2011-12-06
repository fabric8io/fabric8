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
 * A callback used to notify when a new incoming client connection is accepted
 *
 * @author Stan Lewis
 */
public interface ConnectionHandler {

    /**
     * Called when a new connection is created
     *
     * @param connection the connection being created
     */
    public void connectionCreated(Connection connection);
}
