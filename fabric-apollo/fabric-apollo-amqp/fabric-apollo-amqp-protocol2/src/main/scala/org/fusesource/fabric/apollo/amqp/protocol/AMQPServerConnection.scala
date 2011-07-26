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

import org.apache.activemq.apollo.transport._
import tcp.TcpTransportServer
import org.fusesource.fabric.apollo.amqp.protocol.api._
import org.fusesource.hawtdispatch.Dispatch
import org.apache.activemq.apollo.util.Logging

/**
 *
 */
class AMQPServerConnection(handler: ConnectionHandler) extends ServerConnection with TransportAcceptListener with Logging {

  var transportServer: TransportServer = null
  var container_id:String = ""

  override def setContainerID(id:String) = container_id = id
  override def getContainerID = container_id

  def getListenPort = {
    transportServer match {
      case t: TcpTransportServer =>
        t.getSocketAddress.getPort
      case _ =>
        0
    }
  }

  def getListenHost = {
    transportServer match {
      case t: TcpTransportServer =>
        t.getSocketAddress.getHostName
      case _ =>
        ""
    }
  }

  def bind(uri: String, onComplete:Runnable) = {
    transportServer = TransportFactory.bind(uri)
    transportServer.setDispatchQueue(Dispatch.createQueue)
    transportServer.setAcceptListener(this)
    transportServer.start(onComplete)
    info("AMQP Server listening on %s:%s", getListenHost, getListenPort)
  }

  def onAccept(transport: Transport) = {
    val connection = new AMQPConnection
    connection.setContainerID(container_id)
    val clientUri = transport.getTypeId + ":/" + transport.getRemoteAddress
    info("Client connected from %s", clientUri)
    handler.connectionCreated(connection)
    connection._transport.transport = transport
  }

  def onAcceptError(error: Exception) = {
    // TODO
  }

}

