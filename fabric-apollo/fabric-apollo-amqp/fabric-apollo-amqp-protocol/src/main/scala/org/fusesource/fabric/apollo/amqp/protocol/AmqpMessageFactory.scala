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
import org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory.createAmqpList

import MessageSectionCodes._

/**
 * Convenience class to create an AMQP message from a multiple (list of fragments in a transfer frame) or just a scala list of fragments
 */
object AmqpMessageFactory {

  def createDataMessage = new DataMessageImpl().asInstanceOf[DataMessage]
  def createAmqpDataMessage = new AmqpDataMessageImpl().asInstanceOf[AmqpDataMessage]
  def createAmqpMapMessage = new AmqpMapMessageImpl().asInstanceOf[AmqpMapMessage]
  def createAmqpListMessage = new AmqpListMessageImpl().asInstanceOf[AmqpListMessage]

  def list_to_amqp_list[T <: AmqpType[_, _]](list:List[T]) = createAmqpList(new IAmqpList.ArrayBackedList(list.toArray))
  def amqp_list_to_list[T <: AmqpType[_, _]](list:AmqpList) = list.toArray.toList

  def create_from_multiple(fragments:Multiple) = {
    val list = fragments.getValue.asInstanceOf[AmqpList]
    create_from_fragments(amqp_list_to_list(list).asInstanceOf[List[AmqpFragment]])
  }

  def create_from_fragments(fragments:List[AmqpFragment]) = {
    val rc:BaseMessageTrait = get_message_body_type(fragments) match {
      case 0L =>
        new DataMessageImpl
      case DATA =>
        new DataMessageImpl
      case AMQP_DATA =>
        new AmqpDataMessageImpl
      case AMQP_MAP_DATA =>
        new AmqpMapMessageImpl
      case AMQP_LIST_DATA =>
        new AmqpListMessageImpl
      case _ =>
        throw new RuntimeException("Message body type is unknown");
    }
    rc.unmarshal_from_amqp_fragments(fragments)
    rc
  }

  def get_message_body_type(fragments:List[AmqpFragment]):Long = {
    var rc = 0L
    fragments.foreach((x) => {
      x.getSectionCode.longValue match {
        case DATA =>
          rc = DATA
        case AMQP_DATA =>
          rc = AMQP_DATA
        case AMQP_MAP_DATA =>
          rc = AMQP_MAP_DATA
        case AMQP_LIST_DATA =>
          rc = AMQP_LIST_DATA
        case _ =>
      }
    })
    rc
  }
}
