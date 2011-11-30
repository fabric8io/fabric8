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

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{FunSuiteSupport, Logging}
import org.fusesource.fabric.apollo.amqp.api.AmqpConnectionFactory
import org.fusesource.hawtdispatch._
import java.util.concurrent.{TimeUnit, CountDownLatch}

/**
 *
 */

class SSLConnectionTest extends FunSuiteSupport with ShouldMatchers with Logging {

  // TODO
  ignore("Create SSL server and connect to it") {
    val server = new BaseTestServer
    server.bind("tls://localhost:0")

    val latch = new CountDownLatch(1)

    val client = AmqpConnectionFactory.create
    client.connect("tls://localhost:" + server.getListenPort, ^{
      latch.countDown
    })

    latch.await(10, TimeUnit.SECONDS) should be (true)
  }
}
