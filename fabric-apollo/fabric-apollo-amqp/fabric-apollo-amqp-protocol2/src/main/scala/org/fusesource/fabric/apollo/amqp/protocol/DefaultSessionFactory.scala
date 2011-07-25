/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol

import interfaces.{ProtocolConnection, SessionFactory}
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.ProtocolSession

/**
 *
 */
class DefaultSessionFactory extends SessionFactory {
  def create_session(connection: Interceptor):ProtocolSession = new AMQPSession(connection)
}
