package org.fusesource.fabric.apollo.amqp.protocol.interceptors.session

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{FunSuiteSupport, Logging}
import org.fusesource.fabric.apollo.amqp.protocol.AMQPSession
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Tasks
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import org.fusesource.fabric.apollo.amqp.codec.types._
import org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors.FrameDroppingInterceptor


/**
 *
 */



class LinkManagementTest extends FunSuiteSupport with ShouldMatchers with Logging {
  implicit def source2source(s:Source) = s.asInstanceOf[org.fusesource.fabric.apollo.amqp.codec.interfaces.Source]
  implicit def target2target(t:Target) = t.asInstanceOf[org.fusesource.fabric.apollo.amqp.codec.interfaces.Target]

  test("Create session, send attach frame and cause link to be created") {
    val session = new AMQPSession
    session.head.outgoing = new FrameDroppingInterceptor
    val attach = new Attach("foo", 0L, Role.SENDER.getValue, SenderSettleMode.SETTLED.getValue, ReceiverSettleMode.FIRST.getValue, new Source(), new Target())
    session.head.incoming.receive(new AMQPTransportFrame(attach), Tasks())

  }

}