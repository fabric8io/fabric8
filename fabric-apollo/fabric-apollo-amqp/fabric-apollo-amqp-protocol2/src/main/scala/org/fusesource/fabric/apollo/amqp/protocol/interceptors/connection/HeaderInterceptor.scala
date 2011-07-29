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
import org.fusesource.fabric.apollo.amqp.protocol.commands.{CloseConnection, HeaderSent, ConnectionCreated}
import java.lang.IllegalStateException
import org.fusesource.fabric.apollo.amqp.codec.types.{Open, AMQPTransportFrame, AMQPProtocolHeader}
import org.apache.activemq.apollo.util.Logging
import Interceptor._
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{Tasks, Execute}

/**
 *
 */
class HeaderInterceptor extends Interceptor with Logging {

  val error = () => {
    send(CloseConnection(), Tasks())
  }

  val sent = new AtomicBoolean(false)

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]):Unit = {
    frame match {
      case h:AMQPProtocolHeader =>
        if (!sent.getAndSet(true)) {
          if (!tasks.contains(error)) {
            tasks.enqueue(() => {
                receive(HeaderSent(), Tasks())
            })
            tasks.enqueue(rm)
          }
          outgoing.send(frame, tasks)
        } else {
          Execute(tasks)
        }
      case c:CloseConnection =>
        outgoing.send(frame, tasks)
      case _ =>
        if (!sent.get) {
          info("AMQP header frame has not yet been sent, dropping frame : %s", frame)
          Execute(tasks)
          throw new IllegalStateException("Header frame has not yet been sent, cannot send frame : " + frame)
        } else {
          outgoing.send(frame, tasks)
        }
    }
  }

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]):Unit = {
    frame match {
      case s:ConnectionCreated =>
        send(new AMQPProtocolHeader, tasks)
      case h:AMQPProtocolHeader =>
        if ( h.major != MAJOR && h.minor != MINOR && h.revision != REVISION ) {
          tasks.enqueue(error)
        }
        send(new AMQPProtocolHeader, tasks)
      case _ =>
        incoming.receive(frame, tasks)
    }
  }
}