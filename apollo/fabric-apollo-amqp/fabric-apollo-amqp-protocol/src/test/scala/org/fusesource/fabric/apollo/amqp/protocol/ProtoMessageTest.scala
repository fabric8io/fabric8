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

import java.util.UUID
import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.FunSuiteSupport
import org.fusesource.hawtbuf.Buffer
import org.fusesource.hawtdispatch.Dispatch
import java.util.concurrent.{TimeUnit, CountDownLatch}

/**
 *
 */
class ProtoMessageTest extends FunSuiteSupport with ShouldMatchers {
  /*

  test("Create message with one body element, marshal/unmarshal") {
    val message = AmqpProtoMessage.create
    message.tag = createAmqpDeliveryTag(Buffer.ascii(UUID.randomUUID.toString).buffer)
    message.addBodyPart(new Buffer("Hello world!".getBytes))


    println("Created message " + message)

    val transfer = message.transfer(0)
    val new_transfer = marshalUnmarshal(transfer)
    val new_message = new AmqpProtoMessage(new_transfer)

    println("Got message " + new_message)

    new_message.tag should be (message.tag)
    new_message.getBodyPart(0) should be (message.getBodyPart(0))
  }

  test("Create message, marshal and unmarshal, check equivalence") {
    val in = AmqpProtoMessage.create("My tag")
    in.settled = true
    in.header.setDurable(true)
    in.footer.setMessageAttrs(createAmqpMessageAttributes)
    in.footer.getMessageAttrs.put(createAmqpString("test"), createAmqpString("value"))
    in.addBodyPart("Hello world!".getBytes)

    println("Created message " + in)

    val buf = in.toBuffer
    val out = AmqpProtoMessage.fromBuffer(buf)

    println("Got message " + out)

    out.settled should be (in.settled)
    out.batchable should be (in.batchable)
    out.header should be (in.header)
    out.footer should be (in.footer)
    out.getBodyPart(0) should be (in.getBodyPart(0))
  }

  test("Performance of message marshalling/unmarshalling") {

    val max = 1000000
    var iters = 0

    val queue = Dispatch.createQueue
    val latch = new CountDownLatch(1)

    queue {
      for (i <- 1 to max ) {
        val in = AmqpProtoMessage.create
        in.settled = true
        in.addBodyPart(("Message " + i).getBytes)

        val in_transfer = in.transfer(i + 1)
        val out_transfer = marshalUnmarshal(in_transfer)

        val out = AmqpProtoMessage.create(out_transfer)
        iters = iters + 1
      }
      latch.countDown
    }

    var last = 0
    while (iters < max) {
      latch.await(5, TimeUnit.SECONDS)
      val diff = iters - last
      last = last + diff
      println("msg/s - " + (diff / 5))
    }
  }
*/
}
