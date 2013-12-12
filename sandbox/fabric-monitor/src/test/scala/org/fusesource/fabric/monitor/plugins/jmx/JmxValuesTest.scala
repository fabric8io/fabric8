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
package io.fabric8.monitor
package plugins
package jmx

import org.fusesource.scalate.util.Measurements._
import internal.Value

/**
 * Test the Values API
 */
class JmxValuesTest extends FunSuiteSupport {
  protected def dumpValue(v: Value): Unit = {
    println("value name: " + v.name + " description: " + v.description)
  }

  ignore("lookup simple values") {
    val v = Value("java.lang:type=Threading", "ThreadCount")
    val threadCount = v.get

    dumpValue(v)
    println("value " + v + " = " + threadCount)
    assertDefined(v)
  }

  test("lookup nested values") {
    val v = Value("java.lang:type=Memory", "HeapMemoryUsage")
    val used = v("used").get
    val init = v("init").get
    val max = v("max").get

    dumpValue(v)
    println("value " + v + " has used: " + byte(used) + " init: " + byte(init) + " max: " + byte(max))
    assertDefined(v)
  }

  ignore("Non Existent objects, attributes and keys") {
    assertDefined(Value("java.lang:type=Memory", "HeapMemoryUsage", "shouldNotExist"), false)
    assertDefined(Value("java.lang:type=Memory", "ShouldNotExist"), false)
    assertDefined(Value("java.lang:type=NoSuchThing", "ShouldNotExist"), false)
  }

  protected def assertDefined(v: Value, expected: Boolean = true): Unit = {
    expect(expected, "isDefined for " + v) {
      v.isDefined
    }
  }
}