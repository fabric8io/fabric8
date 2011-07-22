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

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame

/**
 *
 */
object CloseConnection {
  val INSTANCE = new CloseConnection

  def apply() = INSTANCE

  def apply(reason:String) = {
    val rc = new CloseConnection
    rc.reason = Option(reason)
    rc
  }

  def apply(reason:Throwable) = {
    val rc = new CloseConnection
    rc.exception = Option(reason)
    rc
  }
}
class CloseConnection extends AMQPFrame {

  var reason:Option[String] = None
  var exception:Option[Throwable] = None

  override def toString = getClass.getSimpleName

}