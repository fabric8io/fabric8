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

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.session

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.types.{Begin, AMQPTransportFrame}
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Tasks
import org.fusesource.fabric.apollo.amqp.protocol.commands.BeginSession
import org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors.{FrameDroppingInterceptor, TaskExecutingInterceptor, TestSendInterceptor}

/**
 *
 */

class BeginInterceptorTest extends FunSuiteSupport with ShouldMatchers with Logging {

  test("Begin a session locally") {

    val begin = new BeginInterceptor

    begin.head.outgoing = new TestSendInterceptor( (frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      frame match {
        case t:AMQPTransportFrame =>
          t.getPerformative match {
            case b:Begin =>
              Option(b.getRemoteChannel) match {
            case Some(c) =>
              fail("Remote channel shouldn't be set")
            case None =>
              }
          }
      }
    })

    begin.tail.incoming = new FrameDroppingInterceptor
    begin.head.outgoing = new TaskExecutingInterceptor
    begin.send_begin
  }

  test("Begin a session remotely") {

    val begin = new BeginInterceptor

    new TestSendInterceptor( (frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      frame match {
        case t:AMQPTransportFrame =>
          t.getPerformative match {
            case b:Begin =>
              Option(b.getRemoteChannel) match {
                case Some(c) =>
                  c should be (7)
                case None =>
                  fail("Remote channel should be set")
              }
          }
      }
    }).incoming = begin

    begin.head.outgoing = new TaskExecutingInterceptor
    begin.tail.incoming = new FrameDroppingInterceptor

    begin.head.receive(new AMQPTransportFrame(7, new Begin), Tasks())
    begin.send_begin
  }

}