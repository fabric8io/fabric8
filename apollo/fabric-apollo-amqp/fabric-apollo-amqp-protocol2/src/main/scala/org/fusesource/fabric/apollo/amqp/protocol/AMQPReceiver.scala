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
import commands.LinkCommand
import interfaces.{Interceptor, FrameInterceptor}
import Interceptor._
import org.fusesource.fabric.apollo.amqp.codec.interfaces.{Target, Outcome}
import org.fusesource.fabric.apollo.amqp.codec.types.{Source, Attach, Role, ReceiverSettleMode}
import org.apache.activemq.apollo.util.Logging
import collection.mutable.Queue


object AMQPReceiver {
  def create(name:String) = {
    val rc = new AMQPReceiver
    rc.setName(name)
    rc
  }

  def create(attach:Attach) = {
    val rc = new AMQPReceiver
    AMQPLink.initialize(rc, attach)
  }
}
/**
 *
 */
class AMQPReceiver extends Interceptor with Receiver with AMQPLink with Logging {
  
  trace("Constructed AMQP receiver chain : %s", display_chain(this))
  
  override def configure_attach(attach:Attach):Attach = {
    val a = super.configure_attach(attach)
    trace("Configured receiver attach : %s", a)
    a
  }

  def setCreditHandler(handler: CreditHandler) {}

  def setMessageHandler(handler: MessageHandler[_]) {}

  def setSettleMode(mode: ReceiverSettleMode) {}

  def settle(deliveryId: Long, outcome: Outcome) {}

  def addLinkCredit(credit: Int) {}

  def drainLinkCredit() {}

  def getRole = Role.RECEIVER

  def established() = false

}