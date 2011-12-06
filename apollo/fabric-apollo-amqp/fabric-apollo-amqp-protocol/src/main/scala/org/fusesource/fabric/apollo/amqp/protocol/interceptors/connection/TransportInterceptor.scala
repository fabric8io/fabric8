/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.connection

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import java.io.IOException
import org.apache.activemq.apollo.transport.{Transport, TransportListener}
import org.apache.activemq.apollo.util.Logging
import org.fusesource.hawtdispatch._
import org.fusesource.fabric.apollo.amqp.protocol.AMQPCodec
import org.apache.activemq.apollo.broker.{OverflowSink, TransportSink, Sink, SessionSinkMux}
import org.fusesource.fabric.apollo.amqp.codec.types.{AMQPProtocolHeader, AMQPTransportFrame}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{ConnectionClosed, CloseConnection, ConnectionCreated}
import org.fusesource.fabric.apollo.amqp.protocol.utilities.{execute, Tasks}

/**
 *
 */

class TransportInterceptor extends Interceptor with TransportListener with Logging {

	var session_manager: SessionSinkMux[AMQPFrame] = null
	var connection_sink: Sink[AMQPFrame] = null
  var _error:Option[Throwable] = None
	private var _transport: Option[Transport] = None

	var _on_connect:Option[() => Unit] = None
  var _on_disconnect:Option[() => Unit] = None

  def transport = _transport.get
  def transport_=(t:Transport) = {
    require(t != null, "Transport cannot be null")
    _transport = Option(t)
    transport.setProtocolCodec(new AMQPCodec)
    transport.setTransportListener(this)
    if (transport.getDispatchQueue == null) {
      queue = Dispatch.createQueue
      transport.setDispatchQueue(queue)
    } else {
      queue = transport.getDispatchQueue
    }
    transport.start
  }

  def error:Throwable = _error.getOrElse(null.asInstanceOf[Throwable])

	def onTransportCommand(command: AnyRef) {
		command match {
			case f:AMQPFrame =>
				receive(f, Tasks())
			case _ =>
				// TODO - handle this case in some way
		}
	}

	def onRefill() {}

	def onTransportFailure(error: IOException) = {
		trace("Connection to %s failed with %s:%s", transport.getRemoteAddress, error, error.getMessage)
		receive(ConnectionClosed(), Tasks())
    _error = Option(error)
    _on_disconnect.foreach((x) => x())
	}

	def onTransportConnected() {
		trace("Connected to %s via %s", transport.getRemoteAddress, transport.getTypeId)
		val transport_sink = new TransportSink(transport)

		session_manager = new SessionSinkMux[AMQPFrame](transport_sink.map {
				x =>
				x
			}, queue, AMQPCodec)
		connection_sink = new OverflowSink(session_manager.open(queue))
		connection_sink.refiller = NOOP
		receive(ConnectionCreated(transport), Tasks())
		_on_connect.foreach( x => x() )
		transport.resumeRead
	}

	def onTransportDisconnected() {
		trace("Disconnected from %s", transport.getRemoteAddress)
		receive(ConnectionClosed(), Tasks())
    _on_disconnect.foreach((x) => x())
	}

	override protected def _send(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
    _transport match {
      case Some(t) =>
        if (t.isConnected) {
          frame match {
            case f:AMQPTransportFrame =>
              connection_sink.offer(f)
            case f:AMQPProtocolHeader =>
              connection_sink.offer(f)
            case c:CloseConnection =>
              c.reason match {
                case Some(reason) =>
                  _error = Option(new RuntimeException(reason))
                case None =>
              }
              c.exception match {
                case Some(reason) =>
                  _error = Option(reason)
                case None =>
              }
              tasks.enqueue( () => {
                trace("Closing connection")
                transport.stop(^{
                  receive(ConnectionClosed(), Tasks())
                })
              })
            case _ =>
              debug("Dropping frame %s", frame)
          }
        }
      case None =>
    }
    execute(tasks)
	}

	def on_connect_=(func:() => Unit) = _on_connect = Option(func)
	def on_connect = _on_connect.getOrElse(() => {})
  def on_disconnect_=(func:() => Unit) = _on_disconnect = Option(func)
  def on_disconnect = _on_disconnect.getOrElse(() => {})

}