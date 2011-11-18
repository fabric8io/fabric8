package org.fusesource.fabric.apollo.amqp.protocol.interceptors.session

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{FunSuiteSupport, Logging}
import org.fusesource.fabric.apollo.amqp.protocol.AMQPSession
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Tasks
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import org.fusesource.fabric.apollo.amqp.codec.types.{AMQPTransportFrame, Role, Attach}


/**
 *
 */
 

class LinkManagementTest extends FunSuiteSupport with ShouldMatchers with Logging {
  
  test("Create session, send attach frame and cause link to be created") {
    val session = new AMQPSession
    session.head.receive(new AMQPTransportFrame(new Attach("foo", 0L, Role.SENDER.getValue)), Tasks())

  }

}