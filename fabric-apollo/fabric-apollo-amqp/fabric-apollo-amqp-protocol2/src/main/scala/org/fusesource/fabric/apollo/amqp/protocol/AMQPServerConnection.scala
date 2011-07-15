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

/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

import org.apache.activemq.apollo.transport._
import tcp.TcpTransportServer
import org.fusesource.fabric.apollo.amqp.api._

/**
 *
 */
class AMQPServerConnection(handler: ConnectionHandler) extends AMQPConnection with ServerConnection with TransportAcceptListener {
  var transportServer: TransportServer = null

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

  def bind(uri: String) = {
    init(uri)
    transportServer = TransportFactory.bind(uri)
    transportServer.setDispatchQueue(dispatch_queue)
    transportServer.setAcceptListener(this)
    transportServer.start()
    info("AMQP Server listening on %s:%s", getListenHost, getListenPort)
  }

  def onAccept(transport: Transport) = {
    val connection = new AMQPServerConnection(null)
    connection.setContainerID(container_id)
    val clientUri = transport.getTypeId + ":/" + transport.getRemoteAddress
    info("Client connected from %s", clientUri)
    connection.connect(Option(transport), uri.toString)
    //trace("Created AmqpConnection %s", connection)
    handler.connectionCreated(connection)
  }

  def onAcceptError(error: Exception) = {

  }

  override def toString = {
    val rc = new StringBuilder(getClass.getSimpleName)
    rc.append("{")
    Option(transportServer) match {
      case Some(transport) =>
        rc.append("local=")
        rc.append(transport.getConnectAddress)
      case None =>
    }
    Option(transport) match {
      case Some(transport) =>
        rc.append(" remote=")
        rc.append(transport.getRemoteAddress)
      case None =>
    }
    rc.append("}")
    rc.toString
  }

}




