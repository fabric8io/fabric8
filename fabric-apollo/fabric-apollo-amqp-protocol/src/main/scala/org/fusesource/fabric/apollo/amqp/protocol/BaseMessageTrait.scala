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
}

trait GenericMessageTrait[T] extends GenericMessage[T] {
  var body:T = null.asInstanceOf[T]

  def getBody = body

  def setBody(body:T) = this.body = body
}

class DataMessageImpl extends DataMessage with BaseMessageTrait with GenericMessageTrait[Buffer]

class AmqpDataMessageImpl extends DataMessageImpl with BaseMessageTrait with GenericMessageTrait[Buffer]

class AmqpMapMessageImpl extends AmqpMapMessage with BaseMessageTrait with GenericMessageTrait[AmqpFields]

class AmqpListMessageImpl extends AmqpListMessage with BaseMessageTrait with GenericMessageTrait[AmqpList]

