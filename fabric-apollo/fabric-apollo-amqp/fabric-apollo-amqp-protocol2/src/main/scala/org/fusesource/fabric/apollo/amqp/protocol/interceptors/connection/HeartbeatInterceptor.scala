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

import org.fusesource.hawtbuf.Buffer._
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.AMQPConnection
import org.apache.activemq.apollo.broker.protocol.HeartBeatMonitor
import org.apache.activemq.apollo.transport.Transport
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.apache.activemq.apollo.util.Logging
import org.fusesource.hawtbuf.Buffer
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{execute, Tasks}
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.{FrameInterceptor, PerformativeInterceptor}
import org.fusesource.fabric.apollo.amqp.protocol.commands.ConnectionCreated

/**
 *
 */
class HeartbeatInterceptor extends PerformativeInterceptor[Open] with Logging {

  var transport:Transport = null
  var local_idle_timeout:Option[Long] = None
  var remote_idle_timeout:Option[Long] = None
  var heartbeat_monitor = new HeartBeatMonitor
  var started = false

  var sent = false
  var received = false

  val on_keep_alive = () => {
    send(new AMQPTransportFrame, Tasks())
  }

  override protected def adding_to_chain = {
    before(new FrameInterceptor[ConnectionCreated] {
      override protected def receive_frame(c:ConnectionCreated, tasks:Queue[() => Unit]) = {
        transport = c.transport
        tasks.enqueue( () => remove)
        incoming.receive(c, tasks)
      }
    })

    before(new PerformativeInterceptor[NoPerformative] {
      override protected def receive(n:NoPerformative, payload:Buffer, tasks:Queue[() => Unit]) = {
        execute(tasks)
        true
      }
    })
  }

  def heartbeat_interval(t:Long) = (t - (t * 0.05)).asInstanceOf[Long]

  override protected def send(o:Open, payload:Buffer, tasks: Queue[() => Unit]) = {
    local_idle_timeout.foreach((x) => o.setIdleTimeout(x))
    if (!sent) {
      sent = true
    }
    maybe_start
    false
  }

  override protected def receive(o:Open, payload:Buffer, tasks: Queue[() => Unit]) = {
    Option(o.getIdleTimeout) match {
      case Some(x) =>
        remote_idle_timeout = Option(x.longValue)
      case None =>
    }
    if (!received) {
      received = true
    }
    maybe_start
    false
  }

  def maybe_start = {
    if (sent && received && !started) {
      start_monitor
    }
  }

  def start_monitor = {
    started = true
    val read_interval = local_idle_timeout.getOrElse(AMQPConnection.DEFAULT_HEARTBEAT)
    val write_interval = heartbeat_interval(remote_idle_timeout.getOrElse(AMQPConnection.DEFAULT_HEARTBEAT))
    trace("Setting up heartbeat, read_interval:%s write_interval: %s", read_interval, write_interval)
    heartbeat_monitor.read_interval = read_interval
    heartbeat_monitor.write_interval = write_interval
    heartbeat_monitor.transport = transport
    heartbeat_monitor.on_dead = () => {
      val close = new Close(new Error(ascii("Idle timeout expired")))
      send(new AMQPTransportFrame(close), Tasks())
    }
    heartbeat_monitor.on_keep_alive = on_keep_alive
    heartbeat_monitor.start
  }

}