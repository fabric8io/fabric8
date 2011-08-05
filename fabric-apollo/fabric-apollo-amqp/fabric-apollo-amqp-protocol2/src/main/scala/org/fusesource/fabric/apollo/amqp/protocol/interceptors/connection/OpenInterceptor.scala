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

import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.types.{Open, AMQPTransportFrame}
import org.fusesource.fabric.apollo.amqp.codec.AMQPDefinitions
import java.util.UUID
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{Tasks, execute}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{OpenReceived, OpenSent, HeaderSent}
import org.apache.activemq.apollo.util.Logging
import org.fusesource.hawtbuf.Buffer
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.{FrameInterceptor, PerformativeInterceptor}
import org.fusesource.hawtdispatch._
/**
 *
 */
class OpenInterceptor extends PerformativeInterceptor[Open] with Logging {

  var sent = false
  var received = false
  val open = new Open
  var peer:Open = new Open
  peer.setMaxFrameSize(AMQPDefinitions.MIN_MAX_FRAME_SIZE.asInstanceOf[Int])
  peer.setChannelMax(0)

  val sender = new FrameInterceptor[HeaderSent] {
      override protected def receive_frame(h:HeaderSent, tasks: Queue[() => Unit]) = {
        queue {
          send_open
        }
        tasks.enqueue(() => remove)
        incoming.receive(h, tasks)
      }
    }

  override protected def adding_to_chain = {
    before(sender)
  }

  override protected def removing_from_chain = {
    if (sender.connected) {
      sender.remove
    }
  }

  override protected def send(o:Open, payload:Buffer, tasks: Queue[() => Unit]) = {
    if (!sent) {
      sent = true
      tasks.enqueue( () => {
        receive(OpenSent(), Tasks())
      })
      false
    } else {
      execute(tasks)
      true
    }
  }

  override protected def receive(o:Open, payload:Buffer, tasks: Queue[() => Unit]) = {
    if (!received) {
      received = true
      peer = o
      receive(OpenReceived(), tasks)
    } else {
      execute(tasks)
    }
    true
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