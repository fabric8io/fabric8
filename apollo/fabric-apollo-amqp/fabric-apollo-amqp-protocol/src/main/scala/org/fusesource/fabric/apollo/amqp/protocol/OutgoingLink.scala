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

import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.hawtdispatch._
import org.apache.activemq.apollo.util.Logging
import org.apache.activemq.apollo.broker.{OverflowSink, Sink}
import scala.util.continuations._
import org.fusesource.fabric.apollo.amqp.api.{Message, Sender}

/**
 *
 */
// TODO - Set default outcome/accepted outcomes, keep this list short for starters

class OutgoingLink(session:LinkSession) extends AmqpLink(session) with Sender with Sink[Message] with Logging {

  def role = Role.SENDER

  var link_credit:Option[Long] = Option(0L)
  var transfer_count = 0L
  var available = 0L
  var flowControlListener:Option[FlowControlListener] = None
  var refiller:Runnable = null

  val outgoing = new OverflowSink[Message](this)
  outgoing.refiller = NOOP
  val dispatch = session.dispatch_queue

  override def flowstate = {
    val rc = super.flowstate
    link_credit.foreach((x) => rc.setLinkCredit(x))
    rc.setAvailable(available)
    rc.setDeliveryCount(transfer_count)
    rc
  }

  def isFlowControlEnabled():Boolean = link_credit == None

  def canSend = {
    link_credit match {
      case Some(credit) =>
        available < credit
      case None =>
        true
    }
  }

  def sufficientLinkCredit = {
    link_credit match {
      case Some(credit) =>
        credit > 0
      case None =>
        true
    }
  }

  def transfer(message:Message) = {
    throw new RuntimeException("Can't transfer a message to an outgoing link")
  }

  def put(message:Message):Boolean  = {
    // TODO - Could buffer messages in case the link gets detached and then eventually re-attached
    require(remoteHandle != None, "Link not established")

    def add = {
      trace("Adding new outgoing message %s", message)
      available = available + 1
      val rc = outgoing.offer(message)

      //protoMessage.onPut.foreach((x) => dispatch << x)
    }

    if (Dispatch.getCurrentQueue == dispatch) {
      add
    } else {
      dispatch {
        add
      }
    }
    true
  }

  override def peer_flowstate(flowState: Flow): Unit = {
    Option(flowState.getLinkCredit) match {
      case Some(credit) =>
        link_credit match {
          case Some(c) =>
            link_credit = Option(credit.longValue)
            Option(flowState.getDrain) match {
              case Some(drain) =>
                if (drain.asInstanceOf[Boolean]) {
                  transfer_count += c
                }
              case None =>
            }
          case None =>
            link_credit = Option(credit.longValue)
        }
      case None =>
        link_credit = None
    }

    trace("Updated link credit to %s", link_credit)

    flowControlListener match {
      case Some(listener) =>
        if (canSend) {
          listener.canSend
        }
      case None =>
    }

    Option(refiller).foreach((x) => dispatch << x)
    super.peer_flowstate(flowState)
  }

  def setFlowControlListener(listener: FlowControlListener): Unit = {
    flowControlListener = Option(listener)
  }

  def setRefiller(r:Runnable) = outgoing.refiller = r

  def empty = !outgoing.overflowed

  def offer(message:Message) = {
    if (!established) {
      trace("received message offer but not established")
      false
    } else if (!sufficientLinkCredit) {
      trace("received message offer but insufficient link credit (%s)", link_credit)
      send_updated_flow_state(flowstate)
      false
    } else {
      trace("received message offer and am able to send")
      link_credit.foreach((x) => link_credit = Option(x - 1))
      transfer_count = transfer_count + 1
      available = available - 1
      trace("Sending message: %s", message)
      session.send(this, message)
      true
    }
  }

  def full = !canSend
}
