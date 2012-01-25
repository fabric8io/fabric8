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