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
import org.fusesource.fabric.apollo.amqp.codec.types._
import java.util.concurrent.atomic.AtomicBoolean
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.protocol.commands.{ConnectionClosed, CloseConnection}

/**
 *
 */
class CloseInterceptor extends Interceptor with Logging {

  val sent = new AtomicBoolean(false)

  val close = () => {
    outgoing.send(CloseConnection.apply, new Queue[() => Unit])
    sent.set(true)
  }

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]):Unit = {
    if (sent.get) {
      tasks.dequeueAll((x) => {x(); true})
      trace("Connection is closed, dropping outgoing frame %s", frame)
    } else {
      frame match {
        case f:AMQPTransportFrame =>
          f.getPerformative match {
            case c:Close =>
              tasks.enqueue(close)
            case _ =>
          }
          outgoing.send(frame, tasks)
        case c:CloseConnection =>
          val close = new Close
          c.reason match {
            case Some(reason) =>
              close.setError(new Error(AMQPError.INTERNAL_ERROR.getValue, reason))
            case None =>
          }
          c.exception match {
            case Some(exception) =>
              close.setError(new Error(AMQPError.INTERNAL_ERROR.getValue, exception.toString))
            case None =>
          }
          send(new AMQPTransportFrame(close), tasks)
        case _ =>
          outgoing.send(frame, tasks)
      }
    }
  }

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]):Unit = {
    if (sent.get) {
      frame match {
        case c:ConnectionClosed =>
          incoming.receive(frame, tasks)
        case _ =>
          trace("Connection is closed, dropping incoming frame %s", frame)
          tasks.dequeueAll((x) => {x(); true})
      }
    } else {
      try {
        frame match {
          case t:AMQPTransportFrame =>
            t.getPerformative match {
              case c:Close =>
                send(CloseConnection.apply, tasks)
              case _ =>
                incoming.receive(frame, tasks)
            }
          case _ =>
            incoming.receive(frame, tasks)
        }
      } catch {
        case t:Throwable =>
          send(CloseConnection(t), tasks)
      }
    }
  }
}