/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.link


import org.apache.activemq.apollo.util.Logging
import org.fusesource.hawtbuf.Buffer
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.AMQPLink
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{Tasks, execute}
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.{FrameInterceptor, PerformativeInterceptor}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{AttachReceived, ChainAttached, AttachSent}

/**
 *
 */
class AttachInterceptor extends PerformativeInterceptor[Attach] with Logging {

  var sent = false
  var received = false
  var configure_attach:Option[(Attach) => Attach] = None

  def send_attach:Unit = {
    configure_attach match {
      case Some(init) =>
        send(new AMQPTransportFrame(init(new Attach)), Tasks())
      case None =>
        throw new RuntimeException("Link not configured")
    }
  }

  override protected def send(performative: Attach, payload: Buffer, tasks: Queue[() => Unit]) = {
    if (!sent) {
      sent = true
      tasks.enqueue( () => {
        receive(AttachSent(), Tasks())
      })
      false
    } else {
      execute(tasks)
      true
    }

  }

  override protected def receive(performative: Attach, payload: Buffer, tasks: Queue[() => Unit]) = {
    if (!received) {
      received = true
      receive(AttachReceived(), Tasks())
      execute(tasks)
      true
    } else {
      val detach = new Detach(0, true, new Error(AMQPError.ILLEGAL_STATE.getValue, "Double attach"))
      send(new AMQPTransportFrame(detach), tasks)
      true
    }
  }
}