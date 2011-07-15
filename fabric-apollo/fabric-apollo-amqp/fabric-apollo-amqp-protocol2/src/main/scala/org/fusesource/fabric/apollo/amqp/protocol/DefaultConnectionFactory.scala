/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 * 	http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.protocol

import org.fusesource.fabric.apollo.amqp.api.ConnectionHandler

/**
 *
 */
class DefaultConnectionFactory extends ConnectionFactory {
  def createConnection() = new AMQPConnection

  def createServerConnection(handler: ConnectionHandler) = new AMQPServerConnection(handler)
}