package org.fusesource.fabric.monitor.plugins
package jmx

import collection.JavaConversions._

class DefaultJvmMonitorSetTest extends FunSuiteSupport {

  test("Default JVM Monitor set") {
    val set = new DefaultJvmMonitorSetBuilder().apply()
    for (ds <- set.data_sources) {
      println("DataSource: " + ds)
    }

    // now lets poll
    val pollerFactory = new JmxPollerFactory()
    for (ds <- set.data_sources) {

      val poller = pollerFactory.create(ds)
      val value = poller.poll

      poller.close
      // TODO have a close on the PollerFactory?

      println(ds.id + ": " + value)
      //assert(value != Double.NaN && value > 0, "Invalid thread count: " + value)
    }
  }


  ignore("query JMX beans") {
    val registry = new JmxDataSourceRegistry()

    val answer = registry.findSources()
    for (a <- answer) {
      println("Found: " + a)
    }
  }
}