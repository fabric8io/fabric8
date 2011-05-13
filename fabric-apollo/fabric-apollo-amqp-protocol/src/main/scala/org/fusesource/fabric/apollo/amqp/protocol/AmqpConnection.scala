/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.protocol

import org.fusesource.hawtdispatch._
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory._
import java.io.{EOFException, IOException}
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.apache.activemq.apollo.transport._
import scala.math._
import scala.util.Random
import collection.mutable.HashMap
import collection.mutable.Map
import org.fusesource.fabric.apollo.amqp.api._
import org.apache.activemq.apollo.broker.{OverflowSink, TransportSink, SinkMux, Sink}
import org.fusesource.fabric.apollo.amqp.codec._
import java.net.URI
import java.util.concurrent.TimeUnit
import org.apache.activemq.apollo.broker.protocol.HeartBeatMonitor
import AmqpConversions._
import org.apache.activemq.apollo.util.{URISupport, Logging}

/**
 *
 */
object AmqpConnection {

  val DEFAULT_DIE_DELAY = 1 * 1000
  var die_delay = DEFAULT_DIE_DELAY

  val DEFAULT_HEARTBEAT = 2 * 1000L

  def connection() = {
    val rc = new AmqpConnection
    rc.asInstanceOf[Connection]
  }

  def serverConnection(listener:ConnectionListener) = {
    val rc = new AmqpServerConnection(listener)
    rc.asInstanceOf[ServerConnection]
  }
}

class AmqpConnection extends Connection with ConnectionHandler with SessionConnection with TransportListener with Logging {
  import AmqpConnection._

  var dispatchQueue: DispatchQueue =  Dispatch.createQueue

  var outbound_sessions: SinkMux[AnyRef] = null
  var connection_session: Sink[AnyRef] = null
  var transport_sink:TransportSink = null

  var containerId: String = null
  var peerContainerId: String = null
  var transport: Transport = null
  var last_error: Throwable = null
  val sessions: HashMap[Int, SessionHandler] = new HashMap[Int, SessionHandler]
  val channels: HashMap[Int, Int] = new HashMap[Int, Int]
  val connectionHandler: AmqpCommandHandler = new AmqpCommandHandler(this, null)
  val headerSent: AtomicBoolean = new AtomicBoolean(false)
  val openSent: AtomicBoolean = new AtomicBoolean(false)
  val closeSent: AtomicBoolean = new AtomicBoolean(false)
  val heartbeat_monitor = new HeartBeatMonitor

  var idle_timeout = DEFAULT_HEARTBEAT
  var heartbeat_interval = idle_timeout / 2

  var uri:URI = null
  var options:Map[String, String] = null
  var hostname:Option[String] = None
  var maxFrameSize: Long = 0
  var sessionListener:Option[SessionListener] = None
  var connectedTask:Option[Runnable] = None
  var disconnectedTask:Option[Runnable] = None
  var channel_max = 32767
  var connecting = false

  def onConnected(task:Runnable) = connectedTask = Option(task)
  def onDisconnected(task:Runnable) = disconnectedTask = Option(task)

  def connected = transport.isConnected
  def error = last_error

  def init(uri: String) = {
    this.uri = new URI(uri)
    import scala.collection.JavaConversions._
    options = mapAsScalaMap(URISupport.parseParamters(this.uri))
    options.get("idleTimeout") match {
      case Some(timeout) =>
        idle_timeout = timeout.toLong
      case None =>
    }
    options.get("containerId") match {
      case Some(id) =>
        containerId = id
      case None =>
        containerId = UUID.randomUUID.toString
    }
    hostname = Option(this.uri.getHost)
  }

  def connect(t:Option[Transport], uri:String) = {
    init(uri)
    t match {
      case Some(t) =>
        transport = t
      case None =>
        transport = TransportFactory.connect(uri)
    }
    transport.setProtocolCodec(new AmqpCodec)
    transport.setTransportListener(this)
    transport.setDispatchQueue(dispatchQueue)
    transport_sink = new TransportSink(transport)
    transport.start
  }

