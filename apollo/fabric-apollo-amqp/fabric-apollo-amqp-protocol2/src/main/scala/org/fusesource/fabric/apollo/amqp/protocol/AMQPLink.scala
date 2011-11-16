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
import org.fusesource.fabric.apollo.amqp.codec.interfaces.{Source, Target}
import utilities.link.LinkFlowControlTracker

/**
 *
 */

trait AMQPLink extends Link {

  val tracker = new LinkFlowControlTracker(getRole)

  var name:Option[String] = None
  var max_message_size:Long = 0L

  var source:Option[Source] = None
  var target:Option[Target] = None

  var on_attach:Option[Runnable] = None
  var on_detach:Option[Runnable] = None

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