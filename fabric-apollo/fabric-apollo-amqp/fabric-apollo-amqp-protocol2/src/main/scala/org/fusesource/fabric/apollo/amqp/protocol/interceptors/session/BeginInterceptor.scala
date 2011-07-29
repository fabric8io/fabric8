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

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{Execute, Tasks}
import org.fusesource.fabric.apollo.amqp.codec.types.{End, AMQPTransportFrame, Begin}

/**
 *
 */

class BeginInterceptor extends Interceptor with Logging {

  var set_outgoing_window:Option[() => Long] = None
  var set_incoming_window:Option[() => Long] = None

  var sent = false
  var received = false

  val begin = new Begin()
  var peer:Begin = null
  var remote_channel:Option[Int] = None

  var on_begin:Option[() => Unit] = None

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case t:AMQPTransportFrame =>
        t.getPerformative match {
          case b:Begin =>
            if (!sent) {
              sent = true
              outgoing.send(frame, tasks)
              maybe_remove
            } else {
              Execute(tasks)
            }
          case _ =>
            if (sent) {
              outgoing.send(frame, tasks)
            } else {
              debug("Session hasn't been started, dropping outgoing frame %s", frame)
              Execute(tasks)
            }
        }
      case _ =>
        outgoing.send(frame, tasks)
    }
  }

  def maybe_remove {
    if (sent && received) {
      remove
      on_begin.foreach((x) => x())
    }
  }

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case t:AMQPTransportFrame =>
        t.getPerformative match {
          case b:Begin =>
            received = true
            peer = b
            remote_channel = Option(t.getChannel)
            Execute(tasks)
            maybe_remove
          case _ =>
            incoming.receive(frame, tasks)
        }
      case _ =>
        incoming.receive(frame, tasks)
    }
  }

  def send_begin = {
    set_outgoing_window.foreach((x) => begin.setOutgoingWindow(x()))
    set_incoming_window.foreach((x) => begin.setIncomingWindow(x()))
    begin.setNextOutgoingID(0L)
    Option(peer) match {
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
    }
    send(new AMQPTransportFrame(begin), Tasks())
    maybe_remove
  }

}