  def connect(uri:String, onConnected:Runnable): Unit = {
    this.onConnected(onConnected)
    connecting = true
    connect(None, uri)
  }

  def createSession:Session = session(false, 0)

  def session(fromRemote:Boolean, remoteChannel:Int): Session = {
    def random_ushort = {
      val bytes:Array[Byte] = Array(0, 0)
      Random.nextBytes(bytes)
      abs(BitUtils.getUShort(bytes, 0))
    }
    var channel = random_ushort
    val keys = sessions.keys
    while (channel > channel_max && keys.exists(x => (x == channel))) {
      channel = random_ushort
    }
    val session = new AmqpSession(this, channel)
    val handler = new SessionHandler(this, session)
    sessions.put(channel, handler)
    if (fromRemote) {
      session.remote_channel = remoteChannel
      channels.put(remoteChannel,session.channel)
    }
    //trace("Session created : %s", handler.session);
    sessionListener.foreach((x) => x.sessionCreated(this, session))
    session
  }

  def release(channel: Int): Unit = dispatchQueue {
    sessions.remove(channel) match {
      case Some(handler) =>
        val remote_channel = channels.remove(handler.session.remote_channel)
        //trace("Session released : %s", handler.session)
        sessionListener.foreach((x) => x.sessionReleased(this, handler.session))
      case None =>
    }
  }

  def handle_frame(frame:AmqpFrame) = {
    val channel = frame.getChannel

    frame.getBody match {
      case o:AmqpOpen =>
        open(o)
      case c:AmqpClose =>
        close
      case _ =>
        get_session(channel, frame.getBody) match {
          case Some(handler) =>
            frame.handle(handler.handler)
          case None =>
            frame.getBody match {
              case e:AmqpEnd =>
              case f:AmqpFlow =>
              case _ =>
                val error = "Received frame for session that doesn't exist"
                warn("%s : %s", error, frame)
                throw new RuntimeException(error)
            }
        }
    }
  }

  def session_from_remote_channel(channel:Int) = {
    channels.get(channel) match {
      case Some(channel) =>
        sessions.get(channel)
      case None =>
        None
    }
  }

  def get_session(channel:Int, command:AnyRef):Option[SessionHandler] = {
    command match {
      case b:AmqpBegin =>
        Option(b.getRemoteChannel) match {
          case Some(local_channel) =>
            channels.put(channel, local_channel.intValue)
            //trace("Received response to begin frame sent from local_channel=%s from remote_channel=%s", local_channel.intValue, channel)
            session_from_remote_channel(channel) match {
              case Some(h) =>
                h.session.remote_channel = channel
                Option(h)
              case None =>
                None
            }
          case None =>
            val s = session(true, channel)
            //trace("Created session from remote begin request %s", s)
            session_from_remote_channel(channel)
        }
      case _ =>
        session_from_remote_channel(channel)
    }
  }

  def onTransportCommand(command:Object): Unit = {
    try {
      command match {
        case a:AmqpProtocolHeader =>
          header(a)
        case frame:AmqpFrame =>
          if (frame.getBody == null) {
            return
          }
          if (closeSent.get) {
            frame.getBody match {
              case c:AmqpClose =>
                handle_frame(frame)
              case e:AmqpEnd =>
                handle_frame(frame)
              case _ =>
                debug("disposing of frame : " + frame + ", connection is closed")
            }
          } else {
            handle_frame(frame)
          }
        case _ =>
          throw new RuntimeException("Received invalid frame : " + command)
      }
    } catch {
      case e: Exception => {
        warn("frame processing error, closing connection", e)
        close(e)
      }
    }
  }

