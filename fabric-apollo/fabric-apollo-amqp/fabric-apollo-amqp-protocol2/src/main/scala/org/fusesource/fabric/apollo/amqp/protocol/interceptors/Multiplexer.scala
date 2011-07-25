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

import org.fusesource.hawtdispatch._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.{HashMap, Queue}
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.SessionFactory
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Slot
import org.fusesource.fabric.apollo.amqp.codec.types.{Begin, AMQPTransportFrame}
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.protocol.DefaultSessionFactory
import org.fusesource.fabric.apollo.amqp.protocol.api.{SessionHandler, Session}

/**
 *
 */
class Multiplexer extends Interceptor with Logging {

  val interceptors = new Slot[Interceptor]
  val channels = new HashMap[Int, Int]
  
  var channel_selector:Option[(AMQPFrame) => Int] = None
  var interceptor_factory:Option[(AMQPFrame) => Interceptor] = None
  
  
  def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    outgoing.send(frame, tasks)
  }

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    map_channel(frame, tasks)
  }
  
  def release(chain:Interceptor) = {
    chain match {
      case o:OutgoingConnector =>
        val (local, remote) = o.release
        interceptors.free(local)
        channels.remove(remote)        
      case _ =>
        throw new IllegalArgumentException("Invalid type (" + chain.getClass.getSimpleName + ") passed to release")        
    }
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
    val interceptor = new OutgoingConnector(this)
    interceptor.incoming = factory(frame)
    val to = interceptors.allocate(interceptor)
    interceptor.local_channel = to
    interceptor.remote_channel = from
    channels.put(from, to)
    interceptor    
  }  
  
  private def map_channel(frame:AMQPFrame, tasks:Queue[() => Unit]) = {
    val from = selector(frame)
    val to = channels.get(from) match {
      case Some(to) =>
        to
      case None =>
        create(frame, from).local_channel
    }
    interceptors.get(to) match {
      case Some(interceptor) =>
        interceptor.receive(frame, tasks)
      case None =>
        create(frame, from).receive(frame, tasks)
    }    
  }  
}