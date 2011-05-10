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

import org.fusesource.fabric.apollo.amqp.api._
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory._
import java.util.LinkedList
import org.fusesource.hawtdispatch._
import org.fusesource.fabric.apollo.amqp.codec.CodecUtils
import java.math.BigInteger
import collection.mutable.{HashMap, ListBuffer}
import org.fusesource.hawtbuf.{ByteArrayOutputStream, Buffer}
import java.io.{DataOutput, DataOutputStream}

// TODO - figure out how to relate transfer-level attributes (settled, batchable, disposition) to a message

object MessageSectionCodes {
  val HEADER = 0L
  val DELIVERY_ANNOTATIONS = 1L
  val MESSAGE_ANNOTATIONS = 2L
  val PROPERTIES = 3L
  val APPLICATION_PROPERTIES = 4L
  val DATA = 5L
  val AMQP_DATA = 6L
  val AMQP_MAP_DATA = 7L
  val AMQP_LIST_DATA = 8L
  val FOOTER = 9L
}

import MessageSectionCodes._
import AmqpMessageFactory._

/**
 * Base representation of an AMQP message with everything except the body, delegates body marshalling/unmarshalling to derived classes
 */
trait BaseMessageTrait extends BaseMessage {

  var _header:Option[AmqpHeader] = None
  var _delivery_annotations:Option[AmqpFields] = None
  var _message_annotations:Option[AmqpFields] = None
  var _properties:Option[AmqpProperties] = None
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

  def marshal_section(payload:Buffer, section_code:Long, max_size:Long = 0) = {
    if (max_size == 0) {
      val rc = createAmqpFragment
      rc.setPayload(payload)
      rc.setSectionCode(section_code)
      rc.setFirst(true)
      rc.setLast(true)
      List(rc)
    } else {
      val tmp = ListBuffer[AmqpFragment]()
      var current_max = 0L
      while (current_max < payload.length) {
        val frag = createAmqpFragment
        frag.setSectionCode(section_code)
        frag.setFirst(false)
        frag.setLast(false)
        frag.setPayload(payload.slice(current_max.intValue, (current_max + max_size).min(payload.length).intValue))
        current_max = current_max + max_size
        tmp.append(frag)
      }
      tmp.head.setFirst(true)
      tmp.last.setLast(true)
      tmp.toList
    }
  }

  val default_offset = new BigInteger("7")

  def figure_out_section_offset(fragment:AmqpFragment) = {
    /*
    var offset = new BigInt(BigInteger.ZERO)
    val size = fragment.getListCount
    var i = 0
    // element 5 is the payload, 4 is section offset
    for ( i <- 0 to (size - 3) ) {
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
    */
    // This is only going to change if they change the fragment type
    fragment.setSectionOffset(default_offset)
  }

  def maybe_add(l:ListBuffer[AmqpFragment], o:Option[AmqpType[_, _]], section_code:Long, max_size:Long) = {
    o.foreach((x) => {
      val payload = new Buffer(CodecUtils.marshal(x.asInstanceOf[AmqpType[_, AmqpBuffer[_]]]))
      l.appendAll(marshal_section(payload, section_code, max_size))
    })
  }

  def marshal_to_multiple(max_size:Long) = {
    val rc = createMultiple
    rc.setValue(list_to_amqp_list(marshal_to_amqp_fragments(max_size)))
    rc
  }

