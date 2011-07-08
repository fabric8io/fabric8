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

import protocol._
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.hawtdispatch._
import org.fusesource.hawtbuf.Buffer._
import java.util.concurrent._
import atomic.{AtomicLong, AtomicBoolean}
import org.apache.activemq.apollo.util.Logging
import collection.mutable.ListBuffer
import org.fusesource.fabric.apollo.amqp.api.{Outcome, MessageListener, Message, Receiver}
import scala.math._
import org.apache.activemq.apollo.broker.{Sink, OverflowSink}
import org.fusesource.hawtbuf.Buffer

/**
 *
 */
class IncomingLink(session:LinkSession) extends AmqpLink(session) with Receiver with Logging {

  def role = Role.RECEIVER

  var _listener:Option[MessageListener] = None
  var incoming:OverflowSink[Message] = null

  var link_credit:Option[Long] = Option(0L)
  var available:Option[Long] = None
  var transfer_count:Option[Long] = None

  def isFlowControlEnabled():Boolean = link_credit == None

  override def attach(a:Attach) = {
    super.attach(a)
    Option(a.getInitialDeliveryCount) match {
      case Some(c) =>
        transfer_count = Option(c.longValue)
      case None =>
    }
  }

  def enableFlowControl(enable:Boolean) = {
    if (enable) {
      link_credit = Option(0L)
    } else {
      link_credit = None
    }
  }

  enableFlowControl(true)

  def settle(message: Message, outcome:Outcome): Unit = {
    /*
    val msg:AmqpProtoMessage = message
    // TODO - for now, the application should set this
    msg.settled = true
    session.settle_incoming(msg, outcome2AmqpType(outcome))
    */
  }

  override def flowstate = {
    val rc = super.flowstate
    link_credit.foreach((x) => rc.setLinkCredit(x))
    available.foreach((x) => rc.setAvailable(x))
    transfer_count.foreach((x) => rc.setDeliveryCount(x))
    rc
  }

  def getAvailable = available.getOrElse(0L)

  def getAvailableLinkCredit:java.lang.Long = link_credit.getOrElse(null).asInstanceOf[java.lang.Long]

  override def attach() = {
    require(_listener != None, "Listener must be set before link can be attached")
    super.attach
    incoming.refiller.run
  }

  def listener:MessageListener = _listener.getOrElse(null)
  def listener_=(l:MessageListener) = {
    require(!established, "Link cannot be attached when setting the listener")
    val link = this
    _listener = Option(l)
    _listener.foreach((l) => {
      incoming = new OverflowSink[Message](new Sink[Message] {
        var refiller:Runnable = null
        def offer(value: Message) = {
          try {
            l.offer(link, value)
          } catch {
            case t:Throwable =>
              info("Message listener threw exception %s, rejecting message", t.getStackTraceString)
              val error = new Error
              error.setCondition(ascii("Application error"))
              error.setDescription(t.getLocalizedMessage)
              val rejected = new Rejected
              rejected.setError(error)
              //value.setSettled(true)
              session.settle_incoming(value, rejected)
              true
          }

        }
        def full = l.full
      })
      incoming.refiller = ^{
        addLinkCredit(l.needLinkCredit(available.getOrElse(0L)))
      }
      l.refiller(incoming.downstream.refiller)
    })
  }
  def setListener(listener: MessageListener) = listener_=(listener)

  def addLinkCredit(credit:Long): Unit = {
    if (credit == 0) {
      return
    }

    link_credit = Option(link_credit match {
      case Some(c) =>
        c + credit
      case None =>
        credit
    })
    send_updated_flow_state(flowstate)
  }

  def drainLinkCredit():Unit = {
    link_credit = Option(0L)
    val flow = flowstate
    flow.setDrain(true)
    send_updated_flow_state(flow)
  }

  def transfer(message:Message): Unit = {
    trace("Received incoming message : %s", message)
    available.foreach((x) => if (x > 0) {available = Option(x - 1)} else {available = Option(0L)})
    transfer_count.foreach((x) => transfer_count = Option(x + 1))
    // TODO - Should probably reject transfer if sender has exceeded available link credit, for now we'll be forgiving.
    link_credit.foreach((x) => if (x > 0) {link_credit = Option(x - 1)} else {link_credit = Option(0L)})
    incoming.offer(message)
  }

  override def peer_flowstate(flowState: Flow): Unit = {
    available = flowState.getAvailable
    transfer_count = flowState.getDeliveryCount

    def update_peer = send_updated_flow_state(flowstate)

    Option(flowState.getLinkCredit) match {
      case Some(credit) =>
        link_credit match {
          case Some(c) =>
            if (credit.longValue < c) {
              update_peer
            }
            if (credit.longValue <= 0) {
              _listener.foreach((l) => addLinkCredit(l.needLinkCredit(available.get)))
            }
          case None =>
            update_peer
        }
      case None =>
        link_credit match {
          case Some(c) =>
            update_peer
          case None =>
        }
    }
    super.peer_flowstate(flowState)
  }

}
