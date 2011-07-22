/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import test_interceptors._
import org.fusesource.fabric.apollo.amqp.codec.types.AMQPProtocolHeader
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor
import org.fusesource.fabric.apollo.amqp.protocol.commands.{ConnectionCreated, HeaderSent}

/**
 *
 */
class HeaderInterceptorTest extends FunSuiteSupport with ShouldMatchers with Logging {

  def createInterceptorChain = {
    val dummy_in = new TestSendInterceptor((frame: AMQPFrame, tasks: Queue[() => Unit]) => {
      frame.isInstanceOf[AMQPProtocolHeader] should be(true)
    })

    val header_interceptor = new HeaderInterceptor

    dummy_in.outgoing = new TaskExecutingInterceptor
    dummy_in.incoming = header_interceptor

    header_interceptor.incoming = new Interceptor {
      def send(frame: AMQPFrame, tasks: Queue[() => Unit]) = outgoing.send(frame, tasks)

      def receive(frame: AMQPFrame, tasks: Queue[() => Unit]) = {
        frame match {
          case h:HeaderSent =>
          case _ =>
            incoming.receive(frame, tasks)
        }
      }
    }

    header_interceptor.incoming.incoming = new TerminationInterceptor

    val tasks = new Queue[() => Unit]
    (dummy_in, tasks)
  }

  test("Create interceptor, send header to it") {
    val (dummy_in, tasks) = createInterceptorChain
    dummy_in.receive(ConnectionCreated.INSTANCE, tasks)

    dummy_in.incoming.incoming.isInstanceOf[TerminationInterceptor] should be (true)
    tasks should be ('empty)
  }

  test("Create interceptor, send wrong version to it") {
    val (dummy_in, tasks) = createInterceptorChain
    val bad = new AMQPProtocolHeader
    bad.revision = 1.asInstanceOf[Short]
    try {
      dummy_in.receive(bad, tasks)
      fail("Should have receieved exception")
    } catch {
      case r:RuntimeException =>

      case _ =>
        fail
    }
    tasks should be ('empty)
  }


}