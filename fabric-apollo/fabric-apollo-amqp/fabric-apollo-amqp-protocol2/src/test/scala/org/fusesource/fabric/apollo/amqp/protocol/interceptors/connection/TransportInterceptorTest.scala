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

import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
import org.scalatest.matchers.ShouldMatchers
import org.fusesource.hawtdispatch._
import org.apache.activemq.apollo.transport.{Transport, TransportAcceptListener, TransportFactory}
import org.fusesource.fabric.apollo.amqp.protocol.AMQPCodec
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.commands.CloseConnection
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import java.util.concurrent.{TimeUnit, CountDownLatch}
import org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors.FrameDroppingInterceptor
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Tasks

/**
 *
 */

class TransportInterceptorTest extends FunSuiteSupport with ShouldMatchers with Logging {

  test("Create server and client") {

    val server = TransportFactory.bind("pipe://localhost:0/test")
    server.setDispatchQueue(Dispatch.createQueue("Server Queue"))
    server.setAcceptListener(new TransportAcceptListener {
      def onAccept(transport: Transport) {
        val transport_interceptor = new TransportInterceptor
        transport_interceptor.tail.incoming = new FrameDroppingInterceptor
        transport_interceptor.transport = transport
        transport.setTransportListener(transport_interceptor)
        transport.setProtocolCodec(new AMQPCodec)
        transport.setDispatchQueue(server.getDispatchQueue)
        transport.start(^{
          server.getDispatchQueue.executeAfter(2, TimeUnit.SECONDS, ^{
            transport_interceptor.send(CloseConnection(), Tasks())
          })
        })
      }

      def onAcceptError(error: Exception) {}
    })

    server.start

    val client_wait = new CountDownLatch(1)

    val client = TransportFactory.connect("pipe://localhost:0/test")
    client.setDispatchQueue(Dispatch.createQueue("Client Queue"))
    client.setProtocolCodec(new AMQPCodec)
    val transport_interceptor = new TransportInterceptor
    transport_interceptor.tail.incoming = new FrameDroppingInterceptor
    transport_interceptor.transport = client
    client.setTransportListener(transport_interceptor)

    transport_interceptor.incoming = new Interceptor {
      override protected def _send(frame: AMQPFrame, tasks: Queue[() => Unit]) = {}

      override protected def _receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
        client_wait.countDown
      }
    }

    client.start

    client_wait.await(10, TimeUnit.SECONDS) should be (true)

  }

}