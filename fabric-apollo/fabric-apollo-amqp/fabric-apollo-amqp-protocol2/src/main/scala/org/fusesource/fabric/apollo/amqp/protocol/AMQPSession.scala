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

import api.{Connection, Link}
import org.fusesource.hawtdispatch._
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import org.fusesource.fabric.apollo.amqp.protocol.commands._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import Interceptor._
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPTransportFrame
import utilities.{execute, Tasks}
import org.apache.activemq.apollo.util.Logging

/**
 *
 */
class AMQPSession extends Interceptor with AbstractSession with Logging {

  var connection:Connection = null

  head.outgoing = _flow
  head.outgoing = _end
  head.outgoing = _begin

  tail.incoming = _links

  setOutgoingWindow(10L)
  setIncomingWindow(10L)

  trace("Constructed session chain : %s", display_chain(this))

  _links.interceptor_factory = Option((frame:AMQPTransportFrame) => {

    null.asInstanceOf[Interceptor]
  })

  def link_name_prefix = connection.getContainerID + "," + connection.getPeerContainerID + ","

  def begin(on_begin: Runnable) = {
    this.on_begin = Option(on_begin)
    send(BeginSession(), Tasks())
  }

  def end() = _end.send(EndSession(), Tasks())

  def end(t: Throwable) = _end.send(EndSession(t), Tasks())

  def end(reason: String) = _end.send(EndSession(reason), Tasks())

  override protected def _receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case t:AMQPTransportFrame =>
        incoming.receive(frame, tasks)
      case x:BeginReceived =>
        begin_sent_or_received
        execute(tasks)
      case x:BeginSent =>
        begin_sent_or_received
        execute(tasks)
      case x:EndReceived =>
        if (!_end.sent) {
          queue {
            send(EndSession(), Tasks())
          }
        }
        end_sent_or_received
        execute(tasks)
      case x:EndSent =>
        end_sent_or_received
        execute(tasks)
      case _ =>
        incoming.receive(frame, tasks)
    }
  }

  def begin_sent_or_received = {
    if (_begin.sent && _begin.received && _begin.connected) {
      info("Begin frames exchanged")
      on_begin.foreach((x) => x.run)
      _begin.remove
    }
  }

  def end_sent_or_received = {
    if (_end.sent && _end.received) {
      info("End frames exchanged")
      on_end.foreach((x) => x.run)
      queue {
        send(ReleaseChain(), Tasks())
      }
    }
  }

  def attach(link: Link) {}

  def detach(link: Link) {}

  def detach(link: Link, reason: String) {}

  def detach(link: Link, t: Throwable) {}

  def sufficientSessionCredit() = false

  def getConnection = connection


}
