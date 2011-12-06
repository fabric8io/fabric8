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

import org.fusesource.fabric.apollo.amqp.protocol.AMQPConnection;

/**
 *
 */
public class AMQPConnectionFactory {

    public static Connection createConnection() {
        return AMQPConnection.createConnection();
    }

    public static ServerConnection createServerConnection(ConnectionHandler handler) {
        return AMQPConnection.createServerConnection(handler);
    }
}
