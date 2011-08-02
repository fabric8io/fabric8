/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.connection

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import java.util.concurrent.atomic.AtomicBoolean
import org.fusesource.fabric.apollo.amqp.codec.types.{Open, AMQPTransportFrame}
import org.fusesource.fabric.apollo.amqp.codec.AMQPDefinitions
import java.util.UUID
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{Tasks, execute}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{SendOpen, OpenReceived, OpenSent, HeaderSent}

/**
 *
 */
class OpenInterceptor extends Interceptor {

  var sent = false
  var received = false
  val open = new Open
  var peer:Open = new Open
  peer.setMaxFrameSize(AMQPDefinitions.MIN_MAX_FRAME_SIZE.asInstanceOf[Int])
  peer.setChannelMax(0)

  override protected def _send(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case f:AMQPTransportFrame =>
        f.getPerformative match {
          case o:Open =>
            if (!sent) {
              sent = true
                tasks.enqueue( () => {
                  receive(OpenSent(), Tasks())
                })
              outgoing.send(frame, tasks)
            } else {
              execute(tasks)
            }
          case _ =>
            outgoing.send(frame, tasks)
        }
      case o:SendOpen =>
        execute(tasks)
        send_open
      case _ =>
        outgoing.send(frame, tasks)
    }
  }

  override protected def _receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case h:HeaderSent =>
        send_open
        execute(tasks)
      case f:AMQPTransportFrame =>
        f.getPerformative match {
          case o:Open =>
            peer = o
            received = true
            incoming.receive(OpenReceived(), tasks)
          case _ =>
            incoming.receive(frame, tasks)
        }
      case _ =>
        incoming.receive(frame, tasks)
    }
  }

  def send_open = {
    Option(open.getContainerID) match {
      case Some(id) =>
      case None =>
        open.setContainerID(UUID.randomUUID.toString)
    }
    send(new AMQPTransportFrame(open), Tasks())
  }

}