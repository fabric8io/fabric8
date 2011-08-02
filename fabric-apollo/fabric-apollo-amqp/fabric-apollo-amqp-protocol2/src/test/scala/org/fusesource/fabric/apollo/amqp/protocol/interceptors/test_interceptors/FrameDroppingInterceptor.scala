/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.protocol.utilities.execute

/**
 *
 */

class FrameDroppingInterceptor extends Interceptor with Logging {
  override protected def _send(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    info("Dropping frame %s", frame)
    execute(tasks)
  }

  override protected def _receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    info("Dropping frame %s", frame)
    execute(tasks)
  }
}