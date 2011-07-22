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
  def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = target.send(frame, tasks)

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = incoming.receive(frame, tasks)
}

