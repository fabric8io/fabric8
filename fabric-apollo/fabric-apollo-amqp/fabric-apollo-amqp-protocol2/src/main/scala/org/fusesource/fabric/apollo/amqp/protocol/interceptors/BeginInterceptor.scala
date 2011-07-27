/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.apache.activemq.apollo.util.Logging
import java.util.concurrent.atomic.AtomicBoolean
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue

/**
 *
 */

class BeginInterceptor extends Interceptor with Logging {

  val sent = new AtomicBoolean(false)

  var onBegin:Option[() => Unit] = None

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = null

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = null
}