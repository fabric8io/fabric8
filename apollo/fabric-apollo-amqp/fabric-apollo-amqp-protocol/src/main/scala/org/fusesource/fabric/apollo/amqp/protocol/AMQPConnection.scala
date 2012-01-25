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

import org.fusesource.hawtdispatch._
import org.fusesource.hawtbuf.Buffer
import commands._
import utilities._
import interceptors.connection.ConnectionFrameBarrier
import org.fusesource.fabric.apollo.amqp.protocol.api._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces._
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor._
import org.apache.activemq.apollo.util.{Log, Logging}
import org.fusesource.fabric.apollo.amqp.codec.types._
import collection.mutable.Queue

/**
 *
 */
object AMQPConnection {

  val DEFAULT_DIE_DELAY = 1 * 1000
  var die_delay = DEFAULT_DIE_DELAY

  val DEFAULT_HEARTBEAT = 10 * 1000L

  def createConnection: Connection = new AMQPConnection().asInstanceOf[Connection]

  def createServerConnection(handler: ConnectionHandler) = new AMQPServerConnection(handler).asInstanceOf[ServerConnection]

}

/**
 *
 */
class AMQPConnection extends FrameInterceptor[ConnectionCommand] with AbstractConnection with Logging {

  import AMQPConnection._
  _transport.tail.incoming = _header
  _transport.tail.incoming = _close
  _transport.tail.incoming = _heartbeat
  _transport.tail.incoming = _open
  _transport.tail.incoming = this
  _transport.tail.incoming = new ConnectionFrameBarrier
  _transport.tail.incoming = _sessions

  _sessions.interceptors.reserve(1)

  var on_connect:Option[() => Unit] = None

  _sessions.interceptor_factory = Option((frame:AMQPTransportFrame) => {
    frame.getPerformative match {
      case b:Begin =>
        val rc = new AMQPSession
        rc.connection = this
        rc.head
      case _ =>
        throw new RuntimeException("Frame received for non-existant session : {" + frame + "}")
    }
  })

  _sessions.channel_selector = Option((frame:AMQPTransportFrame) => frame.getChannel)
  _sessions.outgoing_channel_setter = Option((channel:Int, frame:AMQPTransportFrame) => frame.setChannel(channel))
  _sessions.channel_mapper = Option((frame:AMQPTransportFrame) => {
      frame.getPerformative match {
        case b:Begin =>
          Option(b.getRemoteChannel) match {
            case Some(i) =>
              Option(i.intValue)
            case None =>
              None
          }
        case _ =>
          None
      }
    })

  _transport.after(new PerformativeInterceptor[Close] {
    override protected def send(c:Close, payload:Buffer, tasks:Queue[() => Unit]):Boolean = {
      tasks.enqueue(() => {
        _sessions.foreach_chain((x) => _sessions.release(x))
      })
      false
    }
  })

  trace("Constructed connection chain : %s", display_chain(this))

  def createSession() = {
    val rc = new AMQPSession
    rc.connection = this
    rc.queue = getDispatchQueue
    _sessions.attach(rc.head)
    rc.asInstanceOf[Session]
  }

  def setSessionHandler(handler: SessionHandler) = {
    _sessions.chain_attached = Option((chain:Interceptor) => {
      chain.head.foreach((x) => x match {
        case s:AMQPSession =>
          s.on_begin_received = Option( () => {
            handler.sessionCreated(s)
          })
          s.on_end_received = Option( () => {
            handler.sessionReleased(s)
          })
        case _ =>
      })
    })

    _sessions.chain_released = Option((chain:Interceptor) => {})
  }

  def onConnected(task: Runnable) {
    Option(task) match {
      case Some(task) =>
        on_connect = Option(() => task.run)
      case None =>
        on_connect = None
    }
  }

  def open_sent_or_received = {
    if (_open.sent && _open.received && _open.connected) {
      info("Open frames exchanged")
      on_connect.foreach((x) => x())
      _open.remove
    }
  }

  def header_sent_or_received = {
    if (_header.sent && _header.received && _header.connected) {
      info("AMQP protocol header frames exchanged")
      _header.remove
    }
  }

  override protected def receive_frame(frame: ConnectionCommand, tasks: Queue[() => Unit]) = {
    frame match {
      case x:ConnectionCreated =>
        execute(tasks)
      case x:ConnectionClosed =>
        _sessions.foreach_chain((x) => _sessions.release(x))
        execute(tasks)
      case o:HeaderSent =>
        header_sent_or_received
        execute(tasks)
      case o:HeaderReceived =>
        header_sent_or_received
        execute(tasks)
      case o:OpenReceived =>
        open_sent_or_received
        execute(tasks)
      case o:OpenSent =>
        open_sent_or_received
        execute(tasks)
      case _ =>
        incoming.receive(frame, tasks)
    }
  }
}
