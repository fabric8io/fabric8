/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.protocol

import org.fusesource.fusemq.amqp.codec.AmqpCommand
import org.fusesource.hawtdispatch.DispatchQueue

/**
 *
 */
trait SessionConnection {
  def getDispatchQueue: DispatchQueue
  def send(channel: Int, frame: AmqpCommand): Boolean
  def close(t:Throwable): Unit
  def close(reason:String): Unit
  def release(channel: Int): Unit
  def getContainerId: String
  def getPeerContainerId: String
}