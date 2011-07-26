package org.fusesource.fabric.apollo.amqp.protocol.interceptors

/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.{HashMap, Queue}
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Slot
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPTransportFrame

/**
 *
 */

class OutgoingConnector(target:Interceptor) extends Interceptor {

  private var _local_channel:Option[Int] = None
  private var _remote_channel:Option[Int] = None

  def release = {
    val rc = (local_channel, remote_channel)
    _local_channel = None
    _remote_channel = None
    rc
  }

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = target.send(frame, tasks)

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = incoming.receive(frame, tasks)

  def local_channel = _local_channel.getOrElse(throw new RuntimeException("No local channel set on connector"))

  def local_channel_=(channel:Int) = _local_channel = Option(channel)

  def remote_channel = _remote_channel.getOrElse(throw new RuntimeException("No remote channel set on connector"))

  def remote_channel_=(channel:Int) = _remote_channel = Option(channel)

  override def toString = String.format("%s{local_channel=%s remote_channel=%s", getClass.getSimpleName, _local_channel, _remote_channel)
}

