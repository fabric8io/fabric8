/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.session

import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{execute, Tasks}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{BeginSession, BeginSent, BeginReceived}
import org.fusesource.hawtbuf.Buffer
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.{FrameInterceptor, PerformativeInterceptor, Interceptor}
import org.fusesource.fabric.apollo.amqp.codec.types._

/**
 *
 */

class BeginInterceptor extends PerformativeInterceptor[Begin] with Logging {

  var set_outgoing_window:Option[() => Long] = None
  var set_incoming_window:Option[() => Long] = None

  var sent = false
  var received = false

  val begin = new Begin()
  var peer:Option[Begin] = None

  var remote_channel = 0

  val channel_interceptor = new FrameInterceptor[AMQPTransportFrame] {
    override protected def receive_frame(f:AMQPTransportFrame, tasks:Queue[() => Unit]) = {
      remote_channel = f.getChannel
      incoming.receive(f, tasks)
    }
  }

  val session_started = new FrameInterceptor[BeginSession] {
    override protected def send_frame(b:BeginSession, tasks:Queue[() => Unit]) = {
      send_begin
      execute(tasks)
    }
  }

  override protected def adding_to_chain = {
    before(channel_interceptor)
    after(session_started)
  }

  override protected def removing_from_chain = {
    if (channel_interceptor.connected) {
      channel_interceptor.remove
    }
    if (session_started.connected) {
      session_started.remove
    }
  }

  override protected def send(b:Begin, payload:Buffer, tasks:Queue[() => Unit]):Boolean = {
    if (!sent) {
      sent = true
      tasks.enqueue( () => {
        if (connected) {
          receive(BeginSent(), Tasks())
        }
      })
      false
    } else {
      execute(tasks)
      true
    }
  }

  override protected def receive(b:Begin, payload:Buffer, tasks:Queue[() => Unit]):Boolean = {
    if (!received) {
      received = true
      peer = Option(b)
      begin.setRemoteChannel(remote_channel)
      channel_interceptor.remove
      receive(BeginReceived(), tasks)
    } else {
      val close = new Close
      close.setError(new Error(AMQPError.ILLEGAL_STATE.getValue, "Session has already received a begin frame"))
      send(new AMQPTransportFrame(close), tasks)
    }
    true
  }

  def send_begin = {
    set_outgoing_window.foreach((x) => begin.setOutgoingWindow(x()))
    set_incoming_window.foreach((x) => begin.setIncomingWindow(x()))
    begin.setNextOutgoingID(0L)
    /*
    peer match {
      case Some(peer) =>
        Option(peer.getRemoteChannel) match {
          case Some(channel) =>
          case None =>
            remote_channel match {
              case Some(channel) =>
                begin.setRemoteChannel(channel)
              case None =>
                throw new RuntimeException("Remote channel must be sent but is not set")
            }
        }
      case None =>
    }*/
    send(new AMQPTransportFrame(begin), Tasks())
  }

}