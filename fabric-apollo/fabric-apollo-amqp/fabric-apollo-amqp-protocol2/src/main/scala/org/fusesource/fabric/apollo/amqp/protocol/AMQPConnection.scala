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

import commands.CloseConnection
import interceptors._
import org.fusesource.hawtdispatch._
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.Codec._
import org.fusesource.fabric.apollo.amqp.protocol.api._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces._
import collection.mutable.Queue

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
class AMQPConnection extends Connection with Logging {

  import AMQPConnection._

  val transport = new TransportInterceptor
  val header = new HeaderInterceptor
  val close = new CloseInterceptor
  val heartbeat = new HeartbeatInterceptor
  val open = new OpenInterceptor
  val sessions = new Multiplexer

  transport.tail.incoming = header
  transport.tail.incoming = close
  transport.tail.incoming = heartbeat
  transport.tail.incoming = open
  transport.tail.incoming = sessions

  def connect(uri: String) {}

  def onConnected(task: Runnable) = transport.on_connect = () => { task.run }

  def onDisconnected(task: Runnable) = transport.on_disconnect = () => { task.run }

  def createSession() = null

  def connected() = false

  def error() = null

  def getDispatchQueue = transport.dispatch_queue

  def setContainerID(id: String) = open.open.setContainerID(id)

  def getContainerID = open.open.getContainerID

  def close(t: Throwable) = close.send(CloseConnection(t), new Queue[() => Unit])

  def close(reason: String) = close.send(CloseConnection(reason), new Queue[() => Unit])

  def setSessionHandler(handler: SessionHandler) {}

  def getPeerContainerID = open.peer.getContainerID
}
