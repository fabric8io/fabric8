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

package org.fusesource.fabric.apollo.amqp.broker

import org.apache.activemq.apollo.broker._
import org.apache.activemq.apollo.broker.protocol.Protocol
import org.apache.activemq.apollo.broker.protocol.ProtocolFactory
import org.apache.activemq.apollo.transport._
import org.apache.activemq.apollo.broker.store._
import org.fusesource.hawtbuf._

import org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.AmqpMarshaller
import org.fusesource.fabric.apollo.amqp.codec.types.{AmqpString, AmqpTransfer}
import java.io.{DataOutputStream, DataInputStream}
import org.fusesource.fabric.apollo.amqp.protocol.AmqpProtoMessage
import org.fusesource.fabric.apollo.amqp.protocol.AmqpConstants._
import org.fusesource.fabric.apollo.amqp.protocol.AmqpProtocolCodecFactory

/*
 *
 */
object AmqpProtocolFactory extends ProtocolFactory.Provider {

  def create() = AmqpProtocol

  def create(config: String) = if (config == PROTOCOL) {
    AmqpProtocol
  } else {
    null
  }

}

/*
 *
 */
// TODO - de-couple message from transfer
object AmqpProtocol extends AmqpProtocolCodecFactory with Protocol {

  def createProtocolHandler = new AmqpProtocolHandler

  def decode(message: MessageRecord) = AmqpMessageTransfer.fromBuffer(message.buffer)

  def encode(message: Message) = message.asInstanceOf[AmqpMessageTransfer].toMessageRecord
}
