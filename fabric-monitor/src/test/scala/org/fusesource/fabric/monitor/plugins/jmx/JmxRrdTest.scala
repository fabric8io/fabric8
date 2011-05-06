package org.fusesource.fabric.monitor
package plugins
package jmx

import java.io.File
import org.fusesource.fabric.monitor.internal.DefaultMonitor

/**
 * Test the default JVM RRD file generation
 */
class JmxRrdTest extends FunSuiteSupport {
  test("Generate standard JVM RRD tool") {
    val stats_directory = new File("target")
    stats_directory.mkdirs()

    val monitor = new DefaultMonitor(stats_directory.getCanonicalPath + "/")

    monitor.poller_factories = MonitorDeamon.poller_factories

    val monitorSet = new DefaultJvmMonitorSetBuilder().apply()
    monitor.configure(List(monitorSet))

    println("Waiting for some values to be written")
    Thread.sleep(5000L)

    println("Done")
  }
}