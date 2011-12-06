/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
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