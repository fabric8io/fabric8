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
import org.fusesource.hawtdispatch._

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

object OptionalTask {
  def apply(args:Option[() => Unit]*) = {
    val rc = new Queue[() => Unit]
    args.foreach((x) => x.foreach((x) => rc.enqueue(x)))
    rc
  }
}

object execute {
  def apply(q:Queue[() => Unit]) = {
    if (!q.isEmpty) {
      Option(Dispatch.getCurrentQueue) match {
        case Some(queue) =>
          q.dequeueAll((x) => {
            queue {
              x()
            }
            true
          })
        case None =>
          q.dequeueAll((x) => {x(); true})
      }
    }
  }
}