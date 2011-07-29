/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.connection

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{FunSuiteSupport, Logging}
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.types.{AMQPTransportFrame, Open}
import org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors.{FrameDroppingInterceptor, TaskExecutingInterceptor, TestReceiveInterceptor, TestSendInterceptor}
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Tasks
import org.fusesource.fabric.apollo.amqp.protocol.commands.{SendOpen, OpenReceived, OpenSent}

/**
 *
 */

class OpenInterceptorTest extends FunSuiteSupport with ShouldMatchers with Logging {

  test("Create open interceptor, configure open frame, send down interceptor chain and check it out") {

    val open = new Open("MyContainer", "localhost", 1024L, 10, 5000)

    val open_interceptor = new OpenInterceptor
    open_interceptor.open.setContainerID("SomeContainer")
    open_interceptor.open.setIdleTimeout(3000L)

    open_interceptor.outgoing = new TestSendInterceptor((frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      frame match {
        case f:AMQPTransportFrame =>
          val p:Object = f.getPerformative
          p match {
            case o:Open =>
              o.getContainerID should be ("SomeContainer")
              o.getIdleTimeout should be (3000L)
            case _ =>
              fail("Performative should be an Open frame")
          }
        case _ =>
          fail("Frame should be an AMQPTransportFrame")
      }
    })

    open_interceptor.outgoing.outgoing = new TaskExecutingInterceptor

    open_interceptor.incoming = new TestReceiveInterceptor((frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      frame match {
        case o:OpenReceived =>
        case o:OpenSent =>
        case _ =>
          fail("Should not have received frame " + frame)
      }
    })
    open_interceptor.incoming.incoming = new FrameDroppingInterceptor

    open_interceptor.connected should be (true)

    open_interceptor.outgoing.outgoing.receive(new AMQPTransportFrame(open), Tasks())

    open_interceptor.peer.getContainerID should  be ("MyContainer")
    open_interceptor.peer.getHostname should be ("localhost")
    open_interceptor.peer.getMaxFrameSize should be (1024L)
    open_interceptor.peer.getChannelMax should be (10)
    open_interceptor.peer.getIdleTimeout should be (5000)

  }

}