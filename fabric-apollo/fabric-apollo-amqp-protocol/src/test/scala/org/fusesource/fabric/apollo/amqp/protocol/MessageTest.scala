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

/**
 *
 */

class MessageTest extends FunSuiteSupport with ShouldMatchers {

  test("Create message, fragment it") {
    val message = new DataMessageImpl
    message.getHeader.setDurable(true)
    message.getProperties.setUserId(Buffer.ascii("foo"))
    message.setBody(Buffer.ascii("Hello world!"))

    val big_fragments = message.marshal_to_amqp_fragments(0)
    big_fragments.size should be (3)
    val little_fragments = message.marshal_to_amqp_fragments(3)
    little_fragments.size should be > (3)
  }

}