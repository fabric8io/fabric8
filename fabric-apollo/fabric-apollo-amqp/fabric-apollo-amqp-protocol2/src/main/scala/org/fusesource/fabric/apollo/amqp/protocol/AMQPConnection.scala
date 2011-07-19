/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 * 	http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.protocol

import interfaces.{ProtocolSession, ProtocolConnection}
import org.fusesource.hawtdispatch._
import org.fusesource.hawtbuf.Buffer
import java.io.IOException
import org.fusesource.fabric.apollo.amqp.codec.Codec._
import org.fusesource.hawtdispatch.DispatchQueue
import java.net.URI
import org.apache.activemq.apollo.util.{URISupport, Logging}
import collection.mutable.HashMap
import collection.mutable.Map
import java.util.UUID
import org.apache.activemq.apollo.transport.{TransportFactory, Transport, TransportListener}
import org.apache.activemq.apollo.broker.{OverflowSink, TransportSink, SessionSinkMux, Sink}
import java.util.concurrent.atomic.AtomicBoolean
import org.apache.activemq.apollo.broker.protocol.HeartBeatMonitor
import org.fusesource.fabric.apollo.amqp.codec.AMQPDefinitions
import java.lang.Long
import org.fusesource.fabric.apollo.amqp.codec.types.{AMQPTransportFrame, Open, AMQPProtocolHeader}
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Slot
import org.fusesource.fabric.apollo.amqp.protocol.api._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces._

/**
 *
 */
object AMQPConnection {

  val DEFAULT_DIE_DELAY = 1 * 1000
  var die_delay = DEFAULT_DIE_DELAY

  val DEFAULT_HEARTBEAT = 10 * 1000L

  var factory: ConnectionFactory = null

  def createConnection: Connection = {
    if ( factory == null ) {
      factory = new DefaultConnectionFactory
    }
    factory.createConnection
  }

  def createServerConnection(handler: ConnectionHandler): ServerConnection = {
    if ( factory == null ) {
      factory = new DefaultConnectionFactory
    }
    factory.createServerConnection(handler)
  }

}

/**
 *
 */
class AMQPConnection extends ProtocolConnection with TransportListener with Logging {

  import AMQPConnection._

  var dispatch_queue: DispatchQueue = null;

  var session_manager: SessionSinkMux[AnyRef] = null
  var connection_sink: Sink[AnyRef] = null
  var transport_sink: TransportSink = null

  var container_id: String = null
  var peer_container_id: String = null
  var transport: Transport = null
  var last_error: Throwable = null
  val sessions = new Slot[ProtocolSession]
  val channels: HashMap[Int, Int] = new HashMap[Int, Int]
  val header_sent: AtomicBoolean = new AtomicBoolean(false)
  val open_sent: AtomicBoolean = new AtomicBoolean(false)
  val close_sent: AtomicBoolean = new AtomicBoolean(false)
  val heartbeat_monitor = new HeartBeatMonitor

  var idle_timeout = DEFAULT_HEARTBEAT

  def heartbeat_interval = (idle_timeout - (idle_timeout * 0.05)).asInstanceOf[Long]

  var uri: URI = null
  var options: Map[String, String] = null
  var hostname: Option[String] = None
  var max_frame_size: Long = 0
  var session_handler: Option[SessionHandler] = None
  var connected_task: Option[Runnable] = None
  var disconnected_task: Option[Runnable] = None
  var channel_max = 32767
  var connecting = false

  var session_factory = new DefaultSessionFactory

  def onConnected(task: Runnable) = connected_task = Option(task)

  def onDisconnected(task: Runnable) = disconnected_task = Option(task)

  def setContainerID(id: String) = container_id = id

  def getContainerID = container_id

  def connected() = transport.isConnected

  def error() = last_error

  def setSessionHandler(handler: SessionHandler) = this.session_handler = Option(handler)

  def getPeerContainerID = peer_container_id

  def send(data: Buffer) = send(data, 0)

  def init(uri: String) = {
    this.uri = new URI(uri)
    import scala.collection.JavaConversions._
    options = URISupport.parseParamters(this.uri)
    options.get("idleTimeout") match {
      case Some(timeout) =>
        idle_timeout = timeout.toLong
      case None =>
    }
    options.get("containerId") match {
      case Some(id) =>
        container_id = id
      case None =>
        container_id = UUID.randomUUID.toString
    }
    hostname = Option(this.uri.getHost)
    if (dispatch_queue == null) {
      dispatch_queue = Dispatch.createQueue
    }
  }

  def connect(t: Option[Transport], uri: String) = {
    init(uri)
    transport = t.getOrElse(TransportFactory.connect(uri))
    transport.setProtocolCodec(new AMQPCodec)
    transport.setTransportListener(this)
    transport.setDispatchQueue(dispatch_queue)
    transport_sink = new TransportSink(transport)
    transport.start
  }

