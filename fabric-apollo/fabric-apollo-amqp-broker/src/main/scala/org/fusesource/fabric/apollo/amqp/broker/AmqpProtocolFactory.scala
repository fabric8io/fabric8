/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
