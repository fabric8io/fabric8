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
import org.fusesource.fabric.apollo.amqp.protocol.commands.{OpenSent, HeaderSent}
import java.util.UUID
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{Tasks, Execute}

object OpenInterceptor {
  // TODO - probably gonna be a few possibilities here...
  val error = () => {
    throw new RuntimeException("")
  }
}

/**
 *
 */
class OpenInterceptor extends Interceptor {
  import OpenInterceptor._

  val sent = new AtomicBoolean(false)
  val open = new Open
  var peer:Open = new Open
  peer.setMaxFrameSize(AMQPDefinitions.MIN_MAX_FRAME_SIZE.asInstanceOf[Int])
  peer.setChannelMax(0)

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case f:AMQPTransportFrame =>
        if (f.getPerformative == null) {
          outgoing.send(frame, tasks)
        }
        f.getPerformative match {
          case o:Open =>
            if (!sent.getAndSet(true)) {
              if (!tasks.contains(error)) {
                tasks.enqueue( () => {
                  receive(OpenSent(), Tasks())
                })
                tasks.enqueue(rm)
              }
              outgoing.send(frame, tasks)
            } else {
              Execute(tasks)
            }
          case _ =>
            outgoing.send(frame, tasks)
        }
      case _ =>
        outgoing.send(frame, tasks)
    }
  }

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {

    def send_open = {
      Option(open.getContainerID) match {
        case Some(id) =>
        case None =>
          open.setContainerID(UUID.randomUUID.toString)
      }
      send(new AMQPTransportFrame(open), tasks)
    }

    frame match {
      case h:HeaderSent =>
        send_open
      case f:AMQPTransportFrame =>
        f.getPerformative match {
          case o:Open =>
            peer = o
            send_open
          case _ =>
            incoming.receive(frame, tasks)
        }
      case _ =>
        incoming.receive(frame, tasks)
    }
  }

}