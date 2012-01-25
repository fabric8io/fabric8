/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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