/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.amqp.protocol.interfaces

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.hawtdispatch.DispatchQueue
import org.apache.activemq.apollo.util.Log
import apple.laf.JRSUIConstants.Direction
import org.fusesource.fabric.apollo.amqp.protocol.utilities.sanitize


object Interceptor {
  def display_chain(in:Interceptor):String = {
    var rc = ""
    in.foreach((in) => {
      rc = rc + "=>{" + in + "}"
    })
    rc.substring(2)
  }
}

trait Interceptor {
  import Interceptor._

  val logger:Log = Log(getClass.getName.stripSuffix("$"))

  private var _queue:Option[DispatchQueue] = None

  private var _outgoing:Option[Interceptor] = None
  private var _incoming:Option[Interceptor] = None

  val rm = () => remove

  def outgoing = _outgoing.getOrElse(throw new RuntimeException("No outgoing interceptor exists at this end of chain"))
  def incoming = _incoming.getOrElse(throw new RuntimeException("No incoming interceptor exists at this end of chain"))

  def queue = {
    _queue match {
      case Some(queue) =>
        queue
      case None =>
        throw new RuntimeException("No queue set for this interceptor chain : " + display_chain(this))
    }
  }

  def queue_=(q:DispatchQueue) = head.foreach((x) => x._queue = Option(q))
  final def queue_set:Boolean = !_queue.isEmpty

  final def remove:Unit = {
    _outgoing match {
      case Some(out) =>
        _incoming match {
          case Some(in) =>
            in._outgoing = Option(out)
          case None =>
            out._incoming = None
        }
      case None =>

    }
    _incoming match {
      case Some(in) =>
        _outgoing match {
          case Some(out) =>
            out._incoming = Option(in)
          case None =>
            in._outgoing = None
        }
      case None =>
    }
    removing_from_chain
    _queue = None
    _outgoing = None
    _incoming = None
  }

  final def connected:Boolean = !(_incoming == None && _outgoing == None)

  final def outgoing_=(i:Interceptor):Unit = {
    if (i != null) {
      i.foreach_reverse((x) => x._queue = _queue)
      i.tail._incoming = Option(this)
      if (logger.log.isTraceEnabled) {
        logger.trace("%s<==%s", i, this)
      }
    }
    _outgoing = Option(i)
    _outgoing.foreach((x) => x.adding_to_chain)
  }

  final def incoming_=(i:Interceptor):Unit = {
    if (i != null) {
      i.foreach((x) => x._queue = _queue)
      i.head._outgoing = Option(this)
      if (logger.log.isTraceEnabled) {
        logger.trace("%s==>%s", this, i)
      }
    }
    _incoming = Option(i)
    _incoming.foreach((x) => x.adding_to_chain)
  }

  final def after(i:Interceptor):Unit = {
    _incoming match {
      case Some(interceptor) =>
        interceptor.outgoing = i
        this.incoming = i
      case None =>
        this.incoming = i
    }
  }

  final def before(i:Interceptor):Unit = {
    _outgoing match {
      case Some(interceptor) =>
        interceptor.incoming = i
        this.outgoing = i
      case None =>
        this.outgoing = i
    }
  }

  final def tail:Interceptor = {
    if (!connected || _incoming == None) {
      this
    } else {
      incoming.tail
    }
  }

  final def head:Interceptor = {
    if (!connected || _outgoing == None) {
      this
    } else {
      outgoing.head
    }
  }

  final def foreach_reverse(func:Interceptor => Unit) = {
    var in = Option[Interceptor](tail)
    while (in != None) {
      func(in.get)
      in = in.get._outgoing
    }
  }

  final def foreach(func:Interceptor => Unit) = {
    var in = Option[Interceptor](head)
    while (in != None) {
      func(in.get)
      in = in.get._incoming
    }
  }

  override def toString = getClass.getSimpleName

  private def log_frame(frame:AMQPFrame, tasks:Queue[() => Unit], prefix:String) = {
    logger.trace("%s(frame=%s, tasks=%s)", prefix, frame, tasks)

  }

  final def send(frame:AMQPFrame, tasks:Queue[() => Unit]):Unit = {
    if (logger.log.isTraceEnabled) {
      log_frame(frame, tasks, "send")
    }
    _send(frame, tasks)
  }

  final def receive(frame:AMQPFrame, tasks:Queue[() => Unit]):Unit = {
    if (logger.log.isTraceEnabled) {
      log_frame(frame, tasks, "receive")
    }
    _receive(frame, tasks)
  }

  protected def adding_to_chain = {}

  protected def removing_from_chain = {}

  protected def _send(frame:AMQPFrame, tasks:Queue[() => Unit]) = outgoing.send(frame, tasks)

  protected def _receive(frame:AMQPFrame, tasks:Queue[() => Unit]) = incoming.receive(frame, tasks)

}
