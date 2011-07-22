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
import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
import org.apache.activemq.apollo.transport.{Transport, TransportAcceptListener, TransportFactory}
import org.fusesource.fabric.apollo.amqp.protocol.AMQPCodec
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import java.util.concurrent.{CountDownLatch, TimeUnit}
import org.fusesource.fabric.apollo.amqp.codec.types.{NoPerformative, AMQPTransportFrame, AMQPProtocolHeader}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{OpenSent, ConnectionClosed, CloseConnection}

/**
 *
 */

class InterceptorIntegrationTest extends FunSuiteSupport with ShouldMatchers with Logging {

  test("Create server, create client, send empty frame, disconnect") {

    val server_queue = Dispatch.createQueue("Server Queue")

    val client_disconnect_wait = new CountDownLatch(1)
    val server_disconnect_wait = new CountDownLatch(1)

    val server = TransportFactory.bind("pipe://vm:0/blah?marshal=true")
    server.setDispatchQueue(server_queue)

    server.setAcceptListener(new TransportAcceptListener {
      def onAccept(transport: Transport) {
        val transport_interceptor = new TransportInterceptor
        transport_interceptor.transport = transport
        transport_interceptor.incoming = new HeaderInterceptor
        transport_interceptor.incoming.incoming = new CloseInterceptor
        val heartbeat_interceptor = new HeartbeatInterceptor
        heartbeat_interceptor.transport = transport
        transport_interceptor.incoming.incoming.incoming = heartbeat_interceptor
        val open_interceptor = new OpenInterceptor
        open_interceptor.open.setIdleTimeout(2500L)
        transport_interceptor.incoming.incoming.incoming.incoming = open_interceptor
        transport_interceptor.incoming.incoming.incoming.incoming.incoming = new Interceptor {
          def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
            frame match {
              case c:ConnectionClosed =>
                server_disconnect_wait.countDown
              case _ =>
            }
          }

          def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = outgoing.send(frame, tasks)
        }
        transport.setTransportListener(transport_interceptor)
        transport.setProtocolCodec(new AMQPCodec)
        transport.setDispatchQueue(server_queue)
        transport.start
      }

      def onAcceptError(error: Exception) {}
    })

    val server_wait = new CountDownLatch(1)
    server.start( ^{
      info("Server started")
      server_wait.countDown
    })

    server_wait.await(10, TimeUnit.SECONDS) should be (true)

    val client_queue = Dispatch.createQueue("Client Queue")

    val client_wait = new CountDownLatch(1)

    val client = TransportFactory.connect("pipe://vm:0/blah")
    client.setDispatchQueue(client_queue)
    client.setProtocolCodec(new AMQPCodec)
    val transport_interceptor = new TransportInterceptor
    client.setTransportListener(transport_interceptor)
    transport_interceptor.transport = client
    transport_interceptor.incoming = new HeaderInterceptor
    transport_interceptor.incoming.incoming = new CloseInterceptor
    val heartbeat_interceptor = new HeartbeatInterceptor
    heartbeat_interceptor.transport = client
    transport_interceptor.incoming.incoming.incoming = heartbeat_interceptor
    val open_interceptor = new OpenInterceptor
    open_interceptor.open.setIdleTimeout(2500L)
    transport_interceptor.incoming.incoming.incoming.incoming = open_interceptor
    transport_interceptor.incoming.incoming.incoming.incoming.incoming = new Interceptor {
      def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = outgoing.send(frame, tasks)

      def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
        frame match {
          case o:OpenSent =>
            client_queue.executeAfter(5, TimeUnit.SECONDS, ^ {
              client_wait.countDown
              send(CloseConnection(), new Queue[() => Unit])
            })
          case c:ConnectionClosed =>
            client_disconnect_wait.countDown
          case _ =>
        }
      }
    }

    transport_interceptor.on_connect = () => {
      info("Client connected")
    }

    client.start(^{
      info("Client started")
    })

    client_wait.await(10, TimeUnit.SECONDS) should be (true)
    server_disconnect_wait.await(10, TimeUnit.SECONDS) should be (true)
    client_disconnect_wait.await(10, TimeUnit.SECONDS) should be (true)
  }

}