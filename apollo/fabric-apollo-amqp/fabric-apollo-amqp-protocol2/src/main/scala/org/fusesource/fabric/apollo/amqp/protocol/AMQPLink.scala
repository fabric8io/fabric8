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

import api.{Session, Link}
import commands.{ChainReleased, ChainAttached}
import interceptors.link.{AttachInterceptor, DetachInterceptor}
import interfaces.{FrameInterceptor, Interceptor}
import org.fusesource.fabric.apollo.amqp.codec.interfaces.{Source, Target}
import utilities.execute
import utilities.execute._
import utilities.fire_function._
import utilities.fire_runnable._
import utilities.link.LinkFlowControlTracker
import org.fusesource.fabric.apollo.amqp.codec.types.Attach
import collection.mutable.Queue
import org.apache.activemq.apollo.util.Logging

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

trait AMQPLink extends Interceptor with Link with Logging {

  val tracker = new LinkFlowControlTracker(getRole)
  val _attach = new AttachInterceptor
  val _detach = new DetachInterceptor

  var name:Option[String] = None
  var max_message_size:Long = 0L

  var source:Option[Source] = None
  var target:Option[Target] = None

  var on_attach:Option[Runnable] = None
  var on_detach:Option[Runnable] = None

  val attach_detector = new FrameInterceptor[ChainAttached] {
    override protected def receive_frame(c:ChainAttached, tasks:Queue[() => Unit]) = {
      trace("Link attached to session")
      execute(tasks)
    }
  }

  val released_detector = new FrameInterceptor[ChainReleased] {
    override protected def receive_frame(c:ChainReleased, tasks:Queue[() => Unit]) = {
      trace("Link detached from session")
      execute(tasks)
    }
  }
  head.outgoing = _detach
  head.outgoing = _attach

  before(attach_detector)
  before(released_detector)

  var session:Option[Session] = None

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
}