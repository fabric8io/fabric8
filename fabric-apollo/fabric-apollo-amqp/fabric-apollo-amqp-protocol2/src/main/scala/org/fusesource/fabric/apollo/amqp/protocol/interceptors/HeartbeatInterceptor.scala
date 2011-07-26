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

import org.fusesource.hawtbuf.Buffer._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.AMQPConnection
import org.apache.activemq.apollo.broker.protocol.HeartBeatMonitor
import org.apache.activemq.apollo.transport.Transport
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.fabric.apollo.amqp.protocol.commands.ConnectionClosed
import org.apache.activemq.apollo.util.Logging

/**
 *
 */

class HeartbeatInterceptor extends Interceptor with Logging {

  var transport:Transport = null

  var idle_timeout = AMQPConnection.DEFAULT_HEARTBEAT

  val heartbeat_monitor = new HeartBeatMonitor

  val on_keep_alive = () => {
    send(new AMQPTransportFrame, new Queue[() => Unit])
  }

  def heartbeat_interval(t:Long) = (t - (t * 0.05)).asInstanceOf[Long]

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]):Unit = outgoing.send(frame, tasks)

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]):Unit = {
    frame match {
      case c:ConnectionClosed =>
        heartbeat_monitor.stop
      case f:AMQPTransportFrame =>
        val performative:Object = f.getPerformative
        performative match {
          case n:NoPerformative =>
            info("Dropping heartbeat frame : %s", frame)
            return
          case o:Open =>
            Option(o.getIdleTimeout) match {
              case Some(t) =>
                tasks.enqueue( () => {
                  val write_interval = heartbeat_interval(t)
                  trace("Setting up heartbeat, read_interval:%s write_interval: %s", idle_timeout, write_interval)
                  heartbeat_monitor.read_interval = idle_timeout
                  heartbeat_monitor.write_interval = write_interval
                  heartbeat_monitor.transport = transport
                  heartbeat_monitor.on_dead = () => {
                    val close = new Close(new Error(ascii("Idle timeout expired")))
                    send(new AMQPTransportFrame(close), new Queue[() => Unit])
                  }
                  heartbeat_monitor.on_keep_alive = on_keep_alive
                  heartbeat_monitor.start
                })
              case None =>
                tasks.enqueue(rm)
            }
          case _ =>
        }
      case _ =>
    }
    incoming.receive(frame, tasks)
  }

}