package org.fusesource.fabric.monitor.plugins

/**
 * Builds the default monitor set for a typical JVM
 */
class DefaultJvmMonitorSetBuilder extends MonitorSetBuilder("Default JVM Statistics") {

  def configure {
    jmxDataSources(
      // heap
      "java.lang:name=Par Survivor Space,type=MemoryPool/Usage/used",
      "java.lang:name=CMS Old Gen,type=MemoryPool/Usage/used",
      "java.lang:name=Par Eden Space,type=MemoryPool/Usage/used",

      // non heap
      "java.lang:name=CMS Perm Gen,type=MemoryPool/Usage/used",
      "java.lang:name=Code Cache,type=MemoryPool/Usage/used",

      // memory summaries
      "java.lang:type=Memory/HeapMemoryUsage/used",
      "java.lang:type=Memory/NonHeapMemoryUsage/used",

      // other stuff
      "java.lang:type=Threading/ThreadCount"
    )
  }

}