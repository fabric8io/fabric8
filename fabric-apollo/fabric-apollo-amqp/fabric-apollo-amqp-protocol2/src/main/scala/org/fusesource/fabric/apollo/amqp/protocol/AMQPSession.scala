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
import interfaces.{FrameInterceptor, Interceptor}
import org.fusesource.hawtdispatch._
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import org.fusesource.fabric.apollo.amqp.protocol.commands._
import Interceptor._
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPTransportFrame
import org.apache.activemq.apollo.util.Logging
import utilities.{fire_function, fire_runnable, execute, Tasks}

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

  var on_begin_received:Option[() => Unit] = None
  var on_end_received:Option[() => Unit] = None

  var _established = false

  before(new FrameInterceptor[ChainAttached] {
    override protected def receive_frame(c:ChainAttached, tasks:Queue[() => Unit]) = {
      trace("Session connected to connection")
      _established = true
      execute(tasks)
    }
  })

  before(new FrameInterceptor[ChainReleased] {
    override protected def receive_frame(c:ChainReleased, tasks:Queue[() => Unit]) = {
      trace("Session disconnected from connection")
      _established = false
      on_end_received = fire_function(on_end_received)
      on_end = fire_runnable(on_end)
      execute(tasks)
    }
  })

  trace("Constructed session chain : %s", display_chain(this))

  _links.interceptor_factory = Option((frame:AMQPTransportFrame) => {
    null.asInstanceOf[Interceptor]
  })

  def established() = _begin.sent && _begin.received && !_end.sent && !_end.received && _established

  def link_name_prefix = connection.getContainerID + "," + connection.getPeerContainerID + ","

  def begin(on_begin: Runnable) = if (_established) {
    this.on_begin = Option(on_begin)
    _begin.send_begin
  }

  def end() = if (_established) {
    _end.send(EndSession(), Tasks())
  }

  def end(t: Throwable) = if (_established) {
    _end.send(EndSession(t), Tasks())
  }

  def end(reason: String) = if (_established) {
    _end.send(EndSession(reason), Tasks())
  }

  override protected def _receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case t:AMQPTransportFrame =>
        incoming.receive(frame, tasks)
      case x:BeginReceived =>
        on_begin_received = fire_function(on_begin_received)
        begin_sent_or_received
        execute(tasks)
      case x:BeginSent =>
        begin_sent_or_received
        execute(tasks)
      case x:EndReceived =>
        on_end_received = fire_function(on_end_received)
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
    if (_begin.sent && _begin.received) {
      info("Begin frames exchanged")
      on_begin = fire_runnable(on_begin)
    }
  }

  def end_sent_or_received = {
    if (_end.sent && _end.received) {
      info("End frames exchanged")
      on_end = fire_runnable(on_end)
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
