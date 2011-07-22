package org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.apache.activemq.apollo.util.Logging

/**
 *
 */

class TaskExecutingInterceptor extends Interceptor  {
  def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    printf("Tasks : %s\n", tasks)
    tasks.dequeueAll((x) => {
      x()
      true
    })
  }

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    incoming.receive(frame, tasks)
  }
}