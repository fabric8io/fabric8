/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.protocol

import api.{DeliveryTagger, AvailableHandler, AckHandler, Sender}
import interfaces.Interceptor
import Interceptor._
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.api.{AnnotatedMessage, BareMessage}
import org.fusesource.hawtbuf.Buffer
import utilities.execute
import org.fusesource.fabric.apollo.amqp.codec.marshaller.MessageSupport._
import utilities.link.LinkFlowControlTracker
import org.fusesource.fabric.apollo.amqp.codec.interfaces.{Target, AMQPFrame}
import org.fusesource.fabric.apollo.amqp.codec.types.{Source, Attach, Role, SenderSettleMode}
import org.apache.activemq.apollo.util.Logging

/**
 *
 */
object AMQPSender {
  def create(name:String) = {
    val rc = new AMQPSender
    rc.setName(name)
    rc
  }
  
  def create(attach:Attach) = {
    val rc = new AMQPSender
    AMQPLink.initialize(rc, attach)
  }
}

class AMQPSender extends AMQPLink with Sender with Logging {

  trace("Constructed AMQP sender chain : %s", display_chain(this))

  def full() = getSession.sufficientSessionCredit() && tracker.credit

  def offer(message: Buffer):Boolean = false

  def offer(message: AnnotatedMessage):Boolean = false

  def offer(message: BareMessage[_]):Boolean = false

  def setTagger(tagger: DeliveryTagger) {}

  def setAvailableHandler(handler: AvailableHandler) {}

  def setAckHandler(handler: AckHandler) {}

  def setSettleMode(mode: SenderSettleMode) {}

  def refiller(refiller: Runnable) {}

  def getLinkCredit = 0

  def getRole = Role.SENDER

}