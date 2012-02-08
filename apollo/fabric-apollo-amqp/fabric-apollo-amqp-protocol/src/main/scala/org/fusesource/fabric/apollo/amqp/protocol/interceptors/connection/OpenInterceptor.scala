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

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.connection

import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.types.{Open, AMQPTransportFrame}
import org.fusesource.fabric.apollo.amqp.codec.AMQPDefinitions
import java.util.UUID
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{Tasks, execute}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{OpenReceived, OpenSent, HeaderSent}
import org.apache.activemq.apollo.util.Logging
import org.fusesource.hawtbuf.Buffer
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.{FrameInterceptor, PerformativeInterceptor}
import org.fusesource.hawtdispatch._
/**
 *
 */
class OpenInterceptor extends PerformativeInterceptor[Open] with Logging {

  var sent = false
  var received = false
  val open = new Open
  var peer:Open = new Open
  peer.setMaxFrameSize(AMQPDefinitions.MIN_MAX_FRAME_SIZE.asInstanceOf[Int])
  peer.setChannelMax(0)

  val sender = new FrameInterceptor[HeaderSent] {
      override protected def receive_frame(h:HeaderSent, tasks: Queue[() => Unit]) = {
        queue {
          send_open
        }
        tasks.enqueue(() => remove)
        incoming.receive(h, tasks)
      }
    }

  override protected def adding_to_chain = {
    before(sender)
  }

  override protected def removing_from_chain = {
    if (sender.connected) {
      sender.remove
    }
  }

  override protected def send(o:Open, payload:Buffer, tasks: Queue[() => Unit]) = {
    if (!sent) {
      sent = true
      tasks.enqueue( () => {
        receive(OpenSent(), Tasks())
      })
      false
    } else {
      execute(tasks)
      true
    }
  }

  override protected def receive(o:Open, payload:Buffer, tasks: Queue[() => Unit]) = {
    if (!received) {
      received = true
      peer = o
      receive(OpenReceived(), tasks)
    } else {
      execute(tasks)
    }
    true
  }

  def send_open = {
    Option(open.getContainerID) match {
      case Some(id) =>
      case None =>
        open.setContainerID(UUID.randomUUID.toString)
    }
    send(new AMQPTransportFrame(open), Tasks())
  }

}