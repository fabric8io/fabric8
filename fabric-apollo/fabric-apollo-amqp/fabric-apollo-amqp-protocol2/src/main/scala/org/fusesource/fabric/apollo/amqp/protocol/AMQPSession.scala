/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 * 	http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.protocol

import org.fusesource.fabric.apollo.amqp.api._

class AMQPSession(val connection:ProtocolConnection) extends ProtocolSession {

  var local_channel:Option[Int] = None

  var remote_channel:Option[Int] = None

  def setOutgoingWindow(window: Long) {}

  def setIncomingWindow(window: Long) {}

  def getOutgoingWindow = 0L

  def getIncomingWindow = 0L

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

  def setLocalChannel(channel: Int) = local_channel = Option(channel)

  def setRemoteChannel(channel: Int) = remote_channel = Option(channel)

  def getLocalChannel = local_channel

  def getRemoteChannel = remote_channel

}
