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

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.connection

import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
import org.scalatest.matchers.ShouldMatchers
import org.fusesource.hawtdispatch._
import internal.util.RunnableCountDownLatch
import org.apache.activemq.apollo.broker.transport.TransportFactory
import org.fusesource.hawtdispatch.transport._
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
    server.setTransportServerListener(new TransportServerListener {
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

    var cd = new RunnableCountDownLatch(1)
    server.start(cd)
    cd.await()

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

    cd = new RunnableCountDownLatch(1)
    client.start(cd)
    cd.await()

    client_wait.await(10, TimeUnit.SECONDS) should be (true)

  }

}