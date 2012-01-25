/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
class AMQPReceiver extends AMQPLink with Receiver with Logging {
  
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

}