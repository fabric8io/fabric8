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

import api.{Session, Link}
import commands._
import interceptors.link.{AttachInterceptor, DetachInterceptor}
import interfaces.{FrameInterceptor, Interceptor}
import org.fusesource.fabric.apollo.amqp.codec.interfaces.{Source, Target}
import utilities.execute._
import utilities.fire_function._
import utilities.fire_runnable._
import utilities.link.LinkFlowControlTracker
import org.fusesource.fabric.apollo.amqp.codec.types.Attach
import collection.mutable.Queue
import org.apache.activemq.apollo.util.Logging
import utilities.{fire_runnable, execute}

object AMQPLink {
  def initialize(link:AMQPLink, attach:Attach) = {
    link.setName(attach.getName)
    Option(attach.getTarget) match {
      case Some(t) =>
        link.setTarget(t.asInstanceOf[Target])
      case None =>
    }
    Option(attach.getSource) match {
      case Some(s) =>
        link.setSource(s.asInstanceOf[Source])
      case None =>
    }
    Option(attach.getMaxMessageSize) match {
      case Some(m) =>
        link.setMaxMessageSize(m.longValue)
      case None =>
    }
    link

  }
}
/**
 *
 */

trait AMQPLink extends FrameInterceptor[LinkCommand] with Link with Logging {

  val tracker = new LinkFlowControlTracker(getRole)
  val _attach = new AttachInterceptor
  val _detach = new DetachInterceptor

  var name:Option[String] = None
  var max_message_size:Long = 0L

  var source:Option[Source] = None
  var target:Option[Target] = None

  var on_attach:Option[Runnable] = None
  var on_detach:Option[Runnable] = None

  var _established = false

  val attach_detector = new FrameInterceptor[ChainAttached] {
    override protected def receive_frame(c:ChainAttached, tasks:Queue[() => Unit]) = {
      trace("Link attached to session")
      _established = true
      _attach.send_attach
      execute(tasks)
    }
  }

  val released_detector = new FrameInterceptor[ChainReleased] {
    override protected def receive_frame(c:ChainReleased, tasks:Queue[() => Unit]) = {
      trace("Link detached from session")
      _established = false
      execute(tasks)
    }
  }
  head.outgoing = _detach
  head.outgoing = _attach

  before(attach_detector)
  before(released_detector)
  
  _attach.configure_attach = Option(configure_attach)

  var session:Option[Session] = None

  def established() = _attach.sent && _attach.received && !_detach.sent && !_detach.received && _established
  
  def configure_attach(attach:Attach):Attach = {
    attach.setName(getName)
    attach.setSource(getSource)
    attach.setTarget(getTarget)
    attach.setRole(getRole.getValue)
    attach
  }

  def getName = name.getOrElse(throw new RuntimeException("No name has been set for this link"))

  def setName(name: String) = this.name = Option(name)

  def setMaxMessageSize(size: Long) = max_message_size = size

  def getMaxMessageSize = max_message_size

  def setSource(source: Source) = this.source = Option(source)

  def setTarget(target: Target) = this.target = Option(target)

  def getSource = source.getOrElse(throw new RuntimeException("No source has been specified for this link"))

  def getTarget = target.getOrElse(throw new RuntimeException("No target has been specified for this link"))

  def onAttach(task: Runnable) = on_attach = Option(task)

  def onDetach(task: Runnable) = on_detach = Option(task)

  def getSession = session.getOrElse(throw new RuntimeException("This link is not currently attached to a session"))
  
  def attach_sent_or_received = {
    if (_attach.sent && _attach.received) {
      info("Attach frames exchanged")
      fire_runnable(on_attach)
    }
  }

  def detach_sent_or_received = {
    if (_detach.sent && _detach.received) {
      info("Detach frames exchanged")
      fire_runnable(on_detach)
    }

  }

  override protected def receive_frame(frame: LinkCommand, tasks: Queue[() => Unit]) {
    frame match {
      case x:AttachReceived =>
        attach_sent_or_received
        execute(tasks)
      case x:AttachSent =>
        attach_sent_or_received
        execute(tasks)
      case x:DetachReceived =>
        execute(tasks)
      case x:DetachSent =>
        execute(tasks)
    }

  }

}