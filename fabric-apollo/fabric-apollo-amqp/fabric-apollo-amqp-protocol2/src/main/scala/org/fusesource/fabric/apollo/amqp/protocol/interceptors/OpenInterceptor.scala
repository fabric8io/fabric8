package org.fusesource.fabric.apollo.amqp.protocol.interceptors

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import java.util.concurrent.atomic.AtomicBoolean
import org.fusesource.fabric.apollo.amqp.codec.types.{Open, AMQPTransportFrame}
import org.fusesource.fabric.apollo.amqp.codec.AMQPDefinitions
import org.fusesource.fabric.apollo.amqp.protocol.commands.{OpenSent, HeaderSent}

object OpenInterceptor {
  // TODO - probably gonna be a few possibilities here...
  val error = () => {
    throw new RuntimeException("")
  }
}

/**
 *
 */
class OpenInterceptor extends Interceptor {
  import OpenInterceptor._

  val sent = new AtomicBoolean(false)
  val open = new Open
  var peer:Open = new Open
  peer.setMaxFrameSize(AMQPDefinitions.MIN_MAX_FRAME_SIZE.asInstanceOf[Int])
  peer.setChannelMax(0)

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case f:AMQPTransportFrame =>
        if (f.getPerformative == null) {
          outgoing.send(frame, tasks)
        }
        f.getPerformative match {
          case o:Open =>
            if (!sent.getAndSet(true)) {
              if (!tasks.contains(error)) {
                tasks.enqueue( () => {
                  receive(new OpenSent, new Queue[() => Unit])
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
      case _ =>
        outgoing.send(frame, tasks)
    }
  }

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case h:HeaderSent =>
        send(new AMQPTransportFrame(open), tasks)
      case f:AMQPTransportFrame =>
        f.getPerformative match {
          case o:Open =>
            peer = o
            send(new AMQPTransportFrame(open), tasks)
          case _ =>
            incoming.receive(frame, tasks)
        }
      case _ =>
        incoming.receive(frame, tasks)
    }
  }

}