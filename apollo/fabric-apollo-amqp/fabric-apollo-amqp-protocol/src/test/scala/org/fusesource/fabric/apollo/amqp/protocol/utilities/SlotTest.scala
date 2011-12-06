/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.utilities

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.{Logging, FunSuiteSupport}

/**
 *
 */
class SlotTest extends FunSuiteSupport with ShouldMatchers with Logging {

  test("Add/remove and then add items") {

    val foo = new Slot[String]
    foo.allocate("Zero")
    foo.allocate("One")
    foo.allocate("Two")
    foo.allocate("Three")

    printf("foo is %s\n", foo)

    foo.free(3)
    foo.free(2)
    printf("foo is %s\n", foo)
    foo.allocate("Four") should be(2)
    printf("foo is %s\n", foo)

    foo.used_slots.foreach((x) => {
      foo.free(x)
    })

    printf("foo is %s\n", foo)

    foo.used_slots() should be('empty)
    foo.available_slots() should not(be('empty))
  }

  test("Create with lower bound") {
    val foo = new Slot[String]
    foo.reserve(4)
    printf("foo is %s\n", foo)
    foo.allocate("Zero") should be (4)
  }

}