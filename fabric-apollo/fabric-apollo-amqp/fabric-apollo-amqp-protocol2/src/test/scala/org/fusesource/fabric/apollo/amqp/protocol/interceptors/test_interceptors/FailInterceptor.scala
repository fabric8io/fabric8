package org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue

/**
 *
 */
class FailInterceptor extends Interceptor {
  def send(frame: AMQPFrame, tasks: Queue[() => Unit]):Unit = {
    throw new RuntimeException("FAIL on send")
  }

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]):Unit = {
    throw new RuntimeException("FAIL on receive")
  }
}

