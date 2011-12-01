/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.common

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.commands.{ReleaseChain, CloseConnection}
import org.fusesource.fabric.apollo.amqp.protocol.utilities.execute
import org.fusesource.fabric.apollo.amqp.codec.types.{Close, AMQPTransportFrame}

/**
 *
 */
class OutgoingConnector(target:Multiplexer, set_outgoing_channel:(Int, AMQPTransportFrame) => Unit) extends Interceptor {

  private var _local_channel:Option[Int] = None
  private var _remote_channel:Option[Int] = None

  def release = {
    val rc = (_local_channel, _remote_channel)
    _local_channel = None
    _remote_channel = None
    rc
  }

  override protected def _send(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case t:AMQPTransportFrame =>
        if (!t.getPerformative.isInstanceOf[Close]) {
          set_outgoing_channel(local_channel, t)
        }
        target.send(frame, tasks)
      case c:CloseConnection =>
        target.send(frame, tasks)
      case r:ReleaseChain =>
        target.release(this)
        execute(tasks)
      case _ =>
        execute(tasks)
    }
  }

  def local_channel = _local_channel.getOrElse(throw new RuntimeException("No local channel set on connector"))

  def local_channel_=(channel:Int) = _local_channel = Option(channel)

  def remote_channel = _remote_channel.getOrElse(throw new RuntimeException("No remote channel set on connector"))

  def remote_channel_=(channel:Int) = _remote_channel = Option(channel)

  override def toString = String.format("%s{local_channel=%s remote_channel=%s", getClass.getSimpleName, _local_channel, _remote_channel)
}

