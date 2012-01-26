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

package org.fusesource.fabric.apollo.amqp.protocol.interceptors

import org.apache.activemq.apollo.util.FunSuiteSupport
import org.scalatest.matchers.ShouldMatchers
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.commands.SimpleFrame
import test_interceptors._
import org.fusesource.hawtdispatch._
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Tasks
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPTransportFrame
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor._

/**
 *
 */
class InterceptorTest extends FunSuiteSupport with ShouldMatchers {

  test("Test get head/tail of interceptor chain") {
    val middle = new SimpleInterceptor
    middle.incoming = new SimpleInterceptor
    middle.outgoing = new SimpleInterceptor

    val start = middle.outgoing
    val end = middle.incoming

    start.tail should be theSameInstanceAs (end)
    end.head should be theSameInstanceAs (start)
    middle.tail should be theSameInstanceAs (end)
    middle.head should be theSameInstanceAs (start)

    middle.remove

    middle.tail should be theSameInstanceAs  (middle)
    middle.head should be theSameInstanceAs (middle)

    start.tail should be theSameInstanceAs (end)
    end.head should be theSameInstanceAs (start)

  }

  test("Create interceptor chain, remove an interceptor, verify it's disconnected") {
    val middle = new SimpleInterceptor
    middle.incoming = new SimpleInterceptor
    middle.outgoing = new SimpleInterceptor

    val start = middle.outgoing
    val end = middle.incoming

    middle.remove

    start.incoming should be theSameInstanceAs (end)
    end.outgoing should be theSameInstanceAs (start)
    middle.connected should be (false)
  }

  test("Create interceptor chain, send message down it, modify chain, send another message down it") {
    var got_here = false
    val in = new TestSendInterceptor((frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      frame should not be (null)
      frame.isInstanceOf[SimpleFrame] should be (true)
      got_here = true
    })

    in.outgoing = new TaskExecutingInterceptor
    in.incoming = new SimpleInterceptor
    in.incoming.incoming = new SimpleInterceptor
    in.incoming.incoming.incoming = new SimpleInterceptor
    in.incoming.incoming.incoming.incoming = new TerminationInterceptor

    in.receive(new SimpleFrame, Tasks())
    got_here should be (true)

    got_here = false
    in.incoming.incoming.incoming.remove
    in.incoming.incoming.remove

    in.receive(new SimpleFrame, Tasks())
    got_here should be (true)
  }

  test("Create interceptor chains, send message, add another chain, send another message") {
    var received = 0
    var sent = 0
    val in = new TaskExecutingInterceptor
    in.tail.incoming = new TestSendInterceptor((frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      sent = sent + 1
      info("Sent : %s\n", sent)
    })
    in.tail.incoming = new TestReceiveInterceptor((frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      received = received + 1
      info("Received : %s\n", received)
    })
    in.tail.incoming = new TerminationInterceptor

    in.receive(new AMQPTransportFrame(), Tasks())
    sent should be (1)
    received should be (1)

    var received2 = 0
    var sent2 = 0

    val in_two = new SimpleInterceptor
    in_two.tail.incoming = new TestSendInterceptor((frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      sent2 = sent2 + 1
      info("Sent : %s\n", sent2)
    })
    in_two.tail.incoming = new TestReceiveInterceptor((frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      received2 = received2 + 1
      info("Received : %s\n", received2)
    })
    in_two.tail.incoming = new TerminationInterceptor

    in.tail.remove
    in.tail.incoming = in_two

    in.receive(new AMQPTransportFrame(), Tasks())
    sent should be (2)
    received should be (2)
    sent2 should be (1)
    received2 should be (1)
  }

  test("Create interceptor chain, send message, add interceptor, send another message") {
    var received = 0
    var sent = 0
    val in = new TaskExecutingInterceptor
    in.tail.incoming = new TestSendInterceptor((frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      sent = sent + 1
      info("Sent : %s\n", sent)
    })
    in.tail.incoming = new SimpleInterceptor
    in.tail.incoming = new SimpleInterceptor
    in.tail.incoming = new SimpleInterceptor
    in.tail.incoming = new TestReceiveInterceptor((frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      received = received + 1
      info("Received : %s\n", received)
    })
    in.tail.incoming = new TerminationInterceptor
    info("Created chain - %s", display_chain(in))
    in.receive(new AMQPTransportFrame(), Tasks())
    received should be (1)
    sent should be (1)
    in.incoming.incoming.after(new SimpleInterceptor)
    info("Chain is now - %s", display_chain(in))
    in.receive(new AMQPTransportFrame(), Tasks())
    received should be (2)
    sent should be (2)

    val new_chain = new SimpleInterceptor
    new_chain.tail.incoming = new SimpleInterceptor
    new_chain.tail.incoming = new SimpleInterceptor
    in.incoming.incoming.after(new_chain)
    info("Chain is now - %s", display_chain(in))
    in.receive(new AMQPTransportFrame(), Tasks())
    received should be (3)
    sent should be (3)
  }

  test("toString") {
    val in = new SimpleInterceptor
    in.tail.incoming = new SimpleInterceptor
    in.tail.incoming = new TerminationInterceptor
    in.tail.incoming = new TaskExecutingInterceptor
    in.tail.incoming = new TerminationInterceptor
    info("Chain is %s", display_chain(in))
  }

  test("set dispatch queue on chain") {
    val in = new SimpleInterceptor
    in.tail.incoming = new SimpleInterceptor
    in.tail.incoming = new SimpleInterceptor

    in.foreach((x) => {x.queue_set should be (false); Unit})

    in.queue = Dispatch.createQueue

    in.foreach((x) => {x.queue_set should be (true); Unit})

    in.tail.incoming = new SimpleInterceptor

    in.foreach((x) => {x.queue_set should be (true); Unit})

    val middle = in.incoming.incoming
    middle.remove
    in.foreach((x) => {x.queue_set should be (true); Unit})
    middle.queue_set should be (false)

    val in2 = new SimpleInterceptor
    in2.tail.incoming = new SimpleInterceptor
    in2.tail.incoming = new SimpleInterceptor

    in2.foreach((x) => {x.queue_set should be (false); Unit})

    in.tail.incoming = in2

    in.foreach((x) => {x.queue_set should be (true); Unit})
    in2.foreach((x) => {x.queue_set should be (true); Unit})
  }

  test("Ensure callbacks work") {
    val in = new SimpleInterceptor

    var amount = 0
    in.incoming = new Interceptor {
      override protected def adding_to_chain = amount = amount + 1
      override protected def removing_from_chain = amount = amount - 1
    }
    amount should be (1)
    in.incoming.remove
    amount should be (0)
    in.after(new Interceptor {
      override protected def adding_to_chain = amount = amount + 1
      override protected def removing_from_chain = amount = amount - 1
    })
    amount should be (1)
    in.incoming.remove
    amount should be (0)
  }

}