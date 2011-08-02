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
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.types._

/**
 * Prevents frames on channel 0 from proceeding further in the receive interceptor
 * chain
 */
class ConnectionFrameBarrier extends Interceptor with Logging {

  override protected def _receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case p:AMQPProtocolHeader =>
        tasks.dequeueAll((x) => { x(); true })
      case t:AMQPTransportFrame =>
        if (t.getChannel == 0) {
          tasks.dequeueAll((x) => { x(); true })
          t.getPerformative match {
            case o:Open =>
            case c:Close =>
            case n:NoPerformative =>
            case _ =>
              throw new RuntimeException("Only open/close and heartbeat frames can be sent on channel 0")
          }
        } else {
          incoming.receive(frame, tasks)
        }
      case _ =>
        incoming.receive(frame, tasks)
    }
  }
}