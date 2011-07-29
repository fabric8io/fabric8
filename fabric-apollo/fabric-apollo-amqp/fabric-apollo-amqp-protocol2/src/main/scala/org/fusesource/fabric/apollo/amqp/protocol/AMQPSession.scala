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

import org.fusesource.hawtdispatch._
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import org.fusesource.fabric.apollo.amqp.protocol.commands._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import Interceptor._
import collection.mutable.Queue
import utilities.Tasks
import org.apache.activemq.apollo.util.Logging

/**
 *
 */
class AMQPSession extends Interceptor with AbstractSession with Logging {

  tail.incoming = _begin
  tail.incoming = _end
  tail.incoming = _flow

  setOutgoingWindow(10L)
  setIncomingWindow(10L)

  trace("Constructed session chain : %s", display_chain(this))

  def begin(onBegin: Runnable) = {
    Option(onBegin) match {
      case Some(x) =>
        _begin.on_begin = Option(() => x.run())
      case None =>
        _begin.on_begin = None
    }
    queue {
      _begin.send_begin
    }
  }

  def end() = _end.send(EndSession(), Tasks())

  def end(t: Throwable) = _end.send(EndSession(t), Tasks())

  def end(reason: String) = _end.send(EndSession(reason), Tasks())

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = outgoing.send(frame, tasks)

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = incoming.receive(frame, tasks)

}
