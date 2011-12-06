/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.session

import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPTransportFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.PerformativeInterceptor
import org.fusesource.hawtbuf.Buffer
import org.fusesource.fabric.apollo.amqp.codec.types.{Transfer, Begin, Flow}
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Tasks
import org.fusesource.hawtdispatch._

/**
 *
 */
class SessionFlowControlInterceptor extends PerformativeInterceptor[Flow] with Logging {

  var incoming_window_max = 0L
  var current_transfer_id = 0L

  val flow = new Flow
  val peer = new Flow

  val initial_config_interceptor = new PerformativeInterceptor[Begin] {
    override protected def send(begin:Begin, payload:Buffer, tasks:Queue[() => Unit]) = {
      flow.setOutgoingWindow(begin.getOutgoingWindow)
      flow.setIncomingWindow(begin.getIncomingWindow)
      flow.setNextOutgoingID(begin.getNextOutgoingID)
      flow.setIncomingWindow(begin.getIncomingWindow)
      flow.setOutgoingWindow(begin.getOutgoingWindow)
      incoming_window_max = begin.getIncomingWindow
      current_transfer_id = begin.getNextOutgoingID
      false
    }

    override protected def receive(begin:Begin, payload:Buffer, tasks:Queue[() => Unit]) = {
      peer.setIncomingWindow(begin.getIncomingWindow)
      peer.setOutgoingWindow(begin.getOutgoingWindow)
      peer.setNextOutgoingID(begin.getNextOutgoingID)
      flow.setNextIncomingID(begin.getNextOutgoingID)
      false
    }
  }

  val transfer_interceptor = new PerformativeInterceptor[Transfer] {
    override protected def send(transfer:Transfer, payload:Buffer, tasks:Queue[() => Unit]) = {
      flow.setNextOutgoingID(flow.getNextOutgoingID + 1L)
      flow.setOutgoingWindow(flow.getOutgoingWindow - 1L)
      peer.setIncomingWindow(peer.getIncomingWindow - 1L)
      false
    }

    override protected def receive(transfer:Transfer, payload:Buffer, tasks:Queue[() => Unit]) = {
      flow.setNextIncomingID(flow.getNextIncomingID + 1)
      flow.setIncomingWindow(flow.getIncomingWindow - 1)
      peer.setOutgoingWindow(peer.getOutgoingWindow - 1)
      if (peer.getOutgoingWindow < 1) {
        flow.setEcho(true)
        queue {
          send(new AMQPTransportFrame(flow), Tasks(() => flow.setEcho(false)))
        }
      }
      if (flow.getIncomingWindow < 1) {
        flow.setIncomingWindow(incoming_window_max)
        queue {
          send(new AMQPTransportFrame(flow), Tasks())
        }
      }
      false
    }
  }

  override protected def adding_to_chain = {
    foreach((i) => {
      i match {
        case b:BeginInterceptor =>
          b.before(initial_config_interceptor)
        case _ =>
      }
    })
    if (!initial_config_interceptor.connected) {
      throw new RuntimeException("No BeginInterceptor found in chain, cannot properly configure session flow control interceptor")
    }
    after(transfer_interceptor)
  }

  override protected def removing_from_chain = {
    initial_config_interceptor.remove
    transfer_interceptor.remove
  }

  override protected def send(f:Flow, payload:Buffer, tasks:Queue[() => Unit]) = {
    f.setNextOutgoingID(flow.getNextOutgoingID)
    f.setIncomingWindow(flow.getIncomingWindow)
    f.setOutgoingWindow(flow.getOutgoingWindow)
    false
  }

  override protected def receive(f:Flow, payload:Buffer, tasks:Queue[() => Unit]) = {
    flow.setNextIncomingID(f.getNextOutgoingID)
    peer.setOutgoingWindow(f.getOutgoingWindow)
    Option(f.getNextIncomingID) match {
      case Some(id) =>
        peer.setIncomingWindow(f.getNextIncomingID + f.getIncomingWindow - flow.getNextOutgoingID)
      case None =>
        peer.setIncomingWindow(1L + flow.getIncomingWindow - flow.getNextOutgoingID)
    }

    Option(f.getHandle) match {
      case Some(handle) =>
        false
      case None =>
        val echo:Boolean = Option(flow.getEcho) match {
          case Some(echo) =>
            echo
          case None =>
            false
        }
        if (echo) {
          queue {
            send(new AMQPTransportFrame(flow), Tasks())
          }
        }
        true
    }
  }

}