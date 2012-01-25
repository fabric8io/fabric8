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

import org.fusesource.fabric.apollo.amqp.codec.types.{AMQPTransportFrame, Transfer}
import collection.mutable.{HashMap, ListBuffer, Queue}
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.{FrameInterceptor, PerformativeInterceptor}
import org.fusesource.fabric.apollo.amqp.protocol.utilities.execute
import org.fusesource.hawtbuf.{DataByteArrayOutputStream, Buffer}
import java.io.DataOutput

/**
 *
 */

class MessageAssembler extends PerformativeInterceptor[Transfer] {

  val current_delivery = new ListBuffer[Buffer]
  var current_transfer:Transfer = null

  val transfer_aborter = new PerformativeInterceptor[Transfer] {
    override protected def receive(transfer:Transfer, payload:Buffer, tasks:Queue[() => Unit]) = {
      if (transfer.getAborted != null && transfer.getAborted == true) {
        current_delivery.clear
        current_transfer = null
        execute(tasks)
        true
      } else {
        false
      }
    }
  }

  val completed_transfer_receiver = new PerformativeInterceptor[Transfer] {
    override protected def receive(transfer:Transfer, payload:Buffer, tasks:Queue[() => Unit]):Boolean = {
      if (transfer.getMore == null || transfer.getMore == false) {
        if (current_delivery.isEmpty) {
          false
        } else {
          current_delivery.append(payload)
          var size = 0
          current_delivery.foreach((x) => size = size + x.length())
          val out = new DataByteArrayOutputStream(size)
          current_delivery.foreach((x) => x.writeTo(out.asInstanceOf[DataOutput]))
          current_transfer.setMore(null)
          current_delivery.clear
          val t = current_transfer
          current_transfer = null
          receive(new AMQPTransportFrame(t, out.toBuffer), tasks)
          true
        }
      } else {
        false
      }
    }
  }


  override protected def adding_to_chain = {
    before(transfer_aborter)
    before(completed_transfer_receiver)
  }

  override protected def removing_from_chain = {
    if (transfer_aborter.connected) {
      transfer_aborter.remove
    }
    if (completed_transfer_receiver.connected) {
      completed_transfer_receiver.remove
    }
  }

  override protected def receive(transfer:Transfer, payload:Buffer, tasks:Queue[() => Unit]):Boolean = {
    if (transfer.getMore != null && transfer.getMore == true) {
      if (current_delivery.isEmpty) {
        current_transfer = transfer
      }
      current_delivery.append(payload)
      execute(tasks)
      true
    } else {
      false
    }
  }
}