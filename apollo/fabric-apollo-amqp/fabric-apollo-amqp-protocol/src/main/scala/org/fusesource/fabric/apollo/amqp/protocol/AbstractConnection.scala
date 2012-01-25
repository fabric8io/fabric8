/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.protocol

import api.Connection
import commands.CloseConnection
import collection.mutable.Queue
import interceptors.common.Multiplexer
import interceptors.connection.{TransportInterceptor, HeaderInterceptor, CloseInterceptor, HeartbeatInterceptor, OpenInterceptor}
import org.apache.activemq.apollo.util.{URISupport, IntrospectionSupport}
import java.net.URI
import org.apache.activemq.apollo.broker.transport.TransportFactory
import utilities.Tasks

/**
 *
 */

trait AbstractConnection extends Connection {

  val _transport = new TransportInterceptor
  val _header = new HeaderInterceptor
  val _close = new CloseInterceptor
  val _heartbeat = new HeartbeatInterceptor
  val _open = new OpenInterceptor
  val _sessions = new Multiplexer

  def connect(uri:String) = {
    IntrospectionSupport.setProperties(this, URISupport.parseParamters(new URI(uri)))
    _transport.transport = TransportFactory.connect(uri)
  }

  def error() = _transport.error

  def isConnected() = {
    Option(_transport.transport) match {
      case Some(t) =>
        t.isConnected
      case None =>
        false
    }
  }

  def onDisconnected(task: Runnable) = _transport.on_disconnect = () => { task.run }

  def getDispatchQueue = _transport.queue

  def setContainerID(id: String) = _open.open.setContainerID(id)

  def getContainerID = _open.open.getContainerID

  def close = _close.send(CloseConnection(), Tasks())

  def close(t: Throwable) = _close.send(CloseConnection(t), Tasks())

  def close(reason: String) = _close.send(CloseConnection(reason), new Queue[() => Unit])

  def getPeerContainerID = _open.peer.getContainerID

  def getIdleTimeout = _heartbeat.local_idle_timeout.getOrElse(-1)

  def setIdleTimeout(timeout:Long) = {
    if (timeout <= 0) {
      _heartbeat.local_idle_timeout = None
    } else {
      _heartbeat.local_idle_timeout = Option(timeout)
    }
  }

}