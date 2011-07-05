/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.broker

import org.fusesource.hawtdispatch._
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.marshaller.v1_0_0.AmqpMarshaller
import org.fusesource.hawtbuf.AsciiBuffer.ascii
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory._
import org.fusesource.fabric.apollo.amqp.protocol._
import org.fusesource.fabric.apollo.amqp.api._
import org.fusesource.fabric.apollo.amqp.api.Message
import org.apache.activemq.apollo.dto.DestinationDTO
import org.fusesource.fabric.apollo.amqp.codec.types.{AmqpTransfer, AmqpRole}
import org.apache.activemq.apollo.broker._
import scala.math._

/**
 * An AMQP message listener that produces message deliveries
 */
class AmqpDeliveryProducer(val handler:AmqpProtocolHandler, val link:Receiver, val destination:Array[DestinationDTO]) extends MessageListener with Logging {

  val batch_size = {
    val options = link.getTargetOptionsMap
    Option[AmqpType[_, _]](options.get(createAmqpSymbol("batch-size"))) match {
      case Some(size) =>
        size.asInstanceOf[AmqpLong].getValue.longValue
      case None =>
        10L
    }
  }

  // TODO - check if null, if so check dynamic
  val addr = ascii(link.getAddress)

  val producer = new DeliveryProducerRoute(handler.host.router) {
    override def connection = Some(handler.connection)
    override def dispatch_queue = handler.dispatchQueue

    refiller = ^{
      //trace("Running refiller %s", transportRefiller)
      transportRefiller.run
    }
  }

  handler.connectDeliveryProducer(this)

  def needLinkCredit(available:Long) : Long = {
    if (producer.full) {
      0L
    } else {
      available.max(batch_size)
    }
  }

  def offer(receiver:Receiver, message:Message) = {

    assert(receiver == link)

    val protoMessage = message.asInstanceOf[AmqpProtoMessage]
    //trace("Received message : %s", protoMessage);
    val delivery = new Delivery
    val message_transfer = new AmqpMessageTransfer(protoMessage, createAmqpString(link.getName), createAmqpString(link.getAddress))
    delivery.message = message_transfer
    delivery.size = message_transfer.size
    // TODO - when transactions are supported
    delivery.uow = null;

    // TODO - Fix up API so a message can be settled by delivery tag so this runnable doesn't have a reference to the message
    delivery.ack = { (consumed, uow) => {
        if (!message.getSettled) {
          consumed match {
            case Delivered=>
              link.settle(message, Outcome.ACCEPTED)
            case Expired=>
              link.settle(message, Outcome.ACCEPTED)
            case Undelivered=>
              link.settle(message, Outcome.REJECTED)
            case Poisoned=>
              link.settle(message, Outcome.REJECTED)
          }
        }
        if (uow != null) {
          uow.complete_asap
        }
      }
    }

    assert(!producer.full)
    producer.offer(delivery)
    if(producer.full) {
      //trace("Delivery producer full, flow-controlling client")
      link.drainLinkCredit;
      //handler.suspendRead("blocked destination: " + producer.overflowSessions.mkString(", "))
      false
    } else {
      val available = link.getAvailableLinkCredit
      if (available != null && available.longValue < 5) {
        link.addLinkCredit(batch_size - available.longValue)
      }
      true
    }
  }

  var refiller:Runnable = null

  def transportRefiller = refiller

  def refiller(r:Runnable) = {
    refiller = r
    //trace("Setting refiller to %s", refiller)
  }

  def full = producer.full
}
