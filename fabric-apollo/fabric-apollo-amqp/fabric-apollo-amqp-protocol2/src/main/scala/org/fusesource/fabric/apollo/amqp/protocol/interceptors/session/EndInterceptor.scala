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
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{execute, Tasks}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{EndReceived, EndSent, EndSession}

/**
 *
 */

class EndInterceptor extends Interceptor with Logging {

  var sent = false
  var received = false

  protected def _send(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case f:AMQPTransportFrame =>
        f.getPerformative match {
          case e:End =>
            if (!sent) {
              sent = true
              tasks.enqueue(() => receive(EndSent(), Tasks()))
              outgoing.send(frame, tasks)
            } else {
              execute(tasks)
            }
          case _ =>
            outgoing.send(frame, tasks)
        }
      case e:EndSession =>
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
      case _ =>
        outgoing.send(frame, tasks)
    }
  }

  protected def _receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    try {
      frame match {
        case t:AMQPTransportFrame =>
          t.getPerformative match {
            case e:End =>
              if (!received) {
                received = true
                incoming.receive(EndReceived(), tasks)
              } else {
                execute(tasks)
              }
            case _ =>
              incoming.receive(frame, tasks)
          }
        case _ =>
          incoming.receive(frame, tasks)
      }
    } catch {
      case t:Throwable =>
        warn("Exception processing frame : %s, error is %s", frame, t)
        warn("Exception stack trace : \n%s", t.getStackTraceString)
        send(EndSession(t), tasks)
    }
  }
}