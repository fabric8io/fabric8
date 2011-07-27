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

import interceptors.connection.ConnectionFrameBarrier
import org.fusesource.fabric.apollo.amqp.protocol.api._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces._

/**
 *
 */
object AMQPConnection {

  val DEFAULT_DIE_DELAY = 1 * 1000
  var die_delay = DEFAULT_DIE_DELAY

  val DEFAULT_HEARTBEAT = 10 * 1000L

  var factory: ConnectionFactory = null

  def createConnection: Connection = {
    if ( factory == null ) {
      factory = new DefaultConnectionFactory
    }
    factory.createConnection
  }

  def createServerConnection(handler: ConnectionHandler): ServerConnection = {
    if ( factory == null ) {
      factory = new DefaultConnectionFactory
    }
    factory.createServerConnection(handler)
  }

}

/**
 *
 */
class AMQPConnection extends AbstractConnection {

  import AMQPConnection._

  _transport.tail.incoming = _header
  _transport.tail.incoming = _close
  _transport.tail.incoming = _heartbeat
  _transport.tail.incoming = _open
  _transport.tail.incoming = new ConnectionFrameBarrier
  _transport.tail.incoming = _sessions

  def createSession() = {
    val rc = new AMQPSession
    _sessions.attach(rc)
    rc
  }

  def setSessionHandler(handler: SessionHandler) = {
    _sessions.chain_attached = Option((chain:Interceptor) => {
      chain.head.foreach((x) => if (x.isInstanceOf[Session]) {
        handler.sessionCreated(x.asInstanceOf[Session])
      })
    })

    _sessions.chain_released = Option((chain:Interceptor) => {
      chain.head.foreach((x) => if (x.isInstanceOf[Session]) {
        handler.sessionReleased(x.asInstanceOf[Session])
      })
    })
  }

}
