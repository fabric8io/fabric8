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

import interfaces.{ProtocolConnection, ProtocolSession}
import org.fusesource.fabric.apollo.amqp.protocol.api._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.hawtdispatch._

class AMQPSession extends Interceptor with AbstractSession {

  tail.incoming = _begin
  tail.incoming = _end
  tail.incoming = _flow

  setOutgoingWindow(10L)
  setIncomingWindow(10L)

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

  def end() {}

  def end(t: Throwable) {}

  def end(reason: String) {}

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = outgoing.send(frame, tasks)

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = incoming.receive(frame, tasks)

}
