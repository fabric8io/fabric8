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

import org.fusesource.hawtdispatch.DispatchQueue
import org.fusesource.fabric.apollo.amqp.api.{Message}
import org.fusesource.fabric.apollo.amqp.codec.interfaces.{Outcome, Frame}
import org.fusesource.fabric.apollo.amqp.codec.types.Error

trait LinkSession {
  def established:Boolean
  def attach(link:AmqpLink)
  def detach(link:AmqpLink)
  def send(link:AmqpLink, command:Frame)
  def send(link:AmqpLink, message:Message):Unit
  def settle_incoming(message:Message, outcome:Outcome);
  def end(error:Option[Error])
  def dispatch_queue:DispatchQueue
}
