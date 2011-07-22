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
import org.fusesource.fabric.apollo.amqp.codec.types.{AMQPTransportFrame, AMQPProtocolHeader}
import java.util.concurrent.{CountDownLatch, TimeUnit}

/**
 *
 */

class TransportInterceptorTest extends FunSuiteSupport with ShouldMatchers with Logging {

  test("Create server, create client, send empty frame, disconnect") {

    val server_queue = Dispatch.createQueue("Server Queue")
    server_queue.resume

    val client_disconnect_wait = new CountDownLatch(1)
    val server_disconnect_wait = new CountDownLatch(1)

    val server = TransportFactory.bind("pipe://vm:0/blah?marshal=true")
    server.setDispatchQueue(server_queue)

    server.setAcceptListener(new TransportAcceptListener {
      def onAccept(transport: Transport) {
        val listener = new TransportInterceptor
        listener.transport = transport
        listener.incoming = new HeaderInterceptor
        listener.incoming.incoming = new OpenInterceptor
        listener.incoming.incoming.incoming = new Interceptor {
          def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
            transport.stop(^{
              server_disconnect_wait.countDown
            })
          }

          def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = outgoing.send(frame, tasks)
        }
        transport.setTransportListener(listener)
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
    client_queue.resume

    val client = TransportFactory.connect("pipe://vm:0/blah")
    client.setDispatchQueue(client_queue)
    client.setProtocolCodec(new AMQPCodec)
    val listener = new TransportInterceptor
    client.setTransportListener(listener)
    listener.transport = client
    listener.incoming = new HeaderInterceptor
    listener.incoming.incoming = new OpenInterceptor
    listener.incoming.incoming.incoming = new Interceptor {
      def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = outgoing.send(frame, tasks)

      def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
        client.stop(^{
          client_disconnect_wait.countDown
        })
      }
    }

    val client_wait = new CountDownLatch(1)

    listener.on_connect = () => {
      info("Client connected")
      listener.incoming.send(new AMQPTransportFrame, new Queue[() => Unit])
      client_wait.countDown
    }

    client.start(^{
      info("Client started")
    })

    client_wait.await(10, TimeUnit.SECONDS) should be (true)

    server_disconnect_wait.await(10, TimeUnit.SECONDS) should be (true)
    client_disconnect_wait.await(10, TimeUnit.SECONDS) should be (true)
  }

}