package org.fusesource.fabric.monitor.plugins

/**
 * Builds the default monitor set for a typical JVM
 */
class DefaultJvmMonitorSetBuilder extends MonitorSetBuilder("JvmStatistics") {

  def configure {
    archive("5m")
    archive("24h", "1m")
    archive("1y", "1h")

/*
    TODO can we support more than one archive???

    archive("24h", "1m")
    archive("30d", "1h")
    archive("1y", "1d")
*/

    jmxDataSources(
      // heap
      "java.lang:name=Par Survivor Space,type=MemoryPool/Usage/used" -> "jvm.heap.parSurvivor",
      "java.lang:name=CMS Old Gen,type=MemoryPool/Usage/used" -> "jvm.heap.cmsOldGen",
      "java.lang:name=Par Eden Space,type=MemoryPool/Usage/used" -> "jvm.heap.ParEden",

      // non heap
      "java.lang:name=CMS Perm Gen,type=MemoryPool/Usage/used" -> "jvm.nonHeap.cmsPerm",
      "java.lang:name=Code Cache,type=MemoryPool/Usage/used" -> "jvm.nonHeap.code",

      // memory summaries
      "java.lang:type=Memory/HeapMemoryUsage/used" -> "jvm.heap.summary",
      "java.lang:type=Memory/NonHeapMemoryUsage/used" -> "jvm.nonHeap.summary",

      // other stuff
      "java.lang:type=Threading/ThreadCount" -> "jvm.threading.count"
    )
  }

}