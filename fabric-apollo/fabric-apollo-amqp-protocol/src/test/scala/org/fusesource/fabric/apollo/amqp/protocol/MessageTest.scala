/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.protocol

import org.apache.activemq.apollo.util.FunSuiteSupport
import org.scalatest.matchers.ShouldMatchers
import org.fusesource.hawtbuf._
import org.fusesource.fabric.apollo.amqp.api.DataMessage

/**
 *
 */

class MessageTest extends FunSuiteSupport with ShouldMatchers {

  def create_message = {
    val message = new DataMessageImpl
    message.getHeader.setDurable(true)
    message.getProperties.setUserId(Buffer.ascii("foo"))
    message.setBody(Buffer.ascii("Hello world!"))
    message
  }

  test("Create message, fragment it") {
    val message = create_message
    val big_fragments = message.marshal_to_amqp_fragments(0)
    big_fragments.size should be (3)
    val little_fragments = message.marshal_to_amqp_fragments(3)
    little_fragments.size should be > (3)
  }

  test("Create message, fragment it, assemble new message from fragments") {
    val message = create_message
    val big_fragments = message.marshal_to_amqp_fragments(0)
    val new_message = AmqpMessageFactory.create_from_fragments(big_fragments).asInstanceOf[DataMessageImpl]

    //println("\n\nOld: " + message + "\n\nNew:" + new_message)

    new_message.getHeader should be (message.getHeader)
    new_message.getProperties should be (message.getProperties)
    new_message.getBody.compareTo(message.getBody) should be (0)

    val little_fragments = message.marshal_to_amqp_fragments(3)
    val new_message2 = AmqpMessageFactory.create_from_fragments(little_fragments).asInstanceOf[DataMessageImpl]

    //println("\n\nOld: " + message + "\n\nNew:" + new_message2)

    new_message2.getHeader should be (message.getHeader)
    new_message2.getProperties should be (message.getProperties)
    new_message2.getBody.compareTo(message.getBody) should be (0)

  }

}