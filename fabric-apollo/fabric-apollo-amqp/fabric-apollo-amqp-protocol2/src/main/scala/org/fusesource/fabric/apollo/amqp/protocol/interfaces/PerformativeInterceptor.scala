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

import org.fusesource.fabric.apollo.amqp.codec.types.AMQPTransportFrame
import collection.mutable.Queue
import org.fusesource.hawtbuf.Buffer
import org.fusesource.fabric.apollo.amqp.codec.interfaces.{AMQPFrame, Frame}

/**
 *
 */

class PerformativeInterceptor[K <: Frame : ClassManifest] extends FrameInterceptor[AMQPTransportFrame] {

  implicit val performative_cm = classManifest[K]

  private def performative_matches(performative:Frame) = performative_cm.erasure.isInstance(performative)

  final override protected def send_frame(frame: AMQPTransportFrame, tasks: Queue[() => Unit]) = {
    if (performative_matches(frame.getPerformative)) {
      if (!send(frame.getPerformative.asInstanceOf[K], frame.getPayload, tasks)) {
        outgoing.send(frame, tasks)
      }
    } else {
      outgoing.send(frame, tasks)
    }
  }

  final override protected def receive_frame(frame: AMQPTransportFrame, tasks: Queue[() => Unit]) = {
    if (performative_matches(frame.getPerformative)) {
      if (!receive(frame.getPerformative.asInstanceOf[K], frame.getPayload, tasks)) {
        incoming.receive(frame, tasks)
      }
    } else {
      incoming.receive(frame, tasks)
    }

  }

  protected def send(performative:K, payload:Buffer, tasks:Queue[() => Unit]):Boolean = false

  protected def receive(performative:K, payload:Buffer, tasks:Queue[() => Unit]):Boolean = false
}