  def header(protocolHeader: AmqpProtocolHeader): Unit = {
    if ( !headerSent.getAndSet(true) ) {
      dispatchQueue !! {
        val response = new AmqpProtocolHeader
        /*
        Option(protocolHeader) match {
          case Some(h) =>
            trace("Received header {%s}, responding with {%s}", h, response)
          case None =>
            trace("Sending protocol header {%s}", response)

        }
        */
        val rc = send(response)
      }
    }

    if ( protocolHeader != null ) {
      if ( protocolHeader.major != AmqpDefinitions.MAJOR && protocolHeader.minor != AmqpDefinitions.MINOR && protocolHeader.revision != AmqpDefinitions.REVISION ) {
        val e = new RuntimeException("Unexpected protocol version received!")
        close(e)
        throw e
      }
    }
    open(null)
  }

  def open(open: AmqpOpen): Unit = {
    Option(open).foreach((open) => {
      Option(open.getMaxFrameSize).foreach((x) => setMaxFrameSize(x.asInstanceOf[Long]))
      peerContainerId = open.getContainerId
      Option(open.getChannelMax).foreach((x) => channel_max = min(channel_max, x.intValue))
    })
    if ( !openSent.getAndSet(true) ) {
      val response: AmqpOpen = createAmqpOpen
      response.setChannelMax(channel_max)
      response.setIdleTimeOut(idle_timeout)
      Option(containerId).foreach((x) => response.setContainerId(x.asInstanceOf[String]))
      response.setContainerId(containerId)
      hostname.foreach((x) => response.setHostname(x))
      if ( getMaxFrameSize != 0 ) {
        response.setMaxFrameSize(getMaxFrameSize)
      }
      dispatchQueue !! {
        /*
        Option(open) match {
          case Some(o) =>
            trace("Received open frame {%s}, responding with {%s}", o, response)
          case None =>
            trace("Sending open frame {%s}", response)
        }
        */
        val rc = send(response)
      }
    }
    Option(open).foreach((o) => {
      connecting = false
      Option(open.getIdleTimeOut) match {
        case Some(timeout) =>
          idle_timeout = idle_timeout.min(timeout.getValue.longValue)
          heartbeat_interval = idle_timeout / 2
          heartbeat_monitor.read_interval = idle_timeout
          heartbeat_monitor.write_interval = heartbeat_interval
          heartbeat_monitor.transport = transport
          heartbeat_monitor.on_dead = () => {
            close("Idle timeout expired")
          }
          heartbeat_monitor.on_keep_alive = () => {
            val frame = new AmqpFrame
            frame.setType(AmqpFrame.AMQP_FRAME_TYPE)
            connection_session.offer(frame)
          }
          heartbeat_monitor.start
        case None =>
      }
      connectedTask.foreach((x) => dispatchQueue << x)
    })
  }

  def setOnClose(task:Runnable) = onDisconnected(task)

  def close: Unit = close(None)

  def close(reason:String):Unit = {
    val error = createAmqpError
    error.setCondition(reason)
    error.setDescription(reason)
    last_error = new RuntimeException(reason)
    close(Option(error))
  }

  def close(t:Throwable):Unit = {
    val error = createAmqpError
    error.setCondition(t.getClass + " : " + t.getMessage)
    error.setDescription(t.getStackTraceString)
    last_error = t
    close(Option(error))
  }

  def close(error:Option[AmqpError]):Unit = {
    if (!closeSent.getAndSet(true)) {
      sessions.foreach {
        case (channel, handler) =>
          handler.session.end(error)
          handler.session.on_end.foreach((x) => dispatchQueue << x)
      }
      val close = createAmqpClose
      error match {
        case Some(e) =>
          close.setError(e)
          warn("Closing connection due to error : %s", e)
        case None =>
          info("Closing connection")
      }
      heartbeat_monitor.stop
      send(close)
      dispatchQueue.after(die_delay, TimeUnit.MILLISECONDS) {
        stop(disconnectedTask.getOrElse(NOOP))
      }
    }
  }

  protected def stop(on_stop:Runnable):Unit = transport.stop(on_stop)

  def send(command:AnyRef):Boolean = send(0, command)

