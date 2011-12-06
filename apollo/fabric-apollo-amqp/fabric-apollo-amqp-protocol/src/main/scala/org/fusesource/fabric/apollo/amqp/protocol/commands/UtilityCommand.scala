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

/**
 *
 */

class UtilityCommand extends Command

object ReleaseChain {
  val INSTANCE = new ReleaseChain
  def apply() = INSTANCE
}
class ReleaseChain extends UtilityCommand

object ChainReleased {
  val INSTANCE = new ChainReleased
  def apply() = INSTANCE
}
class ChainReleased extends UtilityCommand

object ChainAttached {
  val INSTANCE = new ChainAttached
  def apply() = INSTANCE
}
class ChainAttached extends UtilityCommand


