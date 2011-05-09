package org.fusesource.fabric.monitor.plugins

/**
 * Builds the default monitor set for a typical JVM
 */
class DefaultJvmMonitorSetBuilder extends MonitorSetBuilder("jvm-default") {

  def configure {

    archive("5m", "1s")  // 300 data points
    archive("24h", "2m") // 720 data points
    archive("7d", "30m") // 336 data points
    archive("30d", "1h") // 720 data points
    archive("1y", "24h") // 365 data points..

    //
    // The heap spaces.
    //
    jmxDataSource("java.lang:name=CMS Old Gen,type=MemoryPool", "Usage", "used")
    jmxDataSource("java.lang:name=Par Survivor Space,type=MemoryPool", "Usage", "used")
    jmxDataSource("java.lang:name=Par Eden Space,type=MemoryPool", "Usage", "used")
    jmxDataSource("java.lang:type=Memory", "HeapMemoryUsage", "used")
    jmxDataSource("java.lang:type=Memory", "HeapMemoryUsage", "committed")

    //
    // Non heap spaces.
    //
    jmxDataSource("java.lang:name=CMS Perm Gen,type=MemoryPool", "Usage", "used")
    jmxDataSource("java.lang:name=Code Cache,type=MemoryPool", "Usage", "used")
    jmxDataSource("java.lang:type=Memory", "NonHeapMemoryUsage", "used")
    jmxDataSource("java.lang:type=Memory", "NonHeapMemoryUsage", "committed")

    // other stuff
    jmxDataSource("java.lang:type=Threading", "ThreadCount")
    jmxDataSource("java.lang:type=OperatingSystem", "OpenFileDescriptorCount")
  }

}