  def send(channel:Int, command:AmqpCommand):Boolean = send(channel, command.asInstanceOf[AnyRef])

  def send(channel:Int, command:AnyRef, sasl:Boolean = false):Boolean = {

    def createFrame(command:AmqpCommand, sasl:Boolean = false) = {
      val rc = new AmqpFrame(command)
      if (sasl) {
        rc.setType(AmqpFrame.AMQP_SASL_FRAME_TYPE)
      } else {
        rc.setType(AmqpFrame.AMQP_FRAME_TYPE)
      }
      //trace("Setting outgoing frame channel to %s" ,channel)
      rc.setChannel(channel)
      rc
    }

    command match {
      case header:AmqpProtocolHeader =>
        doSend(header)
      case open:AmqpOpen =>
        doSend(createFrame(open))
      case close:AmqpClose =>
        doSend(createFrame(close))
      case command:AmqpCommand =>
        if ( closeSent.get ) {
          // can silently ignore detach/end frames
          command match {
            case d:AmqpDetach =>
              return true
            case e:AmqpEnd =>
              return true
            case _ =>
              warn("Transport is not connected, discarding outgoing frame body %s for channel %s", command, channel)
              if (last_error != null) {
                throw last_error
              } else {
                throw new RuntimeException("Transport connection not established")
              }
          }
        }

        doSend(createFrame(command, sasl))
      case _ =>
        warn("Unknown frame body passed to send : %s", command)
        false
    }
  }

  def doSend(frame:AnyRef) = {
    if ( getCurrentQueue != dispatchQueue ) {
      var rc = false
      dispatchQueue !! {
        rc = connection_session.offer(frame)
      }
      rc
    } else {
      connection_session.offer(frame)
    }
  }

  def getContainerId = containerId
  def setContainerId(id:String) = containerId = id

  def getPeerContainerId = peerContainerId

  def onRefill: Unit = {
    //trace("onRefill called...")
    if( transport_sink.refiller !=null ) {
      transport_sink.refiller.run
    }
  }

  def onTransportFailure(some_error: IOException): Unit = {
    some_error match {
      case e:EOFException =>
        info("Peer closed connection")
        close
      case _ =>
        info("Transport failure received : %s", some_error)
        close(some_error)
        this.last_error = some_error
        if (connecting) {
          connectedTask.foreach((x) => dispatchQueue << x)
        }
    }
  }

  def onTransportConnected: Unit = {
    //trace("Connected to %s:/%s", transport.getTypeId, transport.getRemoteAddress)
    outbound_sessions = new SinkMux[AnyRef](transport_sink.map {
      x =>
        x
    }, dispatchQueue, AmqpCodec)
    connection_session = new OverflowSink(outbound_sessions.open(dispatchQueue));
    connection_session.refiller = NOOP
    header(null)
    transport.resumeRead
  }

  def onTransportDisconnected: Unit = {
    //trace("Disconnected from %s", transport.getRemoteAddress)
    close
  }

  def getMaxFrameSize = maxFrameSize

  def getDispatchQueue = dispatchQueue

  def setSessionListener(l:SessionListener) = sessionListener = Option(l)

  def setMaxFrameSize(size: Long): Unit = {
    maxFrameSize = if (maxFrameSize != 0) {
      min(maxFrameSize, size)
    } else {
      size
    }
  }

  override def toString = {
    val rc = new StringBuilder("AmqpConnection{")

    Option(transport) match {
      case Some(transport) =>
        rc.append(transport.getTypeId + ":/" + transport.getRemoteAddress)
      case None =>
    }
    rc.append("}")
    rc.toString
  }

  def sasl_outcome(saslOutcome: AmqpSaslOutcome) {}

  def sasl_init(saslInit: AmqpSaslInit) {}

  def sasl_mechanisms(saslMechanisms: AmqpSaslMechanisms) {}

  def sasl_response(saslResponse: AmqpSaslResponse) {}

  def sasl_challenge(saslChallenge: AmqpSaslChallenge) {}
}
