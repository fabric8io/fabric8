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

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.common

import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.{HashMap, Queue}
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPTransportFrame
import org.fusesource.hawtdispatch.DispatchQueue
import org.fusesource.fabric.apollo.amqp.protocol.commands.{ChainAttached, ChainReleased}
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{Tasks, execute, Slot}
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.{FrameInterceptor, Interceptor}

/**
 *
 */
class Multiplexer extends FrameInterceptor[AMQPTransportFrame] with Logging {

  val interceptors = new Slot[Interceptor]
  val channels = new HashMap[Int, Int]

  var channel_selector:Option[(AMQPTransportFrame) => Int] = None
  var channel_mapper:Option[(AMQPTransportFrame) => Option[Int]] = None
  var interceptor_factory:Option[(AMQPTransportFrame) => Interceptor] = None
  var outgoing_channel_setter:Option[(Int, AMQPTransportFrame) => Unit] = None

  var chain_attached:Option[(Interceptor) => Unit] = None
  var chain_released:Option[(Interceptor) => Unit] = None

  override protected def receive_frame(frame:AMQPTransportFrame, tasks: Queue[() => Unit]) = map_channel(frame, tasks)

  def foreach_chain(func:(Interceptor) => Unit) = interceptors.foreach((x) => func(x))

  override def queue_=(q:DispatchQueue) = {
    super.queue_=(q)
    foreach_chain((x) => x.queue = q)
  }

  def release(chain:Interceptor):Interceptor = {
    chain match {
      case o:OutgoingConnector =>
        val (local, remote) = o.release
        local.foreach((x) => interceptors.free(x))
        remote.foreach((x) => channels.remove(x))
        chain_released.foreach((x) => x(o))
        o.incoming.receive(ChainReleased(), Tasks())
        o.incoming
      case _ =>
        throw new IllegalArgumentException("Invalid type (" + chain.getClass.getSimpleName + ") passed to release")
    }
  }

  def attach(chain:Interceptor):Interceptor = {
    val temp = chain match {
      case o:OutgoingConnector =>
        o
      case _ =>
        val o = new OutgoingConnector(this, outgoing_channel)
        o.incoming = chain
        o
    }
    val to = interceptors.allocate(temp)
    temp.local_channel = to
    if (queue_set) {
      temp.queue = queue
    }
    temp.head.receive(ChainAttached(), Tasks())
    temp
  }

  private def outgoing_channel = outgoing_channel_setter match {
    case Some(setter) =>
      setter
    case None =>
      throw new RuntimeException("Outgoing channel setter not set on multiplexer")
  }

  private def mapper = channel_mapper match {
    case Some(mapper) =>
      mapper
    case None =>
      throw new RuntimeException("Channel mapper not set on multiplexer")
  }

  private def selector = channel_selector match {
      case Some(selector) =>
        selector
      case None =>
        throw new RuntimeException("Channel selector not set on multiplexer")
  }

  private def factory = interceptor_factory match {
    case Some(factory) =>
      factory
    case None =>
      throw new RuntimeException("Factory not set on multiplexer")
  }

  private def create(frame:AMQPTransportFrame, from:Int):OutgoingConnector = {
    val interceptor = attach(factory(frame)).asInstanceOf[OutgoingConnector]
    val to = interceptor.local_channel
    interceptor.remote_channel = from
    channels.put(from, to)
    chain_attached.foreach((x) => x(interceptor))
    trace("Created local channel %s for remote channel %s", to, from)
    interceptor
  }

  private def map_channel(frame:AMQPTransportFrame, tasks:Queue[() => Unit]) = {
    val from = selector(frame)
    val to = channels.get(from) match {
      case Some(to) =>
        to
      case None =>
        mapper(frame) match {
          case Some(x) =>
            channels.put(from, x)
            interceptors.get(x) match {
              case Some(i) =>
                i.asInstanceOf[OutgoingConnector].remote_channel = from
                chain_attached.foreach((x) => x(i))
              case None =>
                throw new RuntimeException("No local slot allocated for channel " + x)
            }
            x
          case None =>
            create(frame, from).local_channel
        }
    }
    interceptors.get(to) match {
      case Some(interceptor) =>
        trace("Mapping incoming channel %s to local channel %s", from, to)
        interceptor.receive(frame, tasks)
      case None =>
        create(frame, from).receive(frame, tasks)
    }
  }
}