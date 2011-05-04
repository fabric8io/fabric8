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

import org.fusesource.fabric.apollo.amqp.codec.AmqpCommand
import org.fusesource.hawtdispatch.DispatchQueue
import org.fusesource.fabric.apollo.amqp.codec.types.{AmqpBuffer, AmqpType, AmqpFlow, AmqpError}

trait LinkSession {
  def established:Boolean
  def attach(link:AmqpLink)
  def detach(link:AmqpLink)
  def send(link:AmqpLink, command:AmqpCommand)
  def send(link:AmqpLink, message:AmqpProtoMessage):Unit
  def settle_incoming(message:AmqpProtoMessage, outcome:AmqpType[_, AmqpBuffer[_]]);
  def end(error:Option[AmqpError])
  def dispatch_queue:DispatchQueue
}
