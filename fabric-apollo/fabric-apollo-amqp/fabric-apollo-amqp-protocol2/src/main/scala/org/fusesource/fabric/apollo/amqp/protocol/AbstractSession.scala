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

/**
 *
 */

trait AbstractSession extends Session {

  var outgoing_window = 10L
  var incoming_window = 10L

  def setOutgoingWindow(window: Long) = outgoing_window = window

  def setIncomingWindow(window: Long) = incoming_window = window

  def getOutgoingWindow = outgoing_window

  def getIncomingWindow = incoming_window

  def attach(link: Link) {}

  def detach(link: Link) {}

  def detach(link: Link, reason: String) {}

  def detach(link: Link, t: Throwable) {}

  def sufficientSessionCredit() = false

  def begin(onBegin: Runnable) {}

  def getConnection = null

  def established() = false

  def setLinkHandler(handler: LinkHandler) {}

  def end(onEnd: Runnable) {}

  def end(t: Throwable) {}

  def end(reason: String) {}

}