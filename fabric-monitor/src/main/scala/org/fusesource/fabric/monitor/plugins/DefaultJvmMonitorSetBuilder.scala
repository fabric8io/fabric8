package org.fusesource.fabric.monitor.plugins

import org.fusesource.fabric.monitor.api.{PollDTO, DataSourceDTO, MonitoredSetDTO}

/**
 * Builds the default monitor set for a typical JVM
 */
class DefaultJvmMonitorSetBuilder extends MonitorSetBuilder("Default JVM Statistics") {

  def configure {
    jmxDataSource("java.lang:type=Memory", "HeapMemoryUsage", "used")
    jmxDataSource("java.lang:type=Threading", "ThreadCount")
  }

}