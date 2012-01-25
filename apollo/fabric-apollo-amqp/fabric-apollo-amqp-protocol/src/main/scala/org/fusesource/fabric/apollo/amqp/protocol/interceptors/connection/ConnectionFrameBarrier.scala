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
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.fabric.apollo.amqp.protocol.utilities.execute
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.FrameInterceptor
import org.fusesource.fabric.apollo.amqp.protocol.commands.ConnectionCommand

/**
 * Prevents frames on channel 0 and connection command frames from proceeding further in the receive interceptor
 * chain
 */
class ConnectionFrameBarrier extends FrameInterceptor[AMQPTransportFrame] with Logging {

  val connection_command_dropper = new FrameInterceptor[ConnectionCommand] {
    override protected def receive_frame(c:ConnectionCommand, tasks: Queue[() => Unit]) = {
      execute(tasks)
    }
  }

  override protected def adding_to_chain = {
    before(connection_command_dropper)
  }

  override protected def removing_from_chain = {
    connection_command_dropper.remove
  }

  override protected def receive_frame(frame:AMQPTransportFrame, tasks: Queue[() => Unit]) = {
    if (frame.getChannel == 0) {
      execute(tasks)
    } else {
      incoming.receive(frame, tasks)
    }
  }
}