package org.fusesource.fabric.apollo.amqp.protocol.interceptors

import org.fusesource.hawtbuf.Buffer._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.AMQPConnection
import org.apache.activemq.apollo.broker.protocol.HeartBeatMonitor
import org.apache.activemq.apollo.transport.Transport
import org.fusesource.fabric.apollo.amqp.codec.types.{AMQPTransportFrame, Close, Open, Error}

/**
 *
 */

class HeartbeatInterceptor extends Interceptor {

  var transport:Transport = null

  var idle_timeout = AMQPConnection.DEFAULT_HEARTBEAT

  val heartbeat_monitor = new HeartBeatMonitor

  val on_keep_alive = () => {
    send(new AMQPTransportFrame, new Queue[() => Unit])
  }

  def heartbeat_interval = (idle_timeout - (idle_timeout * 0.05)).asInstanceOf[Long]

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]):Unit = outgoing.send(frame, tasks)

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]):Unit = {
    frame match {
      case f:AMQPTransportFrame =>
        val performative:Object = f.getPerformative
        performative match {
          case o:Open =>
            Option(o.getIdleTimeout).foreach( t => {
              idle_timeout = idle_timeout.min(t)
              heartbeat_monitor.read_interval = idle_timeout
              heartbeat_monitor.write_interval = heartbeat_interval
              heartbeat_monitor.transport = transport
              heartbeat_monitor.on_dead = () => {
                val close = new Close(new Error(ascii("Idle timeout expired")))
                send(new AMQPTransportFrame(close), new Queue[() => Unit])
              }
              heartbeat_monitor.on_keep_alive = on_keep_alive
            })
          case _ =>
        }
      case _ =>
    }
    incoming.receive(frame, tasks)
  }

}