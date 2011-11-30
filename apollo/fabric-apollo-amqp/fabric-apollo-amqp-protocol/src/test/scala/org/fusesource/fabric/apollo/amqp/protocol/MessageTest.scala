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
import org.fusesource.fabric.apollo.amqp.codec.types.TypeFactory._
import org.fusesource.fabric.apollo.amqp.codec.CodecUtils
import org.fusesource.fabric.apollo.amqp.codec.types.{AmqpFragment, AmqpList}

/**
 *
 */

class MessageTest extends FunSuiteSupport with ShouldMatchers {
  /*

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
    val little_fragments = message.marshal_to_amqp_fragments(2)
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

    val little_fragments = message.marshal_to_amqp_fragments(2)
    val new_message2 = AmqpMessageFactory.create_from_fragments(little_fragments).asInstanceOf[DataMessageImpl]

    //println("\n\nOld: " + message + "\n\nNew:" + new_message2)

    new_message2.getHeader should be (message.getHeader)
    new_message2.getProperties should be (message.getProperties)
    new_message2.getBody.compareTo(message.getBody) should be (0)
  }

  for ( frag_size <- List(0, 1, 2, 3, 4, 5) ) {
    test("Create message, put it into a transfer (frag_size=" + frag_size + "), marshal/unmarshal and then re-assemble") {
      val message = create_message
      val in = createAmqpTransfer
      in.setTransferId(0L)
      in.setDeliveryTag(Buffer.ascii(UUID.randomUUID.toString))
      in.setHandle(0)
      in.setFragments(message.marshal_to_multiple(frag_size))

      val out = CodecUtils.marshalUnmarshal(in)

      //println("in : " + in + "\n\n out : " + out)

      out should be (in)

      val new_message = AmqpMessageFactory.create_from_multiple(out.getFragments).asInstanceOf[DataMessageImpl]

      //println("in : " + message + "\n\n out : " + new_message)

      new_message.getHeader should be (message.getHeader)
      new_message.getProperties should be (message.getProperties)
      new_message.getBody.compareTo(message.getBody) should be (0)
    }
  }
*/
}