/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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