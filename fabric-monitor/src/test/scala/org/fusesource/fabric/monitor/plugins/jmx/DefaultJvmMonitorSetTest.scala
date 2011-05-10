package org.fusesource.fabric.monitor.plugins
package jmx

import collection.JavaConversions._
import org.fusesource.fabric.monitor.api.DataSourceDTO

class DefaultJvmMonitorSetTest extends FunSuiteSupport {

  val verbose = false

  test("Default JVM Monitor set") {
    val set = new DefaultJvmMonitorSetBuilder().apply()
    if (verbose) {
      for (ds <- set.data_sources) {
        println("DataSource: " + ds)
      }
    }

    // now lets poll
    val pollerFactory = new JmxPollerFactory()
    for (ds <- set.data_sources) {

      val poller = pollerFactory.create(ds)
      val value = poller.poll

      poller.close
      // TODO have a close on the PollerFactory?

      if (verbose) {
        println(ds.id + ": " + value)
        //assert(value != Double.NaN && value > 0, "Invalid thread count: " + value)
      }
    }
  }


  def dump(d: DataSourceGroup, indent: Int, concise: Boolean) {
    printIndent(indent)
    println(if (concise) d.id else d)
    val newIndent = indent + 1
    for (child <- d.children) {
      dump(child, newIndent, concise)
    }
    for (ds <- d.data_sources) {
      dump(ds, newIndent, concise)
    }
  }

  def dump(d: DataSourceDTO, indent: Int, concise: Boolean) {
    printIndent(indent)
    println(if (concise) d.id else d)
  }

  def printIndent(indent: Int) {
    for (i <- 0.to(indent)) {
      print("  ")
    }
  }

  test("query JMX beans") {
    val registry = new JmxDataSourceRegistry()

    val answer = registry.findSources()
    for ((d, a) <- answer) {
      dump(a, 0, true)
    }
  }
}