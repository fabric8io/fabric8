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

import api.{LinkHandler, Link, Session}
import interceptors.common.Multiplexer
import interceptors.session.{SessionFlowControlInterceptor, EndInterceptor, BeginInterceptor}

/**
 *
 */

trait AbstractSession extends Session {

  val _begin = new BeginInterceptor

  _begin.set_outgoing_window = Option(() => getOutgoingWindow.asInstanceOf[Long])
  _begin.set_incoming_window = Option(() => getIncomingWindow.asInstanceOf[Long])

  val _end = new EndInterceptor
  val _flow = new SessionFlowControlInterceptor
  val _links = new Multiplexer

  var on_begin:Option[Runnable] = None
  var on_end:Option[Runnable] = None

  def setOnEnd(on_end:Runnable) = this.on_end = Option(on_end)

  def setOutgoingWindow(window: Long) = _flow.flow.setOutgoingWindow(window)

  def setIncomingWindow(window: Long) = _flow.flow.setIncomingWindow(window)

  def getOutgoingWindow = _flow.flow.getOutgoingWindow

  def getIncomingWindow = _flow.flow.getIncomingWindow

  def setLinkHandler(handler: LinkHandler) {}


}