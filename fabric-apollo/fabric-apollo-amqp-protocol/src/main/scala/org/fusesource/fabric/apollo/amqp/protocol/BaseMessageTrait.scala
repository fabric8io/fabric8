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

import org.fusesource.hawtbuf.Buffer
import org.fusesource.fabric.apollo.amqp.api._
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory._
import java.util.LinkedList
import org.fusesource.hawtdispatch._
import collection.mutable.ListBuffer
import org.fusesource.fabric.apollo.amqp.codec.CodecUtils
import java.math.BigInteger
import org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.{EncodedBuffer, AmqpMarshaller}

object MessageSectionCodes {
  val HEADER = 0L
  val DELIVERY_ANNOTATIONS = 1L
  val MESSAGE_ANNOTATIONS = 2L
  val PROPERTIES = 3L
  val APPLICATION_PROPERTIES = 4L
  val DATA = 5L
  val AMQP_DATA = 6L
  val AMQP_MAP = 7L
  val AMQP_LIST = 8L
  val FOOTER = 9L
}

import MessageSectionCodes._
/**
 *
 */
trait BaseMessageTrait extends BaseMessage {

  var _header:Option[AmqpHeader] = None
  var _properties:Option[AmqpProperties] = None
  var _message_annotations:Option[AmqpFields] = None
  var _delivery_annotations:Option[AmqpFields] = None
  var _application_properties:Option[AmqpFields] = None
  var _footer:Option[AmqpFooter] = None

  val _onAckTasks:LinkedList[Runnable] = new LinkedList[Runnable]()
  val _onSendTasks:LinkedList[Runnable] = new LinkedList[Runnable]()

  def header:AmqpHeader = _header.getOrElse {
    _header = Option(createAmqpHeader)
    _header.get
  }

  def properties = _properties.getOrElse {
    _properties = Option(createAmqpProperties)
    _properties.get
  }

  def message_annotations = _message_annotations.getOrElse {
    _message_annotations = Option(createAmqpFields)
    _message_annotations.get
  }

  def delivery_annotations = _delivery_annotations.getOrElse {
    _delivery_annotations = Option(createAmqpFields)
    _delivery_annotations.get
  }

  def application_properties = _application_properties.getOrElse {
    _application_properties = Option(createAmqpFields)
    _application_properties.get
  }

  def footer = _footer.getOrElse {
    _footer = Option(createAmqpFooter)
    _footer.get
  }

  def getHeader = header
  def getProperties = properties
  def getMessageAnnotations = message_annotations
  def getDeliveryAnnotations = delivery_annotations
  def getApplicationProperties = application_properties
  def getFooter = footer

  def getOnSendTasks = _onAckTasks
  def getOnAckTasks = _onSendTasks

  def clearTasks(queue:DispatchQueue, tasks:LinkedList[Runnable]) = {
    while (!tasks.isEmpty) {
      queue << tasks.pop
    }
  }

  def executeOnAck(queue:DispatchQueue) = clearTasks(queue, _onAckTasks)
  def executeOnSend(queue:DispatchQueue) = clearTasks(queue, _onSendTasks)

  def marshal_single_fragment_section(section_body:AmqpType[_, _], section_code:Long) = {
    val rc = createAmqpFragment
    rc.setPayload(new Buffer(CodecUtils.marshal(section_body.asInstanceOf[AmqpType[_, AmqpBuffer[_]]])))
    rc.setSectionCode(section_code)
    rc.setFirst(true)
    rc.setLast(true)
    rc
  }

  // TODO - check that this is calculating this correctly...
  def figure_out_section_offset(fragment:AmqpFragment) = {
    var offset = new BigInt(BigInteger.ZERO)
    val size = fragment.getListCount
    // avoid taking any previously set section offset into account
    fragment.set(size - 2, null)
    var i = 0
    for ( i <- 1 to (size - 1) ) {
      val obj = fragment.get(i)
      if (obj != null) {
        offset = offset + BigInt(CodecUtils.marshal(obj.asInstanceOf[AmqpType[_, AmqpBuffer[_]]]).length)
      }
    }
    if (offset > 254) {
      offset = offset + 8
    } else {
      offset = offset + 1
    }
    fragment.setSectionOffset(offset.bigInteger)
  }

  def maybe_add(l:ListBuffer[AmqpFragment], o:Option[AmqpType[_, _]], section_code:Long) = o.foreach((x) => l.append(marshal_single_fragment_section(x, section_code)))

  def marshal_to_amqp_fragments = {
    val rc = ListBuffer[AmqpFragment]()
    // need to check section codes...
    maybe_add(rc, _header, HEADER)
    maybe_add(rc, _delivery_annotations, DELIVERY_ANNOTATIONS)
    maybe_add(rc, _message_annotations, MESSAGE_ANNOTATIONS)
    maybe_add(rc, _properties, PROPERTIES)
    maybe_add(rc, _application_properties, APPLICATION_PROPERTIES)
    if (has_body) {
      rc.appendAll(marshal_application_data_section)
      maybe_add(rc, _footer, FOOTER)
    }

    var i = 0
    rc.foreach( (x) => {
      x.setSectionNumber(i)
      figure_out_section_offset(x)
      i = i + 1
    })

    rc.toList
  }

  def marshal_application_data_section = {
    // TODO - break up based on max size
    val rc = createAmqpFragment
    rc.setPayload(marshal_body)
    rc.setSectionCode(get_section_code)
    rc.setFirst(true)
    rc.setLast(true)
    List(rc)
  }

  def get_section_code:Long
  def marshal_body:Buffer
  def has_body:Boolean
}

trait GenericMessageTrait[T] extends GenericMessage[T] {
  var body:T = null.asInstanceOf[T]
  def getBody = body
  def setBody(body:T) = this.body = body
}

class DataMessageImpl extends DataMessage with BaseMessageTrait with GenericMessageTrait[Buffer] {
  override def get_section_code = DATA
  override def marshal_body = body
  override def has_body = body != null
}

// Same as above but body section code is different
class AmqpDataMessageImpl extends DataMessageImpl with BaseMessageTrait with GenericMessageTrait[Buffer] {
  override def get_section_code = AMQP_DATA
}

class AmqpMapMessageImpl extends AmqpMapMessage with BaseMessageTrait with GenericMessageTrait[AmqpFields] {
  body = createAmqpFields
  override def get_section_code = AMQP_MAP
  override def marshal_body = new Buffer(CodecUtils.marshal(body))
  override def has_body = body != null
}

class AmqpListMessageImpl extends AmqpListMessage with BaseMessageTrait with GenericMessageTrait[AmqpList] {
  body = createAmqpList
  override def get_section_code = AMQP_LIST
  override def marshal_body = new Buffer(CodecUtils.marshal(body))
  override def has_body = body != null
}

