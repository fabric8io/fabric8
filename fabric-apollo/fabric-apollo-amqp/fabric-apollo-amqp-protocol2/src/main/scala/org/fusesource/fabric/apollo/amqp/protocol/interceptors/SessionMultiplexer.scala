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
import org.fusesource.fabric.apollo.amqp.codec.types.{Begin, AMQPTransportFrame}
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.protocol.DefaultSessionFactory
import org.fusesource.fabric.apollo.amqp.protocol.api.{SessionHandler, Session}

/**
 *
 */
class SessionMultiplexer extends Interceptor with Logging {

  val sessions = new Slot[Interceptor]
  val channels = new HashMap[Int, Int]
  var session_factory = new DefaultSessionFactory
  var session_handler: Option[SessionHandler] = None

  def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    outgoing.send(frame, tasks)
  }

  def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    frame match {
      case t:AMQPTransportFrame => {
        val channel = t.getChannel()
        channels.get(channel) match {
          case Some(local) =>
            sessions.get(local) match {
              case Some(x) =>
                x.receive(frame, tasks)
              case None =>
            }
          case None =>
        }

      }
      case _ =>
    }
  }

  def session(remote: Boolean, remote_channel: Int): Session = {
    val session = session_factory.create_session(this)
    val local_channel = sessions.allocate(session)
    session.setLocalChannel(local_channel)

    if ( remote ) {
      session.setRemoteChannel(remote_channel)
      channels.put(remote_channel, local_channel)
    }

    session_handler match {
      case Some(x) =>
        x.sessionCreated(this, session)
      case None =>
        if ( remote ) {
          session.begin(^ {
            session.end("Session rejected")
          })
        }
    }
    session
  }

  def session_from_remote_channel(channel:Int) = {
    channels.get(channel) match {
      case Some(channel) =>
        sessions.get(channel)
      case None =>
        None
    }
  }

  def get_session(channel:Int, command:AnyRef):Option[Interceptor] = {
    command match {
      case b:Begin =>
        Option(b.getRemoteChannel) match {
          case Some(local_channel) =>
            channels.put(channel, local_channel.intValue)
            trace("Received response to begin frame sent from local_channel=%s from remote_channel=%s", local_channel.intValue, channel)
            session_from_remote_channel(channel) match {
              case Some(session) =>
                Option(session)
              case None =>
                None
            }
          case None =>
            val s = session(true, channel)
            trace("Created session from remote begin request %s", s)
            session_from_remote_channel(channel)
        }
      case _ =>
        session_from_remote_channel(channel)
    }
  }


}