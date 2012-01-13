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

import org.fusesource.hawtdispatch._
import org.fusesource.hawtdispatch.transport._
import org.fusesource.fabric.apollo.amqp.protocol.api._
import org.fusesource.hawtdispatch.Dispatch
import org.apache.activemq.apollo.util.Logging
import java.util.UUID
import org.apache.activemq.apollo.broker.transport.TransportFactory

/**
 *
 */
class AMQPServerConnection(handler: ConnectionHandler) extends ServerConnection with TransportServerListener with Logging {

  var transport_server: TransportServer = null
  var container_id:String = null

  def setContainerID(id:String) = container_id = id
  def getContainerID = container_id

  def getListenPort = {
    transport_server match {
      case t: TcpTransportServer =>
        t.getSocketAddress.getPort
      case _ =>
        0
    }
  }

  def getListenHost = {
    transport_server match {
      case t: TcpTransportServer =>
        t.getSocketAddress.getHostName
      case _ =>
        ""
    }
  }

  def bind(uri: String, onComplete:Runnable) = {
    transport_server = TransportFactory.bind(uri)
    transport_server.setDispatchQueue(Dispatch.createQueue)
    transport_server.setTransportServerListener(this)
    transport_server.start(onComplete)
    Option(container_id) match {
      case Some(id) =>
      case None =>
        container_id = UUID.randomUUID().toString
    }
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

  def unbind = transport_server.stop(NOOP)

}

