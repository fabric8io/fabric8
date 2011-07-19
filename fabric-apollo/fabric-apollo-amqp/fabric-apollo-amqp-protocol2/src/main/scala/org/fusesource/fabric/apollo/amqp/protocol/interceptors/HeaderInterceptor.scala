package org.fusesource.fabric.apollo.amqp.protocol.interceptors

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import java.util.concurrent.atomic.AtomicBoolean
import org.fusesource.fabric.apollo.amqp.codec.AMQPDefinitions._
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPProtocolHeader

object HeaderInterceptor {
  val error = () => {
    throw new RuntimeException("Unexpected protocol version received, supported : " + new AMQPProtocolHeader)
  }
}
/**
 *
 */
class HeaderInterceptor extends Interceptor {
  import HeaderInterceptor._

  val sent = new AtomicBoolean(false)

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case h:AMQPProtocolHeader =>
        if (!sent.getAndSet(true)) {
          if (!tasks.contains(error)) {
            tasks.enqueue(rm)
          }
          outgoing.send(frame, tasks)
        }
      case _ =>
        outgoing.send(frame, tasks)
    }
  }

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
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