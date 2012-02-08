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

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.common

import org.fusesource.hawtdispatch._
import org.apache.activemq.apollo.util.FunSuiteSupport
import org.apache.activemq.apollo.util.Logging
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import org.scalatest.matchers.ShouldMatchers
import scala.collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.types.{End, AMQPTransportFrame, Begin}
import org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors._
import org.fusesource.fabric.apollo.amqp.protocol.utilities._
import org.fusesource.fabric.apollo.amqp.protocol.commands.{ChainReleased, ReleaseChain}
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.{FrameInterceptor, Interceptor}

/**
 *
 */
class MultiplexerTest extends FunSuiteSupport with ShouldMatchers with Logging {

  test("Create multiplexer, create some chains on the fly, remove a chain") {

    val multiplexer = new Multiplexer
    multiplexer.channel_selector = Option((frame:AMQPTransportFrame) => frame.getChannel)
    multiplexer.channel_mapper = Option((frame:AMQPTransportFrame) => None)
    multiplexer.outgoing_channel_setter = Option((channel:Int, frame:AMQPTransportFrame) => frame.setChannel(channel))

    var instances = 0
    var saved:Interceptor = null

    multiplexer.chain_attached = Option((chain:Interceptor) => {
      instances = instances + 1
      if (instances == 2) {
        saved = chain
      }
    })

    multiplexer.chain_released = Option((chain:Interceptor) => {
      instances = instances - 1
    })

    multiplexer.interceptor_factory = Option((frame:AMQPTransportFrame) => {
      val rc = new FrameLoggingInterceptor("Chain instance " + instances)
      rc.tail.incoming = new TerminationInterceptor
      rc
    })

    multiplexer.head.outgoing = new FrameLoggingInterceptor("Outgoing")
    multiplexer.head.outgoing = new TaskExecutingInterceptor

    multiplexer.head.receive(new AMQPTransportFrame(0, new Begin(0)), Tasks())
    multiplexer.head.receive(new AMQPTransportFrame(1, new Begin(1)), Tasks())
    multiplexer.head.receive(new AMQPTransportFrame(2, new Begin(2)), Tasks())

    instances should be (3)

    // now remove one
    saved.tail.remove
    // filter out this message
    saved.tail.incoming = new FrameInterceptor[ChainReleased] {
      override protected def receive_frame(c:ChainReleased, tasks:Queue[() => Unit]) = {}
    }
    saved.tail.incoming = new FailInterceptor

    multiplexer.release(saved.head)
    multiplexer.head.receive(new AMQPTransportFrame(0, new Begin(0)), Tasks())
    multiplexer.head.receive(new AMQPTransportFrame(1, new Begin(1)), Tasks())
    multiplexer.head.receive(new AMQPTransportFrame(2, new Begin(2)), Tasks())

    instances should be (3)
  }

  test("Add and release interceptors") {
    val multiplexer = new Multiplexer

    multiplexer.channel_selector = Option((frame:AMQPTransportFrame) => frame.getChannel)

    var instances = 0

    multiplexer.chain_attached = Option((chain:Interceptor) => {
      info("Attaching chain : %s", chain)
      instances = instances + 1
    })

    multiplexer.chain_released = Option((chain:Interceptor) => {
      info("Releasing chain : %s", chain)
      instances = instances - 1
    })

    multiplexer.outgoing_channel_setter = Option((channel:Int, frame:AMQPTransportFrame) => frame.setChannel(channel))

    multiplexer.channel_mapper = Option((frame:AMQPTransportFrame) => {
      frame.getPerformative match {
        case b:Begin =>
          Option(b.getRemoteChannel) match {
            case Some(i) =>
              Option(i.intValue)
            case None =>
              None
          }
        case _ =>
          None
      }
    })

    multiplexer.head.outgoing = new TestSendInterceptor( (frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      frame match {
        case t:AMQPTransportFrame =>
          t.getChannel should be (5)
      }
    })
    multiplexer.outgoing = new TaskExecutingInterceptor

    val chain = new FrameLoggingInterceptor("My chain")
    chain.incoming = new TerminationInterceptor
    multiplexer.attach(chain)

    multiplexer.head.receive(new AMQPTransportFrame(5, new Begin(0)), Tasks())

    instances should be (1)
  }

  test("Release interceptor via frame") {
    val multiplexer = new Multiplexer

    multiplexer.channel_selector = Option((frame:AMQPTransportFrame) => frame.getChannel)

    var instances = 0

    multiplexer.chain_attached = Option((chain:Interceptor) => {
      info("Attaching chain : %s", chain)
      instances = instances + 1
    })

    multiplexer.chain_released = Option((chain:Interceptor) => {
      info("Releasing chain : %s", chain)
      instances = instances - 1
    })

    multiplexer.outgoing_channel_setter = Option((channel:Int, frame:AMQPTransportFrame) => frame.setChannel(channel))

    multiplexer.channel_mapper = Option((frame:AMQPTransportFrame) => {
      frame.getPerformative match {
        case b:Begin =>
          Option(b.getRemoteChannel) match {
            case Some(i) =>
              Option(i.intValue)
            case None =>
              None
          }
        case _ =>
          None
      }
    })

    multiplexer.head.outgoing = new TestSendInterceptor( (frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      frame match {
        case t:AMQPTransportFrame =>
          t.getChannel should be (0)
      }
    })
    multiplexer.outgoing = new TaskExecutingInterceptor

    val chain = new FrameLoggingInterceptor("My chain")
    chain.incoming = new FrameDroppingInterceptor
    multiplexer.attach(chain)

    multiplexer.head.receive(new AMQPTransportFrame(5, new Begin(0)), Tasks())

    instances should be (1)

    chain.send(new AMQPTransportFrame(new End), Tasks())
    chain.send(ReleaseChain(), Tasks())

    instances should be(0)
  }

  test("set dispatch queue on multiplexer, ensure chains have dispatch queue set on them") {
    val multiplexer = new Multiplexer
    multiplexer.chain_attached = Option((chain:Interceptor) => {
      info("Attaching chain : %s", chain)
    })

    multiplexer.chain_released = Option((chain:Interceptor) => {
      info("Releasing chain : %s", chain)
    })

    multiplexer.outgoing_channel_setter = Option((channel:Int, frame:AMQPTransportFrame) => frame.setChannel(channel))

    multiplexer.queue_set should be (false)

    multiplexer.attach(new FrameDroppingInterceptor)
    multiplexer.attach(new FrameDroppingInterceptor)
    multiplexer.attach(new FrameDroppingInterceptor)
    multiplexer.attach(new FrameDroppingInterceptor)
    multiplexer.attach(new FrameDroppingInterceptor)

    multiplexer.foreach_chain((x) => x.queue_set should be (false))

    multiplexer.queue = Dispatch.createQueue

    multiplexer.queue_set should be (true)
    multiplexer.foreach_chain((x) => x.queue_set should be (true))
  }


}
