package org.fusesource.fabric.monitor
package plugins
package jmx

import java.io.File
import org.fusesource.scalate.util.Measurements._
import api.Value

/**
 * Test the Values API
 */
class JmxValuesTest extends FunSuiteSupport {
  test("lookup simple values") {
    val v = Value("java.lang:type=Threading", "ThreadCount")
    val threadCount = v.get

    println("value " + v + " = " + threadCount)
  }

  test("lookup nested values") {
    val v = Value("java.lang:type=Memory", "HeapMemoryUsage")
    val used = v("used").get
    val init = v("init").get
    val max = v("max").get

    println("value " + v + " has used: " + byte(used) + " init: " + byte(init) + " max: " + byte(max))
  }
}