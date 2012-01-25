/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.protocol

import api.{Connection, Link}
import interfaces.{FrameInterceptor, Interceptor}
import org.fusesource.hawtdispatch._
import org.fusesource.fabric.apollo.amqp.protocol.commands._
import Interceptor._
import org.apache.activemq.apollo.util.Logging
import utilities.{fire_function, fire_runnable, execute, Tasks}
import org.fusesource.fabric.apollo.amqp.codec.types._
import collection.mutable.{HashMap, Queue}

/**
 *
 */
class AMQPSession extends FrameInterceptor[SessionCommand] with AbstractSession with Logging {

  var connection:Connection = null

  head.outgoing = _end
  head.outgoing = _begin

  tail.incoming = _flow
  tail.incoming = _links

  setOutgoingWindow(10L)
  setIncomingWindow(10L)

  var on_begin_received:Option[() => Unit] = None
  var on_end_received:Option[() => Unit] = None

  var _established = false

  val attach_detector = new FrameInterceptor[ChainAttached] {
    override protected def receive_frame(c:ChainAttached, tasks:Queue[() => Unit]) = {
      trace("Session connected to connection")
      _established = true
      execute(tasks)
    }
  }

  val released_detector = new FrameInterceptor[ChainReleased] {
    override protected def receive_frame(c:ChainReleased, tasks:Queue[() => Unit]) = {
      trace("Session disconnected from connection")
      _established = false
      on_end_received = fire_function(on_end_received)
      on_end = fire_runnable(on_end)
      execute(tasks)
    }
  }

  before(attach_detector)
  before(released_detector)

  trace("Constructed session chain : %s", display_chain(this))

  val link_map = new HashMap[String, Interceptor]()

  _links.interceptor_factory = Option((frame:AMQPTransportFrame) => {
    frame.getPerformative match {
      case a:Attach =>
        if (a.getRole == Role.SENDER.getValue) {
          AMQPReceiver.create(a).head
        } else {
          AMQPSender.create(a).head
        }
      case _ =>
        throw new RuntimeException("Incoming frame for non-existant link : {" + frame + "}")

    }
  })
  _links.outgoing_channel_setter = Option((channel:Int, frame:AMQPTransportFrame) => {
    frame.getPerformative match {
      case a:Attach =>
        a.setHandle(channel)
      case d:Detach =>
        d.setHandle(channel)
      case f:Flow =>
        f.setHandle(channel)
      case t:Transfer =>
        t.setHandle(channel)
    }
  })
  
  _links.channel_mapper = Option((frame:AMQPTransportFrame) => {None})

  _links.channel_selector = Option((frame:AMQPTransportFrame) => {
    frame.getPerformative match {
      case a:Attach =>
        a.getHandle.intValue()
      case d:Detach =>
        d.getHandle.intValue()
      case f:Flow =>
        f.getHandle.intValue()
      case t:Transfer =>
        t.getHandle.intValue()
    }
  })

  override protected def removing_from_chain = {
    attach_detector.remove
    released_detector.remove
  }

  def established() = _begin.sent && _begin.received && !_end.sent && !_end.received && _established

  def begin(on_begin: Runnable) = if (_established) {
    this.on_begin = Option(on_begin)
    send(BeginSession(), Tasks())
  }

  def end() = if (_established) {
    send(EndSession(), Tasks())
  }

  def end(t: Throwable) = if (_established) {
    send(EndSession(t), Tasks())
  }

  def end(reason: String) = if (_established) {
    send(EndSession(reason), Tasks())
  }

  override protected def receive_frame(frame:SessionCommand, tasks: Queue[() => Unit]) = {
    frame match {
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

  private def global_link_name(link:Link) = {
    if (link.getRole == Role.SENDER.getValue) {
      getConnection.getContainerID + "," + getConnection.getPeerContainerID + "," + link.getName
    } else {
      getConnection.getPeerContainerID + "," + getConnection.getContainerID + "," + link.getName
    }
  }

  def attach(link: Link) {
    require(link.isInstanceOf[Interceptor], "Unexpected link type")
  }

  def detach(link: Link) {
    require(link.isInstanceOf[Interceptor], "Unexpected link type")
  }

  def detach(link: Link, reason: String) {
    require(link.isInstanceOf[Interceptor], "Unexpected link type")
  }

  def detach(link: Link, t: Throwable) {
    require(link.isInstanceOf[Interceptor], "Unexpected link type")
  }

  def sufficientSessionCredit() = false

  def getConnection = connection


}
