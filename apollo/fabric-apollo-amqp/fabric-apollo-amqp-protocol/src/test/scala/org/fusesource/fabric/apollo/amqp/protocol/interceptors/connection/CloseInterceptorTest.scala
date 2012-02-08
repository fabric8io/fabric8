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

import org.apache.activemq.apollo.util.FunSuiteSupport
import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.types.{Close, AMQPTransportFrame}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{SimpleFrame, CloseConnection}
import org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors.{FailInterceptor, TaskExecutingInterceptor, TestSendInterceptor}
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Tasks

/**
 *
 */
class CloseInterceptorTest extends FunSuiteSupport with ShouldMatchers with Logging {

  test("Create close interceptor, throw exception and receive close frame") {

    val close_interceptor = new CloseInterceptor

    var received_close_frame = false
    var received_close_connection = false

    close_interceptor.head.outgoing = new TestSendInterceptor((frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      info("Got : %s", frame)
      frame match {
        case f:AMQPTransportFrame =>
          f.getPerformative match {
            case c:Close =>
              received_close_frame = true
            case _ =>
              fail("Should not have received " + f.getPerformative)
          }
        case c:CloseConnection =>
          received_close_connection = true
        case _ =>
          fail("Should not have received " + frame)
      }
    })
    close_interceptor.head.outgoing = new TaskExecutingInterceptor
    close_interceptor.tail.incoming = new FailInterceptor

    close_interceptor.head.receive(new SimpleFrame, Tasks())

    received_close_frame should be (true)
    received_close_connection should be (true)
  }

  test("Create close interceptor, send close frame and verify close connection is also sent") {

    val close_interceptor = new CloseInterceptor

    var received_close_frame = false
    var received_close_connection = false

    close_interceptor.head.outgoing = new TestSendInterceptor((frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      info("Got : %s", frame)
      frame match {
        case f:AMQPTransportFrame =>
          f.getPerformative match {
            case c:Close =>
              received_close_frame = true
            case _ =>
              fail("Should not have received " + f.getPerformative)
          }
        case c:CloseConnection =>
          received_close_connection = true
        case _ =>
          fail("Should not have received " + frame)
      }
    })
    close_interceptor.head.outgoing = new TaskExecutingInterceptor


    close_interceptor.tail.send(new AMQPTransportFrame(new Close), Tasks())

    received_close_frame should be (true)
    received_close_connection should be (true)
  }
}
