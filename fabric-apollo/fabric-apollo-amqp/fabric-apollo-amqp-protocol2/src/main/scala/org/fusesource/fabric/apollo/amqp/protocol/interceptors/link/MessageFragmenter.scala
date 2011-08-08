/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.link

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.PerformativeInterceptor
import collection.mutable.Queue
import org.fusesource.hawtbuf.Buffer
import org.fusesource.fabric.apollo.amqp.codec.types.{AMQPTransportFrame, Transfer}
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Tasks

/**
 *
 */

class MessageFragmenter extends PerformativeInterceptor[Transfer] {

  var max_message_size = 0L

  override protected def send(t:Transfer, payload:Buffer, tasks:Queue[() => Unit]):Boolean = {
    if (max_message_size == 0L) {
      false
    } else if (payload.length() <= max_message_size) {
      false
    } else {
      fragment(0, max_message_size, t, payload, tasks)
      true
    }
  }

  private def fragment(low:Long, high:Long, transfer:Transfer, payload:Buffer, tasks:Queue[() => Unit]):Unit = {
    implicit def long2int(i:Long) = i.asInstanceOf[Int]
    if (high < payload.length) {
      val b = payload.slice(low, high)
      val t = copy_transfer(transfer)
      if (low == 0L) {
        t.setDeliveryID(transfer.getDeliveryID)
        t.setDeliveryTag(transfer.getDeliveryTag)
      }
      t.setMore(true)
      send(new AMQPTransportFrame(t, b), Tasks())
      fragment(high, high + max_message_size, transfer, payload, tasks)
    } else {
      val b = payload.slice(low, payload.length)
      send(new AMQPTransportFrame(copy_transfer(transfer), b), tasks)
    }
  }

  private def copy_transfer(t:Transfer):Transfer = {
    val rc = new Transfer
    rc.setMessageFormat(t.getMessageFormat)
    rc.setSettled(t.getSettled)
    rc.setRcvSettleMode(t.getRcvSettleMode)
    rc.setState(t.getState)
    rc.setAborted(t.getAborted)
    rc.setBatchable(t.getBatchable)
    rc
  }
}

