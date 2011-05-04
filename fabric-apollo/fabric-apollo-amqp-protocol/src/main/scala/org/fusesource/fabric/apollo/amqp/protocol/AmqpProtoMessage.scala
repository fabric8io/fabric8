/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.protocol

import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.fabric.apollo.amqp.codec.CodecUtils._
import org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory._
import org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.AmqpMarshaller
import collection.mutable.ListBuffer
import java.util.{UUID, LinkedList}
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.api.{Outcome, Message}
import org.fusesource.hawtbuf.{ByteArrayOutputStream, ByteArrayInputStream, Buffer}
import java.io._
import org.fusesource.fabric.apollo.amqp.codec.{CodecUtils, AmqpMessage}

/**
 *
 */
object AmqpProtoMessage {

  def create: AmqpProtoMessage = create(UUID.randomUUID.toString)

  def create(tag:String):AmqpProtoMessage = create(createAmqpDeliveryTag(Buffer.ascii(tag).buffer))

  def create(tag:AmqpDeliveryTag):AmqpProtoMessage = {
    val rc = new AmqpProtoMessage
    rc.tag = tag
    rc
  }

  def create(transfer:AmqpTransfer) = new AmqpProtoMessage(transfer)

  def payload(fragment:AmqpFragment) : AnyRef = {
    fragment.getSectionCode.intValue match {
      case AmqpMessage.DATA =>
        fragment.getPayload
      case AmqpMessage.AMQP_DATA =>
        CodecUtils.unmarshal(fragment.getPayload.getData)
      case AmqpMessage.AMQP_LIST =>
        AmqpList.AmqpListBuffer.create(fragment.getPayload, 0, AmqpMarshaller.getMarshaller)
      case AmqpMessage.AMQP_MAP =>
        AmqpMap.AmqpMapBuffer.create(fragment.getPayload, 0, AmqpMarshaller.getMarshaller)
      case _ =>
        throw new RuntimeException("Unknown fragment format code")
    }
  }

  def fromBuffer(buffer:Buffer) = {
    val in = new DataInputStream(new ByteArrayInputStream(buffer))
    unmarshal(in)
  }

  def unmarshal(in:DataInput) = {
    val rc = new AmqpProtoMessage
    rc.settled = AmqpBoolean.AmqpBooleanBuffer.create(in, AmqpMarshaller.getMarshaller).getValue.booleanValue
    rc.batchable = AmqpBoolean.AmqpBooleanBuffer.create(in, AmqpMarshaller.getMarshaller).getValue.booleanValue
    rc.tag = AmqpDeliveryTag.AmqpDeliveryTagBuffer.create(in, AmqpMarshaller.getMarshaller)
    rc._message = new AmqpMessage(AmqpList.AmqpListBuffer.create(in, AmqpMarshaller.getMarshaller).asInstanceOf[IAmqpList[AmqpFragment]])
    rc
  }

}

// TODO - Fragmented messages

// TODO - Revise exactly where delivery tags are tracked, they're more of a transfer thing than a message thing

class AmqpProtoMessage extends Message with Logging {
  import AmqpProtoMessage._

  def this(transfer: AmqpTransfer) {
    this ()
    deconstructTransfer(transfer)
  }

  protected var _message:AmqpMessage = new AmqpMessage

  var tag: AmqpDeliveryTag = null

  // TODO - this will become a list when fragmented messages are supported
  var _transfer_id:Option[Long] = None

  var settled: Boolean = false
  var batchable:Boolean = false

  private var _onSend:Option[Runnable] = None
  private var _onPut:Option[Runnable] = None
  private var _onAck:Option[Runnable] = None
  var error:AmqpError = null
  private var _outcome:Outcome = null

  def outcome = _outcome
  def outcome_=(o:Outcome) = {
    o match {
      case Outcome.REJECTED =>
        Option(header.getDeliveryFailures) match {
          case Some(fails) =>
            header.setDeliveryFailures(fails.longValue + 1)
          case None =>
            header.setDeliveryFailures(1)
        }
      case _ =>
    }
    _outcome = o
  }

  def getOutcome = outcome
  def getError = error

  def onAck = _onAck
  def onPut = _onPut
  def onSend = _onSend

  def onAck(task:Runnable) = _onAck = Option(task)
  def onPut(task:Runnable) = _onPut = Option(task)
  def onSend(task:Runnable) = _onSend = Option(task)

  private def deconstructTransfer(transfer:AmqpTransfer): Unit = {
    tag = transfer.getDeliveryTag
    _transfer_id = Option(transfer.getTransferId.getValue.longValue)

    val fragments = transfer.getFragments.getValue.asInstanceOf[IAmqpList[AmqpFragment]]
    _message = new AmqpMessage(fragments)

    import AmqpConversions._
    settled = Option[Boolean](transfer.getSettled).getOrElse(false)
    batchable = Option[Boolean](transfer.getBatchable).getOrElse(false)
  }

  def transfer(transfer_id:Long): AmqpTransfer = {
    _transfer_id = Option(transfer_id)
    val rc: AmqpTransfer = createAmqpTransfer
    rc.setDeliveryTag(tag)
    rc.setTransferId(transfer_id)
    val fragments: Multiple = createMultiple
    fragments.setValue(_message.construct)
    rc.setFragments(fragments)
    rc.setSettled(settled)
    rc.setBatchable(batchable)
    return rc
  }

  override def toString: String = {
    "AMQP Protocol Message{delivery tag=" + tag + " settled=" + settled + " batchable=" + batchable + " message=" + _message + "}"
  }

  def copy = AmqpProtoMessage.fromBuffer(this.toBuffer)

  def transfer_id = _transfer_id.get
  def transfer_id_=(id:Long) = _transfer_id = Option(id)

  def getDeliveryTag = tag
  def setDeliveryTag(tag:AmqpDeliveryTag) = this.tag = tag
  def getHeader = header
  def getProperties = properties
  def getFooter = footer
  def getBatchable:Boolean = batchable
  def setBatchable(batchable:Boolean) = this.batchable = batchable
  def getSettled:Boolean = settled
  def setSettled(settled:Boolean): Unit = this.settled = settled

  def header:AmqpHeader = _message.getHeader
  def properties: AmqpProperties = _message.getProperties
  def footer: AmqpFooter = _message.getFooter

  def getBodyPart(index: Int) = _message.getPayload(index)

  def count = _message.getCount()

  def addBodyPart(list: AmqpList) = _message.add(list)

  def addBodyPart(map: AmqpMap) = _message.add(map)

  def addBodyPart[T <: AmqpType[_, _]](data: T) = _message.add(data)

  def addBodyPart(data: Buffer) = _message.add(data)

  def addBodyPart(data: Array[Byte]) = _message.add(data)

  def toBuffer = {
    val baos = new ByteArrayOutputStream()
    val os = new DataOutputStream(baos)
    marshal(os)
    baos.toBuffer
  }

  def marshal(os:DataOutput) = {
    createAmqpBoolean(settled).marshal(os, AmqpMarshaller.getMarshaller)
    createAmqpBoolean(batchable).marshal(os, AmqpMarshaller.getMarshaller)
    tag.marshal(os, AmqpMarshaller.getMarshaller)
    _message.construct.marshal(os, AmqpMarshaller.getMarshaller)
  }

}
