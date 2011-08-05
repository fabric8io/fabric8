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

import connection._
import org.fusesource.hawtdispatch._
import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
import org.apache.activemq.apollo.transport.{TransportAcceptListener, TransportFactory, Transport}
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor._
import java.util.concurrent.{TimeUnit, CountDownLatch}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{CloseConnection, OpenSent, ConnectionClosed}
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Tasks

/**
 *
 */
class InterceptorIntegrationTest extends FunSuiteSupport with ShouldMatchers with Logging {

  ignore("Create server, create client, send empty frame, disconnect") {

    val server_queue = Dispatch.createQueue("Server Queue")

    val client_disconnect_wait = new CountDownLatch(1)
    val server_disconnect_wait = new CountDownLatch(1)

    val server = TransportFactory.bind("pipe://vm:0/blah?marshal=true")
    server.setDispatchQueue(server_queue)

    server.setAcceptListener(new TransportAcceptListener {
      def onAccept(transport: Transport) {
        val transport_interceptor = new TransportInterceptor
        transport_interceptor.tail.incoming = new HeaderInterceptor
        transport_interceptor.tail.incoming = new CloseInterceptor
        val heartbeat_interceptor = new HeartbeatInterceptor
        heartbeat_interceptor.transport = transport
        heartbeat_interceptor.local_idle_timeout = Option(1500L)
        transport_interceptor.tail.incoming = heartbeat_interceptor
        val open_interceptor = new OpenInterceptor
        transport_interceptor.tail.incoming = open_interceptor
        transport_interceptor.tail.incoming = new Interceptor {
          override protected def _receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
            frame match {
              case c:ConnectionClosed =>
                server_disconnect_wait.countDown
              case _ =>
            }
          }

          override protected def _send(frame: AMQPFrame, tasks: Queue[() => Unit]) = outgoing.send(frame, tasks)
        }
        transport_interceptor.transport = transport
        info("Server created chain : %s", display_chain(transport_interceptor))
      }

      def onAcceptError(error: Exception) {}
    })

    val server_wait = new CountDownLatch(1)
    server.start( ^{
      info("Server started")
      server_wait.countDown
    })

    server_wait.await(10, TimeUnit.SECONDS) should be (true)

    val client_wait = new CountDownLatch(1)

    val client = TransportFactory.connect("pipe://vm:0/blah")
    val transport_interceptor = new TransportInterceptor
    transport_interceptor.tail.incoming = new HeaderInterceptor
    transport_interceptor.tail.incoming = new CloseInterceptor
    val heartbeat_interceptor = new HeartbeatInterceptor
    heartbeat_interceptor.transport = client
    heartbeat_interceptor.local_idle_timeout = Option(2500L)
    transport_interceptor.tail.incoming = heartbeat_interceptor
    val open_interceptor = new OpenInterceptor
    transport_interceptor.tail.incoming = open_interceptor
    transport_interceptor.tail.incoming = new Interceptor {
      override protected def _send(frame: AMQPFrame, tasks: Queue[() => Unit]) = outgoing.send(frame, tasks)

      override protected def _receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
        frame match {
          case o:OpenSent =>
            transport_interceptor.queue.executeAfter(5, TimeUnit.SECONDS, ^ {
              client_wait.countDown
              send(CloseConnection(), Tasks())
            })
          case c:ConnectionClosed =>
            client_disconnect_wait.countDown
          case _ =>
        }
      }
    }

    transport_interceptor.transport = client

    info("Created client chain : %s", display_chain(transport_interceptor))

    transport_interceptor.on_connect = () => {
      info("Client connected, chain : %s", display_chain(transport_interceptor))
    }

    client_wait.await(10, TimeUnit.SECONDS) should be (true)
    server_disconnect_wait.await(10, TimeUnit.SECONDS) should be (true)
    client_disconnect_wait.await(10, TimeUnit.SECONDS) should be (true)
  }

}