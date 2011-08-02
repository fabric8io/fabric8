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

import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.AMQPDefinitions._
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.FrameInterceptor
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{Tasks, execute}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{HeaderReceived, CloseConnection, HeaderSent, ConnectionCreated}
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPProtocolHeader
import org.fusesource.hawtdispatch._
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame

/**
 *
 */
class HeaderInterceptor extends FrameInterceptor[AMQPProtocolHeader] with Logging {
  val error = () => {
    send(CloseConnection(), Tasks())
  }

  var sent = false
  var received = false

  override protected def send_frame(frame: AMQPProtocolHeader, tasks: Queue[() => Unit]) = {
    if (!sent) {
      if (!tasks.contains(error)) {
        sent = true
        tasks.enqueue(() => {
          receive(HeaderSent(), Tasks())
        })
      }
      outgoing.send(frame, tasks)
    } else {
      execute(tasks)
    }
  }

  override protected def receive_frame(frame: AMQPProtocolHeader, tasks: Queue[() => Unit]) = {
    if (!received) {
      if ( frame.major != MAJOR && frame.minor != MINOR && frame.revision != REVISION ) {
        send(new AMQPProtocolHeader(), Tasks(error))
      } else {
        received = true
        send(new AMQPProtocolHeader, Tasks())
        queue {
          incoming.receive(HeaderReceived(), tasks)
        }
      }
    }
  }

}