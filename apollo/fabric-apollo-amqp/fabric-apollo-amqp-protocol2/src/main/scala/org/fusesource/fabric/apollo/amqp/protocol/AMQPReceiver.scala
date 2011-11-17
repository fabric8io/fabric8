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

import api.{CreditHandler, MessageHandler, Receiver}
import interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.{Target, Outcome}
import org.fusesource.fabric.apollo.amqp.codec.types.{Source, Attach, Role, ReceiverSettleMode}


object AMQPReceiver {
  def create(name:String) = {
    val rc = new AMQPReceiver
    rc.setName(name)
    rc
  }

  def create(attach:Attach) = {
    val rc = new AMQPReceiver
    rc.setName(attach.getName)
    rc.setTarget(attach.getTarget.asInstanceOf[Target])
    rc.setSource(attach.getSource.asInstanceOf[Source])
    rc.setMaxMessageSize(attach.getMaxMessageSize.longValue)
    rc
  }
}
/**
 *
 */
class AMQPReceiver extends Interceptor with Receiver with AMQPLink {

  def setCreditHandler(handler: CreditHandler) {}

  def setMessageHandler(handler: MessageHandler[_]) {}

  def setSettleMode(mode: ReceiverSettleMode) {}

  def settle(deliveryId: Long, outcome: Outcome) {}

  def addLinkCredit(credit: Int) {}

  def drainLinkCredit() {}

  def getRole = Role.RECEIVER

  def established() = false
}