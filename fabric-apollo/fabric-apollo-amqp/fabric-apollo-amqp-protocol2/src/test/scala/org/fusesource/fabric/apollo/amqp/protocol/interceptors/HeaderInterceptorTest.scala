package org.fusesource.fabric.apollo.amqp.protocol.interceptors

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPProtocolHeader

/**
 *
 */
class HeaderInterceptorTest extends FunSuiteSupport with ShouldMatchers with Logging {

  test("Create interceptor, send header to it") {

    val dummy_in = new TestSendInterceptor( (frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      frame.isInstanceOf[AMQPProtocolHeader] should be(true)
    })

    val header_interceptor = new HeaderInterceptor

    dummy_in.outgoing = new TaskExecutingInterceptor
    dummy_in.incoming = header_interceptor

    header_interceptor.incoming = new TerminationInterceptor

    val tasks = new Queue[() => Unit]
    dummy_in.receive(new AMQPProtocolHeader(), tasks)

    dummy_in.incoming.isInstanceOf[TerminationInterceptor] should be (true)
    tasks should be ('empty)
  }


}