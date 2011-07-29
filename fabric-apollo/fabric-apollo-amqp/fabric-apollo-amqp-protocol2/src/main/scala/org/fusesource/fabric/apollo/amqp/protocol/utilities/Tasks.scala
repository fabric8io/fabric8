/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.utilities

import collection.mutable.Queue

/**
 *
 */

object Tasks {
  def apply() = new Queue[() => Unit]

  def apply(args:(() => Unit)*) = {
    val rc = new Queue[() => Unit]
    args.foreach((x) => rc.enqueue(x))
    rc
  }
}

object Execute {
  def apply(q:Queue[() => Unit]) = q.dequeueAll((x) => {x(); true})
}