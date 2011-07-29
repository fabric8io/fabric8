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
import commands._
import utilities._
import interceptors.connection.ConnectionFrameBarrier
import org.fusesource.fabric.apollo.amqp.protocol.api._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces._
import org.fusesource.fabric.apollo.amqp.codec.types.{Begin, End, AMQPTransportFrame}
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor._
import org.apache.activemq.apollo.util.{Log, Logging}

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
class AMQPConnection extends Interceptor with AbstractConnection with Logging {

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

  trace("Constructed connection chain : %s", display_chain(this))

  def createSession() = {
    val rc = new AMQPSession
    rc.queue = getDispatchQueue
    _sessions.attach(rc.head)
    rc.asInstanceOf[Session]
  }

  def setSessionHandler(handler: SessionHandler) = {
    _sessions.chain_attached = Option((chain:Interceptor) => {
      chain.head.foreach((x) => if (x.isInstanceOf[AMQPSession]) {
        handler.sessionCreated(x.asInstanceOf[Session])
      })
    })

    _sessions.chain_released = Option((chain:Interceptor) => {
      chain.head.foreach((x) => if (x.isInstanceOf[AMQPSession]) {
        handler.sessionReleased(x.asInstanceOf[Session])
      })
    })
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
    if (_open.sent && _open.received) {
      info("Open frames exchanged")
      on_connect.foreach((x) => x())
    }
  }

  def header_sent_or_received = {
      info("AMQP protocol header frames exchanged")
  }

  protected def _send(frame: AMQPFrame, tasks: Queue[() => Unit]) = outgoing.send(frame, tasks)

  protected def _receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case o:HeaderSent =>
        header_sent_or_received
        execute(tasks)
      case o:HeaderReceived =>
        header_sent_or_received
        execute(tasks)
      case o:OpenReceived =>
        if (!_open.sent) {
          queue {
            send(SendOpen(), Tasks())
          }
        }
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
