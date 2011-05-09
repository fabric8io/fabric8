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

// TODO - Assemble message from list of fragments

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
    rc.setPayload(new Buffer(CodecUtils.marshal(section_body)))
    rc.setSectionCode(section_code)
    // TODO - section offset, section number?
    rc.setFirst(true)
    rc.setLast(true)
    rc
  }

  def maybe_add(l:ListBuffer[AmqpFragment], o:Option[AmqpType[_, _]], section_code:Long) = o.foreach((x) => l.append(marshal_single_fragment_section(x, section_code)))

  def marshal_to_amqp_fragments = {
    val rc = ListBuffer[AmqpFragment]()
    // need to check section codes...
    maybe_add(rc, _header, 0)
    maybe_add(rc, _message_annotations, 1)
    maybe_add(rc, _delivery_annotations, 2)
    maybe_add(rc, _properties, 3)
    maybe_add(rc, _application_properties, 4)
    // add body here...

    maybe_add(rc, _footer, 6)

    rc.toList
  }


  def marshal_application_data_section

}

trait GenericMessageTrait[T] extends GenericMessage[T] {
  var body:T = null.asInstanceOf[T]

  def getBody = body
  def setBody(body:T) = this.body = body
}

class DataMessageImpl extends DataMessage with BaseMessageTrait with GenericMessageTrait[Buffer] {
  override def marshal_application_data_section = {
    // TODO
  }
}

// Same as above but body section code is different
class AmqpDataMessageImpl extends DataMessageImpl with BaseMessageTrait with GenericMessageTrait[Buffer]

class AmqpMapMessageImpl extends AmqpMapMessage with BaseMessageTrait with GenericMessageTrait[AmqpFields] {
  body = createAmqpFields

  override def marshal_application_data_section = {
    // TODO
  }
}

class AmqpListMessageImpl extends AmqpListMessage with BaseMessageTrait with GenericMessageTrait[AmqpList] {
  body = createAmqpList

  override def marshal_application_data_section = {
    // TODO
  }
}

