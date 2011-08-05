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
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{execute, Tasks}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{EndReceived, EndSent, EndSession}
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.{FrameInterceptor, PerformativeInterceptor, Interceptor}
import org.fusesource.hawtbuf.Buffer

/**
 *
 */

class EndInterceptor extends PerformativeInterceptor[End] with Logging {

  var sent = false
  var received = false

  val session_ender = new FrameInterceptor[EndSession] {
    override protected def send_frame(e:EndSession, tasks:Queue[() => Unit]) = {
      val end = new End
      e.reason match {
        case Some(reason) =>
          end.setError(new Error(AMQPError.INTERNAL_ERROR.getValue, reason))
        case None =>
      }
      e.exception match {
        case Some(reason) =>
          end.setError(new Error(AMQPError.INTERNAL_ERROR.getValue, reason.toString))
        case None =>
      }
      send(new AMQPTransportFrame(end), tasks)
    }
  }

  val exception_catcher = new Interceptor {
    override protected def _receive(frame:AMQPFrame, tasks:Queue[() => Unit]) {
      try {
        incoming.receive(frame, tasks)
      } catch {
        case t:Throwable =>
          warn("Exception processing frame : %s, error is %s", frame, t)
          warn("Exception stack trace : \n%s", t.getStackTraceString)
          send(EndSession(t), tasks)
      }
    }
  }

  override protected def adding_to_chain = {
    after(exception_catcher)
    after(session_ender)
  }

  override protected def removing_from_chain = {
    exception_catcher.remove
    session_ender.remove
  }

  override protected def send(e:End, payload:Buffer, tasks:Queue[() => Unit]) = {
    if (!sent) {
      sent = true
      tasks.enqueue(() => receive(EndSent(), Tasks()))
      false
    } else {
      execute(tasks)
      true
    }
  }

  override protected def receive(e:End, payload:Buffer, tasks:Queue[() => Unit]) = {
    if (!received) {
      received = true
      incoming.receive(EndReceived(), tasks)
    } else {
      execute(tasks)
    }
    true
  }
}