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

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.connection

import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.AMQPDefinitions._
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.FrameInterceptor
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{Tasks, execute}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{HeaderReceived, CloseConnection, HeaderSent, ConnectionCreated}
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPProtocolHeader
import org.fusesource.hawtdispatch._

/**
 *
 */
class HeaderInterceptor extends FrameInterceptor[AMQPProtocolHeader] with Logging {
  val error = () => {
    send(CloseConnection(), Tasks())
  }

  var sent = false
  var received = false

  val sender = new FrameInterceptor[ConnectionCreated] {
      override protected def receive_frame(c:ConnectionCreated, tasks:Queue[() => Unit]) = {
        queue {
          send_header(false)
        }
        tasks.enqueue(() => remove)
        incoming.receive(c, tasks)
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
        send_header(true)
        sent = false
      } else {
        received = true
        queue {
          send_header(false)
        }
        incoming.receive(HeaderReceived(), tasks)
      }
    }
  }

  def send_header(error:Boolean) = {
    val tasks = Tasks()
    if (error) {
      tasks.enqueue(this.error)
    }
    send(new AMQPProtocolHeader(), tasks)
  }

}