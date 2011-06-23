package org.fusesource.fabric.monitor.plugins
package jmx

import collection.JavaConversions._
import org.fusesource.fabric.api.monitor.DataSourceDTO
import org.fusesource.fabric.api.monitor.DataSourceDTO

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

  test("query JMX beans") {
    val registry = new JmxDataSourceRegistry()

    val answer = registry.findSources()
    for ((d, a) <- answer) {
      a.dump(0, true)
    }
  }
}