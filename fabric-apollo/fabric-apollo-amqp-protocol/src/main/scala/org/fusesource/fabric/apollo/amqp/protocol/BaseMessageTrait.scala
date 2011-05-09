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
import org.fusesource.fabric.apollo.amqp.codec.types.{AmqpList, AmqpFields}
import org.fusesource.fabric.apollo.amqp.api._

/**
 *
 */
trait BaseMessageTrait extends BaseMessage {

  def getFooter = null

  def getApplicationProperties = null

  def getProperties = null

  def getMessageAnnotations = null

  def getDeliveryAnnotations = null

  def getHeader = null

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

