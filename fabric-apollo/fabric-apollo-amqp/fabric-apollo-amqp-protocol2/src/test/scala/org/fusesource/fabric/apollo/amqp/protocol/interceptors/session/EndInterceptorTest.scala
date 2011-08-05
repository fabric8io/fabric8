/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors.session

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.codec.types.{End, AMQPTransportFrame}
import org.fusesource.fabric.apollo.amqp.protocol.utilities._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.FrameInterceptor
import org.fusesource.fabric.apollo.amqp.protocol.interceptors.test_interceptors.{SimpleInterceptor, TaskExecutingInterceptor, FailInterceptor, TestSendInterceptor}
import org.fusesource.fabric.apollo.amqp.protocol.commands.{EndSent, ReleaseChain}

/**
 *
 */

class EndInterceptorTest extends FunSuiteSupport with ShouldMatchers with Logging {

  test("Create end interceptor, throw exception, see that end frame is sent") {
    val end = new EndInterceptor
    new SimpleInterceptor().outgoing = end
    end.head.outgoing = new TestSendInterceptor((frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      frame match {
        case t:AMQPTransportFrame =>
          t.getPerformative match {
            case e:End =>
          }
        case r:ReleaseChain =>
      }
    })
    end.head.outgoing = new TaskExecutingInterceptor
    end.tail.incoming = new FrameInterceptor[EndSent] {
      override protected def receive_frame(e:EndSent, tasks:Queue[() => Unit]) = {
        // filtering this out
      }
    }
    end.tail.incoming = new FailInterceptor
    end.head.receive(new AMQPTransportFrame, Tasks())
    end.sent should be (true)
  }

}