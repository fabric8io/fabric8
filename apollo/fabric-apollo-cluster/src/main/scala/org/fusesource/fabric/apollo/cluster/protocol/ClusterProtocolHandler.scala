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

package org.fusesource.fabric.apollo.cluster.protocol

import ClusterProtocolConstants._
import java.lang.String
import java.io.IOException
import org.apache.activemq.apollo.util._
import org.fusesource.fabric.apollo.cluster.model._
import org.apache.activemq.apollo.broker.protocol.ProtocolHandler
import org.fusesource.fabric.apollo.cluster.{ClusterConnector, Peer}
import org.fusesource.hawtdispatch._

object ClusterProtocolHandler extends Log

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ClusterProtocolHandler(var peer:Peer=null) extends ProtocolHandler {
  import ClusterProtocolHandler._

  def protocol = PROTOCOL_NAME

  var closed = false
  var died = false
  var waiting_on:String = "client request"
  var transport_handler: (AnyRef)=>Unit = connecting_handler

  def session_id = None

  def dispatch_queue = connection.dispatch_queue

  class ProtocolException(msg:String) extends RuntimeException(msg)
  class Break extends RuntimeException
  def async_die(msg:String, e:Throwable=null) = try {
    die(msg)
  } catch {
    case x:Break=>
  }

  def die[T](msg:String, e:Throwable=null):T = {
    if( e!=null) {
      debug(e, "Shutting connection down due to: "+msg)
    } else {
      debug("Shutting connection down due to: "+msg)
    }
    die()
  }

  private def die[T]():T = {
    if( !died  ) {
      died = true
      transport_handler = dead_handler
      waiting_on = "shutdown"
      connection.transport.resumeRead
      connection.stop()
    }
    throw new Break()
  }

  def suspendRead(reason:String) = {
    waiting_on = reason
    connection.transport.suspendRead
  }

  def resumeRead() = {
    waiting_on = "client request"
    connection.transport.resumeRead
  }

  override def create_connection_status = {
    if ( peer==null ) {
      super.create_connection_status
    } else {
      peer.create_connection_status(this)
    }
  }

  override def on_transport_connected = {
    if( peer!=null ) {
      peer.on_client_connected(this)
    }
    resumeRead
  }

  override def on_transport_disconnected = {
    if( !closed ) {
//      heart_beat_monitor.stop
      closed=true
      died = true
      transport_handler = dead_handler

      if( peer!=null ) {
        peer.on_peer_disconnected(this)
      }
    }
  }

  override def on_transport_failure(error: IOException) = {
  }

  override def on_transport_command(command: AnyRef) = {
    try {
      transport_handler(command)
    }  catch {
      case e: Break =>
      case e:Exception =>
        e.printStackTrace
        async_die("Internal Server Error", e);
    }
  }

  def cluster_connector = {
    connection.connector.broker.connectors.values.
            find(_.isInstanceOf[ClusterConnector]).
            map(_.asInstanceOf[ClusterConnector])
  }

  def connecting_handler(command: AnyRef):Unit = command match {
    case frame:Frame =>
      frame.command match {
        case COMMAND_HELLO =>
          val hello = ProtocolHello.FACTORY.parseFramed(frame.data)

          info("remote peer %s connected.", hello.getId)
          transport_handler = connected_handler

          // if the peer is set, that means that this is a client
          // a client/outbound connection.
          if( peer !=null ) {
            // this is a server hello response...
            peer.on_server_hello(this, hello)
          } else {
            suspendRead("Looking up peer")
            cluster_connector match {
              case Some(connector)=>
                peer = connector.get_peer(hello.getId)
                // make subsequent events from the connection use
                // the peer's dispatch queue.
                dispatch_queue.setTargetQueue(peer.dispatch_queue)
                peer.dispatch_queue {
                  peer.on_client_hello(this, hello)
                }
                resumeRead
              case None =>
                die("Cluster connector not enabled")
            }
          }
        case COMMAND_HELLO =>
          die("Expected a hello command.")
      }
    case _=> die("Internal Server Error")
  }

  def connected_handler(command: AnyRef):Unit = command match {
    case frame:Frame=>
      peer.on_frame(this, frame)
    case _=> die("Internal Server Error");
  }


  def dead_handler(command: AnyRef):Unit = {
    // once dead.. we drop subsequently queued commands.
  }

}
