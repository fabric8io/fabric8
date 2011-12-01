package org.fusesource.fabric.apollo.amqp.protocol.interceptors.link

/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.types.Detach
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.PerformativeInterceptor
import org.fusesource.hawtbuf.Buffer
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.utilities.execute

/**
 *
 */
class DetachInterceptor extends PerformativeInterceptor[Detach] with Logging {

  var sent = false
  var received = false

  override protected def receive(performative: Detach, payload: Buffer, tasks: Queue[() => Unit]) = {
    execute(tasks)
    true
  }

}