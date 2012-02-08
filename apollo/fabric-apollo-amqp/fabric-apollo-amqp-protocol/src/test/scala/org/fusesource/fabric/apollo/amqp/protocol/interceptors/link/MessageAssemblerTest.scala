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

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.link

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.FunSuiteSupport
import org.fusesource.fabric.apollo.amqp.codec.api.MessageFactory
import org.fusesource.fabric.apollo.amqp.codec.marshaller.MessageSupport
import org.fusesource.hawtbuf.Buffer._
import collection.mutable.{Queue, ListBuffer}
import org.fusesource.hawtbuf.Buffer
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.{PerformativeInterceptor, FrameInterceptor}
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Tasks
import org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors.SimpleInterceptor

/**
 *
 */

class MessageAssemblerTest extends FunSuiteSupport with ShouldMatchers {

  test("Create message, fragment it and re-assemble it") {

    val fragments = ListBuffer[AMQPTransportFrame]()

    val message = MessageFactory.createDataMessage(ascii("Hello world from AMQP land..."))
    val annotated_message = MessageFactory.createAnnotatedMessage(new Properties(new AMQPLong(0L), ascii("username"), new AMQPString("somewhere"), "Hello there!", new AMQPString("somewhere-else")), message)
    val payload:Buffer = MessageSupport.toBuffer(annotated_message)

    val num_fragments = 3
    val fragment_size:Long = payload.length() / num_fragments

    val fragmenter = new MessageFragmenter
    fragmenter.max_message_size = fragment_size

    fragmenter.head.outgoing = new FrameInterceptor[AMQPTransportFrame] {
      override protected def send_frame(t:AMQPTransportFrame, tasks:Queue[() => Unit]) = {
        fragments.append(t)
      }
    }

    fragmenter.send(new AMQPTransportFrame(new Transfer(0L, 0L, ascii("foo"), 0L), payload), Tasks())

    var assembled:Buffer = null

    val assembler = new MessageAssembler
    val head = new SimpleInterceptor
    head.incoming = assembler

    assembler.tail.incoming = new PerformativeInterceptor[Transfer] {
      override protected def receive(t:Transfer, payload:Buffer, tasks:Queue[() => Unit]) = {
        assembled = payload
        true
      }
    }

    fragments.foreach((x) => head.receive(x, Tasks()))

    payload.equals(assembled) should be (true)
  }

}