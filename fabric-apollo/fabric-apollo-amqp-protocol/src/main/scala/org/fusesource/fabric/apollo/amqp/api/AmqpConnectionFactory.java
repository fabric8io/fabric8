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

import org.fusesource.fabric.apollo.amqp.protocol.AmqpConnection;

/**
 *
 * A factory for AMQP connections
 *
 * @author Stan Lewis
 *
 */
public class AmqpConnectionFactory {

    /**
     * Create a new client AMQP connection
     * @return an unconnected AMQP Connection object
     */
    public static Connection create() {
        return AmqpConnection.connection();
    }

    /**
     * Create a new server AMQP connection
     * @param listener A listener that will be called on as new clients connect to this AMQP server
     * @return an unbound AMQP ServerConnection object
     */
    public static ServerConnection createServer(ConnectionListener listener) {
        return AmqpConnection.serverConnection(listener);
    }

}
