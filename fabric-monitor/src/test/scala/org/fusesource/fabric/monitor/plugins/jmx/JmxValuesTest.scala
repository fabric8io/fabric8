package org.fusesource.fabric.monitor
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