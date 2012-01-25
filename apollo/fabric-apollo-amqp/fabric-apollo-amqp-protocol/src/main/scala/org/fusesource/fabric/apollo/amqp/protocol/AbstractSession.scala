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