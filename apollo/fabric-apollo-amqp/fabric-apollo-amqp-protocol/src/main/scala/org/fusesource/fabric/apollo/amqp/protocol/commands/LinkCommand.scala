/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.commands

import org.fusesource.hawtbuf.Buffer

/**
 *
 */
class LinkCommand extends Command

object AttachSent {
  private val INSTANCE = new AttachSent
  def apply() = INSTANCE
}
class AttachSent extends LinkCommand

object AttachReceived {
  private val INSTANCE = new AttachReceived
  def apply() = INSTANCE
}
class AttachReceived extends LinkCommand

object DetachSent {
  private val INSTANCE = new DetachSent
  def apply() = INSTANCE
}
class DetachSent extends LinkCommand

object DetachReceived {
  private val INSTANCE = new DetachReceived
  def apply() = INSTANCE
}
class DetachReceived extends LinkCommand






