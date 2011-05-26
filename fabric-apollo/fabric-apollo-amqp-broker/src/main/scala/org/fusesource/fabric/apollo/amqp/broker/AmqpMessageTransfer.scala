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

import java.lang.{String, Class}
import org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory._
import org.apache.activemq.apollo.broker.{DestinationParser, Message}
import org.fusesource.fabric.apollo.amqp.codec.CodecUtils.getSize
import org.apache.activemq.apollo.broker.store.MessageRecord
import org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.AmqpMarshaller
import org.fusesource.fabric.apollo.amqp.protocol.AmqpConstants._
import org.fusesource.hawtbuf.{ByteArrayOutputStream, ByteArrayInputStream, Buffer, AsciiBuffer}
import java.io.{DataOutputStream, DataInputStream}
import org.fusesource.fabric.apollo.amqp.protocol.{AmqpConversions, AmqpProtoMessage}
import AmqpConversions._
import java.math.BigInteger
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.types._

object AmqpMessageTransfer {
  val parser = new DestinationParser()

  def fromBuffer(buffer:Buffer) = {
    val is = new DataInputStream(new ByteArrayInputStream(buffer))
    val name = AmqpString.AmqpStringBuffer.create(is, AmqpMarshaller.getMarshaller)
    val address = AmqpString.AmqpStringBuffer.create(is, AmqpMarshaller.getMarshaller)
    val message = AmqpProtoMessage.unmarshal(is)
    new AmqpMessageTransfer(message, name, address)
  }
}

class AmqpMessageTransfer(val message:AmqpProtoMessage, val name:AmqpString, val address:AmqpString) extends Message with Logging {

  import AmqpMessageTransfer._

  lazy val dest = parser.decode_destination(address.getValue)

  def header = message.header

  def size = marshal.length

  def getLocalConnectionId = name

  def getProperty(name: String): Object = {
    var rc:Object = null

    if (name.equalsIgnoreCase("message-id")) {
      rc = message.getProperties.getMessageId
    }
    if (name.equalsIgnoreCase("to")) {
      rc = message.getProperties.getTo.asInstanceOf[AmqpPrimitive[_]].getValue.asInstanceOf[Object]
    }
    if (name.equalsIgnoreCase("subject")) {
      rc = message.getProperties.getSubject
    }
    if (name.equalsIgnoreCase("content-length")) {
      rc = message.getProperties.getContentLength
    }
    if (name.equalsIgnoreCase("content-type")) {
      rc = message.getProperties.getContentType
    }

    def get_from_map(name:String, map:AmqpMessageAttributes) = {
      Option(map) match {
        case Some(map) =>
          Option[AmqpPrimitive[_]](map.get(createAmqpSymbol(name)).asInstanceOf[AmqpPrimitive[_]]) match {
            case Some(rc) =>
              rc.getValue.asInstanceOf[Object]
            case None =>
              null
          }
        case None =>
          null
      }
    }

    if (rc == null) {
      rc = get_from_map(name, message.getHeader.getMessageAttrs)
    }
    if (rc == null) {
      rc = get_from_map(name, message.getHeader.getDeliveryAttrs)
    }
    if (rc == null) {
      rc = get_from_map(name, message.getFooter.getMessageAttrs)
    }
    if (rc == null) {
      rc = get_from_map(name, message.getFooter.getDeliveryAttrs)
    }

    rc
  }

  def getBodyAs[T](toType : Class[T]) = {
    trace("getBodyAs called for type %s", toType)
    // TODO
    null.asInstanceOf[T]
  }

  def protocol = AmqpProtocol

  def destination = dest

  def persistent = Option(message.header.getDurable) match {
    case Some(b) =>
      b.booleanValue
    case None =>
      false
  }

  def expiration = Option[BigInteger](message.header.getTtl).getOrElse(BigInteger.ZERO).longValue
  def priority = Option[Short](message.header.getPriority).getOrElse(0).asInstanceOf[Byte]

  lazy val _producer = new AsciiBuffer(name.getValue)
  def producer = _producer

  lazy val _id = message.tag.getValue().ascii();

  def id = _id

  def retained = throw new UnsupportedOperationException
  def retain = {}
  def release = {}

  def marshal = {
    val baos = new ByteArrayOutputStream()
    val os = new DataOutputStream(baos)
    name.marshal(os, AmqpMarshaller.getMarshaller)
    address.marshal(os, AmqpMarshaller.getMarshaller)
    message.marshal(os)
    baos.toBuffer
  }

  def toMessageRecord = {
    val rc = new MessageRecord
    rc.protocol = new AsciiBuffer(PROTOCOL)
    rc.buffer = marshal
    rc.size = rc.buffer.length
    rc
  }

}
