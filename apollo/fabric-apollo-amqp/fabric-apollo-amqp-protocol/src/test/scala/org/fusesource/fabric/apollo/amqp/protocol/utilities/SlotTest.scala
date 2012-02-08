/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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