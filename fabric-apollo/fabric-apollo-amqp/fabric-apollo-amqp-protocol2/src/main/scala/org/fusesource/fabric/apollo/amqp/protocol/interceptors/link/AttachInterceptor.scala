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

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.PerformativeInterceptor
import org.apache.activemq.apollo.util.Logging
import org.fusesource.hawtbuf.Buffer
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.AMQPLink
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{Tasks, execute}
import org.fusesource.fabric.apollo.amqp.codec.types._

/**
 *
 */
class AttachInterceptor extends PerformativeInterceptor[Attach] with Logging {

  var sent = false
  var received = false

  def send_attach(link:AMQPLink) = {
    val attach = new Attach(link.getName, 0, link.getRole.getValue)
    attach.setTarget(link.getTarget)
    attach.setSource(link.getSource)
    send(attach, null, Tasks())

  }


  override protected def send(performative: Attach, payload: Buffer, tasks: Queue[() => Unit]) = {
    if (!sent) {
      sent = true
      false
    } else {
      execute(tasks)
      true
    }

  }

  override protected def receive(performative: Attach, payload: Buffer, tasks: Queue[() => Unit]) = {
    if (!received) {
      received = true
      execute(tasks)
      true
    } else {
      val detach = new Detach(0, true, new Error(AMQPError.ILLEGAL_STATE.getValue, "D{ouble attach"))
      send(new AMQPTransportFrame(detach), tasks)
      true
    }
  }
}