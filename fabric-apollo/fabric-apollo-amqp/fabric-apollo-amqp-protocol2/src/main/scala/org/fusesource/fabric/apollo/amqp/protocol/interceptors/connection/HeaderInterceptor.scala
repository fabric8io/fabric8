package org.fusesource.fabric.apollo.amqp.protocol.interceptors.connection

/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import java.util.concurrent.atomic.AtomicBoolean
import org.fusesource.fabric.apollo.amqp.codec.AMQPDefinitions._
import java.lang.IllegalStateException
import org.apache.activemq.apollo.util.Logging
import Interceptor._
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{Tasks, execute}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{HeaderReceived, CloseConnection, HeaderSent, ConnectionCreated}
import org.fusesource.fabric.apollo.amqp.codec.types.{AMQPProtocolHeader, Open, AMQPTransportFrame}
import org.fusesource.hawtdispatch._

/**
 *
 */
class HeaderInterceptor extends Interceptor with Logging {

  val error = () => {
    send(CloseConnection(), Tasks())
  }

  var sent = false
  var received = false

  override protected def _send(frame: AMQPFrame, tasks: Queue[() => Unit]):Unit = {
    frame match {
      case h:AMQPProtocolHeader =>
        if (!sent) {
          if (!tasks.contains(error)) {
            sent = true
            tasks.enqueue(() => {
                receive(HeaderSent(), Tasks())
            })
          }
          outgoing.send(frame, tasks)
        } else {
          execute(tasks)
        }
      case c:CloseConnection =>
        outgoing.send(frame, tasks)
      case _ =>
        if (!sent) {
          info("AMQP header frame has not yet been sent, dropping frame : %s", frame)
          execute(tasks)
          throw new IllegalStateException("Header frame has not yet been sent, cannot send frame : " + frame)
        } else {
          outgoing.send(frame, tasks)
        }
    }
  }

  override protected def _receive(frame: AMQPFrame, tasks: Queue[() => Unit]):Unit = {
    frame match {
      case s:ConnectionCreated =>
        send(new AMQPProtocolHeader, tasks)
      case h:AMQPProtocolHeader =>
        if (!received) {
          if ( h.major != MAJOR && h.minor != MINOR && h.revision != REVISION ) {
            send(new AMQPProtocolHeader(), Tasks(error))
          } else {
            received = true
            send(new AMQPProtocolHeader, Tasks())
            queue {
              incoming.receive(HeaderReceived(), tasks)
            }
          }
        }

      case _ =>
        incoming.receive(frame, tasks)
    }
  }
}