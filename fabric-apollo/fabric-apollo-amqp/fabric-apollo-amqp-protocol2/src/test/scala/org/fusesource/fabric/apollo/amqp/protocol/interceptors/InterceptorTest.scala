package org.fusesource.fabric.apollo.amqp.protocol.interceptors

import org.apache.activemq.apollo.util.FunSuiteSupport
import org.scalatest.matchers.ShouldMatchers
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.commands.SimpleFrame
import sun.tools.tree.NewInstanceExpression
import test_interceptors._

/**
 *
 */

class InterceptorTest extends FunSuiteSupport with ShouldMatchers {

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

    in.receive(new SimpleFrame, new Queue[() => Unit])
    got_here should be (true)

    got_here = false
    in.incoming.incoming.incoming.remove
    in.incoming.incoming.remove

    in.receive(new SimpleFrame, new Queue[() => Unit])
    got_here should be (true)

  }

}