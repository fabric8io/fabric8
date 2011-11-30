/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol

import api.{DeliveryTagger, AvailableHandler, AckHandler, Sender}
import interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.api.{AnnotatedMessage, BareMessage}
import org.fusesource.hawtbuf.Buffer
import org.fusesource.fabric.apollo.amqp.codec.types.{Role, SenderSettleMode}
import utilities.execute

/**
 *
 */
object AMQPSender {
  def create(name:String) = {
    val rc = new AMQPSender
    rc.setName(name)
    rc
  }
}

class AMQPSender extends Interceptor with Sender with AMQPLink {
  def full() = false

  def offer(message: Buffer) = false

  def offer(message: AnnotatedMessage) = false

  def offer(message: BareMessage[_]) = false

  def setTagger(tagger: DeliveryTagger) {}

  def setAvailableHandler(handler: AvailableHandler) {}

  def setAckHandler(handler: AckHandler) {}

  def setSettleMode(mode: SenderSettleMode) {}

  def refiller(refiller: Runnable) {}

  def getLinkCredit = 0

  override protected def _receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = execute(tasks)

  def getRole = Role.SENDER

  def established() = false
}