  def marshal_to_amqp_fragments(max_size:Long) = {
    val rc = ListBuffer[AmqpFragment]()
    // need to check section codes...
    maybe_add(rc, _header, HEADER, max_size)
    maybe_add(rc, _delivery_annotations, DELIVERY_ANNOTATIONS, max_size)
    maybe_add(rc, _message_annotations, MESSAGE_ANNOTATIONS, max_size)
    maybe_add(rc, _properties, PROPERTIES, max_size)
    maybe_add(rc, _application_properties, APPLICATION_PROPERTIES, max_size)
    if (has_body) {
      rc.appendAll(marshal_section(marshal_body, get_section_code, max_size))
    }
    maybe_add(rc, _footer, FOOTER, max_size)

    var i = 0
    var current_section_code = rc.head.getSectionCode
    var current_section_number = 0
    rc.foreach( (x) => {
      if (x.getSectionCode != current_section_code) {
        current_section_code = x.getSectionCode
        current_section_number = current_section_number + 1
      }
      x.setSectionNumber(current_section_number)
      figure_out_section_offset(x)
      i = i + 1
    })

    rc.toList
  }

  def unmarshal_from_amqp_fragments(fragments:List[AmqpFragment]) = {
    val sections = break_up_message_sections(fragments)
    sections.foreach {
      case(section, fragments) =>
        assemble_section(section, fragments.toList)
    }
  }

  def assemble_section(section:Long, fragments:List[AmqpFragment]) = {
    val baos = new ByteArrayOutputStream
    val out:DataOutput = new DataOutputStream(baos)
    fragments.foreach((x) => {
      x.getPayload.writeTo(out)
    })

    baos.flush

    section match {
      case HEADER =>
        _header = Option(CodecUtils.unmarshal(baos.toByteArray))
      case DELIVERY_ANNOTATIONS =>
        _delivery_annotations = Option(CodecUtils.unmarshal(baos.toByteArray))
      case MESSAGE_ANNOTATIONS =>
        _message_annotations = Option(CodecUtils.unmarshal(baos.toByteArray))
      case PROPERTIES =>
        _properties = Option(CodecUtils.unmarshal(baos.toByteArray))
      case APPLICATION_PROPERTIES =>
        _application_properties = Option(CodecUtils.unmarshal(baos.toByteArray))
      case DATA =>
        unmarshal_body(baos.toBuffer)
      case AMQP_DATA =>
        unmarshal_body(baos.toBuffer)
      case AMQP_MAP_DATA =>
        unmarshal_body(baos.toBuffer)
      case AMQP_LIST_DATA =>
        unmarshal_body(baos.toBuffer)
      case FOOTER =>
        _footer = Option(CodecUtils.unmarshal(baos.toByteArray))
    }

  }

  def break_up_message_sections(fragments:List[AmqpFragment]) = {
    val rc = HashMap[Long, ListBuffer[AmqpFragment]]()
    fragments.foreach((x) => {
      val code = x.getSectionCode.longValue
      rc.get(code) match {
        case Some(fragments) =>
          fragments.append(x)
        case None =>
          rc.put(code, ListBuffer[AmqpFragment](x))
      }
    })
    rc
  }

  override def toString = {
    val buf = new StringBuilder
    buf.append(getClass.getSimpleName)
    buf.append("{")
    _header.foreach((x) => {
      buf.append(" header=")
      buf.append(x)
    })
    _delivery_annotations.foreach((x) => {
      buf.append(" delivery_annotations=")
      buf.append(x)
    })
    _message_annotations.foreach((x) => {
      buf.append(" message_annotations=")
      buf.append(x)
    })
    _properties.foreach((x) => {
      buf.append(" properties=")
      buf.append(x)
    })
    _application_properties.foreach((x) => {
      buf.append(" application_properties=")
      buf.append(x)
    })
    if (has_body) {
      buf.append(" body=")
      buf.append(body_as_string)
    }
    _footer.foreach((x) => {
      buf.append(" footer=")
      buf.append(x)
    })
    buf.append("}")
    buf.toString
  }

  def body_as_string:String
  def get_section_code:Long
  def unmarshal_body(body:Buffer)
  def marshal_body:Buffer
  def has_body:Boolean
}

trait GenericMessageTrait[T] extends GenericMessage[T] {
  var body:T = null.asInstanceOf[T]
  def getBody = body
  def setBody(body:T) = this.body = body
}


