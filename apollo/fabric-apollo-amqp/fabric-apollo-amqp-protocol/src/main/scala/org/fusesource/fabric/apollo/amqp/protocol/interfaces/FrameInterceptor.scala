/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interfaces

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue

/**
 *
 */

class FrameInterceptor[T <: AMQPFrame : ClassManifest] extends Interceptor {

  implicit val cm = classManifest[T]

  private def matches(frame:AMQPFrame) = cm.erasure.isInstance(frame)

  final override protected def _send(frame:AMQPFrame, tasks:Queue[() => Unit]) = {
    if (matches(frame)) {
        send_frame(frame.asInstanceOf[T], tasks)
    } else {
        outgoing.send(frame, tasks)
    }
  }

  final override protected def _receive(frame:AMQPFrame, tasks:Queue[() => Unit]) = {
    if (matches(frame)) {
        receive_frame(frame.asInstanceOf[T], tasks)
    } else {
        incoming.receive(frame, tasks)
    }
  }

  protected def send_frame(frame:T, tasks:Queue[() => Unit]):Unit = outgoing.send(frame.asInstanceOf[AMQPFrame], tasks)

  protected def receive_frame(frame:T, tasks:Queue[() => Unit]):Unit = incoming.receive(frame.asInstanceOf[AMQPFrame], tasks)

}