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
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.{HashMap, Queue}
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Slot
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPTransportFrame


class UnknownFrameType extends Exception {

}
/**
 *
 */
class Multiplexer extends Interceptor with Logging {

  val interceptors = new Slot[Interceptor]
  val channels = new HashMap[Int, Int]

  var channel_selector:Option[(AMQPFrame) => Int] = None
  var channel_mapper:Option[(AMQPFrame) => Option[Int]] = None
  var interceptor_factory:Option[(AMQPFrame) => Interceptor] = None

  var chain_attached:Option[(Interceptor) => Unit] = None
  var chain_released:Option[(Interceptor) => Unit] = None

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    outgoing.send(frame, tasks)
  }

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case t:AMQPTransportFrame =>
        try {
          map_channel(frame, tasks)
        } catch {
          case e:UnknownFrameType =>
            debug("Selector doesn't recognize frame, dropping %s", frame)
            tasks.dequeueAll((x) => {x(); true})
        }

      // TODO send ConnectionClosed frames down all chains when that happens
      case _ =>
        debug("Dropping frame %s", frame)
        tasks.dequeueAll((x) => {x(); true})
    }
  }

  def release(chain:Interceptor):Interceptor = {
    chain match {
      case o:OutgoingConnector =>
        val (local, remote) = o.release
        interceptors.free(local)
        channels.remove(remote)
        chain_released.foreach((x) => x(o))
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
        val o = new OutgoingConnector(this)
        o.incoming = chain
        o
    }
    val to = interceptors.allocate(temp)
    temp.local_channel = to
    temp
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

  private def create(frame:AMQPFrame, from:Int):OutgoingConnector = {
    val interceptor = attach(factory(frame)).asInstanceOf[OutgoingConnector]
    val to = interceptor.local_channel
    interceptor.remote_channel = from
    channels.put(from, to)
    chain_attached.foreach((x) => x(interceptor))
    interceptor
  }

  private def map_channel(frame:AMQPFrame, tasks:Queue[() => Unit]) = {
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
        interceptor.receive(frame, tasks)
      case None =>
        create(frame, from).receive(frame, tasks)
    }
  }
}