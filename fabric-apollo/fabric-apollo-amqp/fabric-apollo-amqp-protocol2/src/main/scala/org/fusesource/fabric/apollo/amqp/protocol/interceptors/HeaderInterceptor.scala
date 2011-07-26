/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import java.util.concurrent.atomic.AtomicBoolean
import org.fusesource.fabric.apollo.amqp.codec.AMQPDefinitions._
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPProtocolHeader
import org.fusesource.fabric.apollo.amqp.protocol.commands.{CloseConnection, HeaderSent, ConnectionCreated}

/**
 *
 */
class HeaderInterceptor extends Interceptor {

  val error = () => {
    send(CloseConnection.apply, new Queue[() => Unit])
  }

  val sent = new AtomicBoolean(false)

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]):Unit = {
    frame match {
      case h:AMQPProtocolHeader =>
        if (!sent.getAndSet(true)) {
          if (!tasks.contains(error)) {
            tasks.enqueue(() => {
                receive(HeaderSent.apply, new Queue[() => Unit])
            })
            tasks.enqueue(rm)
          }
          outgoing.send(frame, tasks)
        } else {
          tasks.dequeueAll((x) => {
            x()
            true
          })
        }
      case _ =>
        outgoing.send(frame, tasks)
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

  override def toString = String.format("AMQP Header Interceptor outgoing=%s incoming=%s", outgoing, incoming)
}