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

import org.fusesource.fabric.apollo.amqp.codec.types.AMQPTransportFrame
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame

/**
 *
 */
object sanitize {
  def apply(frame:AMQPFrame):String = {
    frame match {
      case t:AMQPTransportFrame =>
        t.getPerformative.toString
      case _ =>
        frame.toString
    }
  }
}

object fire_runnable {
  def apply(runnable:Option[Runnable]):Option[Runnable] = {
    runnable.foreach((x) => x.run())
    None
  }

}

object fire_function {
  def apply(func:Option[() => Unit]):Option[() => Unit] = {
    func.foreach((x) => x())
    None
  }
}