  def connect(uri: String) = {
    connecting = true
    connect(None, uri)
  }

  def header(protocolHeader: AMQPProtocolHeader): Unit = {
    if ( !header_sent.getAndSet(true) ) {

      def send_response = {
        val response = new AMQPProtocolHeader
        Option(protocolHeader) match {
          case Some(h) =>
            trace("Received header {%s}, responding with {%s}", h, response)
          case None =>
            trace("Sending protocol header {%s}", response)

        }
        val rc = send(AMQPProtocolHeader.PROTOCOL_HEADER)
      }
      if ( dispatch_queue != getCurrentQueue ) {
        dispatch_queue.future[Unit](send_response).apply
      } else {
        send_response
      }
    }

    if ( protocolHeader != null ) {
      if ( protocolHeader.major != AMQPDefinitions.MAJOR && protocolHeader.minor != AMQPDefinitions.MINOR && protocolHeader.revision != AMQPDefinitions.REVISION ) {
        val e = new RuntimeException("Unexpected protocol version received!")
        close(e)
        throw e
      }
    }
    open(null)
  }

  def setMaxFrameSize(maxFrameSize: Long) = this.max_frame_size = maxFrameSize

  def getMaxFrameSize: Long = max_frame_size

  def open(open: Open): Unit = {
    Option(open).foreach((open) => {
      Option(open.getMaxFrameSize).foreach((x) => setMaxFrameSize(x))
      this.peer_container_id = open.getContainerID
      Option(open.getChannelMax).foreach((x) => channel_max = channel_max.min(x))
    })
    if ( !open_sent.getAndSet(true) ) {
      val response: Open = new Open
      response.setChannelMax(channel_max)
      response.setIdleTimeout(idle_timeout)
      Option(container_id).foreach((x) => response.setContainerID(x.asInstanceOf[String]))
      response.setContainerID(container_id)
      hostname.foreach((x) => response.setHostname(x))
      if ( getMaxFrameSize != 0 ) {
        response.setMaxFrameSize(getMaxFrameSize)
      }

      def send_response = {
        Option(open) match {
          case Some(o) =>
            trace("Received open frame {%s}, responding with {%s}", o, response)
          case None =>
            trace("Sending open frame {%s}", response)
        }
        val rc = send(toBuffer(response))
      }
      if ( dispatch_queue != getCurrentQueue ) {
        dispatch_queue.future[Unit](send_response).apply
      } else {
        send_response
      }
    }
    Option(open).foreach((o) => {
      connecting = false
      Option(open.getIdleTimeout) match {
        case Some(timeout) =>
          idle_timeout = idle_timeout.min(timeout)
          heartbeat_monitor.read_interval = idle_timeout
          heartbeat_monitor.write_interval = heartbeat_interval
          heartbeat_monitor.transport = transport
          heartbeat_monitor.on_dead = () => {
            close("Idle timeout expired")
          }
          heartbeat_monitor.on_keep_alive = () => {
            val frame = new AMQPTransportFrame
            frame.setType(AMQPTransportFrame.AMQP_FRAME_TYPE)
            connection_sink.offer(frame)
          }
          heartbeat_monitor.start
        case None =>
      }
      connected_task.foreach((x) => dispatch_queue << x)
    })
  }

  def send(data: Buffer, channel: Int) = connection_sink.offer(new AMQPTransportFrame(data))

  def release(session: ProtocolSession) = {
    session.getRemoteChannel.foreach((x) => channels.remove(x))
    session.setRemoteChannel(null.asInstanceOf[Int])
    session.getLocalChannel.foreach((x) => sessions.free(x))
    session.setLocalChannel(null.asInstanceOf[Int])
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

  def createSession: Session = session(false, 0)

  def getDispatchQueue = dispatch_queue

  def close() {}

  def close(t: Throwable) {}

  def close(reason: String) {}

  def onTransportCommand(p1: AnyRef) {}

  def onRefill() {}

  def onTransportFailure(p1: IOException) {}

  def onTransportConnected() = {
    trace("Connected to %s:/%s", transport.getTypeId, transport.getRemoteAddress)
    session_manager = new SessionSinkMux[AnyRef](transport_sink.map {
      x =>
        x
    }, dispatch_queue, AMQPCodec)
    connection_sink = new OverflowSink(session_manager.open(dispatch_queue))
    connection_sink.refiller = NOOP
    header(null)
    transport.resumeRead
  }

  def onTransportDisconnected() {
    trace("Disconnected from %s", transport.getRemoteAddress)
    close
  }

  override def toString = {
    val rc = new StringBuilder(getClass.getSimpleName)
    rc.append("{");

    Option(transport) match {
      case Some(transport) =>
        rc.append(transport.getTypeId + ":/" + transport.getRemoteAddress)
      case None =>
    }
    rc.append("}")
    rc.toString
  }

}
