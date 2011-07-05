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

import org.fusesource.fabric.apollo.amqp.codec.CodecUtils
import org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.AmqpMarshaller
import org.fusesource.fabric.apollo.amqp.api.{AmqpListMessage, AmqpMapMessage, DataMessage}
import org.fusesource.fabric.apollo.amqp.codec.types.{AmqpList, AmqpFields}
import org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory._
import org.fusesource.hawtbuf.Buffer

import MessageSectionCodes._

class DataMessageImpl extends DataMessage with BaseMessageTrait with GenericMessageTrait[Buffer] {
  override def get_section_code = DATA
  override def marshal_body = body
  override def unmarshal_body(body:Buffer) = this.body = body
  override def has_body = body != null
  override def body_as_string = body.toString
}

// Same as above but body section code is different
class AmqpDataMessageImpl extends DataMessageImpl with BaseMessageTrait with GenericMessageTrait[Buffer] {
  override def get_section_code = AMQP_DATA
}

class AmqpMapMessageImpl extends AmqpMapMessage with BaseMessageTrait with GenericMessageTrait[AmqpFields] {
  body = createAmqpFields
  override def get_section_code = AMQP_MAP_DATA
  override def marshal_body = new Buffer(CodecUtils.marshal(body))
  override def unmarshal_body(body:Buffer) = this.body = AmqpFields.AmqpFieldsBuffer.create(body, body.getOffset, AmqpMarshaller.getMarshaller)
  override def has_body = body != null
  override def body_as_string = body.toString
}

class AmqpListMessageImpl extends AmqpListMessage with BaseMessageTrait with GenericMessageTrait[AmqpList] {
  body = createAmqpList
  override def get_section_code = AMQP_LIST_DATA
  override def marshal_body = new Buffer(CodecUtils.marshal(body))
  override def unmarshal_body(body:Buffer) = this.body = AmqpList.AmqpListBuffer.create(body, body.getOffset, AmqpMarshaller.getMarshaller)
  override def has_body = body != null
  override def body_as_string = body.toString
}

