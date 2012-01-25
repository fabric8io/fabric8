/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

