package org.fusesource.fabric.apollo.amqp.protocol.interceptors.link

/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

import org.apache.activemq.apollo.util.Logging
import org.fusesource.hawtbuf.Buffer
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.AMQPLink
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{Tasks, execute}
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.{FrameInterceptor, PerformativeInterceptor}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{AttachReceived, ChainAttached, AttachSent}

/**
 *
 */
class AttachInterceptor extends PerformativeInterceptor[Attach] with Logging {

  var sent = false
  var received = false
  var configure_attach:Option[(Attach) => Attach] = None

  def send_attach:Unit = {
    configure_attach match {
      case Some(init) =>
        send(new AMQPTransportFrame(init(new Attach)), Tasks())
      case None =>
        throw new RuntimeException("Link not configured")
    }
  }

  override protected def send(performative: Attach, payload: Buffer, tasks: Queue[() => Unit]) = {
    if (!sent) {
      sent = true
      tasks.enqueue( () => {
        receive(AttachSent(), Tasks())
      })
      false
    } else {
      execute(tasks)
      true
    }

  }

  override protected def receive(performative: Attach, payload: Buffer, tasks: Queue[() => Unit]) = {
    if (!received) {
      received = true
      receive(AttachReceived(), Tasks())
      execute(tasks)
      true
    } else {
      val detach = new Detach(0, true, new Error(AMQPError.ILLEGAL_STATE.getValue, "Double attach"))
      send(new AMQPTransportFrame(detach), tasks)
      true
    }
  }
}