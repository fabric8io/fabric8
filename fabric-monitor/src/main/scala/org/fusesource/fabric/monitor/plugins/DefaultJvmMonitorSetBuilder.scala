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
    jmxDataSource("java.lang:name=CMS Old Gen,type=MemoryPool", "Usage", "used").foreach { source=>
      source.id = "jvm.heap.cms_old_gen"
      source.name = "Used Old Gen Space"
      source.description = "This space holds tenured objects which are not expected to be garbage collected often."
    }
    jmxDataSource("java.lang:name=Par Survivor Space,type=MemoryPool", "Usage", "used").foreach { source=>
      source.id = "jvm.heap.par_survivor"
      source.name = "Space Used Par Survivor"
      source.description = "This space holds object which survive past the eden pool"
    }
    jmxDataSource("java.lang:name=Par Eden Space,type=MemoryPool", "Usage", "used").foreach { source=>
      source.id = "jvm.heap.par_eden"
      source.name = "Used Par Eden Space"
      source.description = "This space is where objects are first allocated."
    }
    jmxDataSource("java.lang:type=Memory", "HeapMemoryUsage", "used").foreach { source=>
      source.id = "jvm.heap.summary"
      source.name = "Used in All Heap Pools"
      source.description = "The the amount of memory being used in all the heap pools"
    }
    jmxDataSource("java.lang:type=Memory", "HeapMemoryUsage", "committed").foreach { source=>
      source.id = "jvm.heap.committed"
      source.name = "Allocated in All Heap Pools"
      source.description = "The the amount of memory allocated for all the heap pools."
    }


    //
    // Non heap spaces.
    //
    jmxDataSource("java.lang:name=CMS Perm Gen,type=MemoryPool", "Usage", "used").foreach { source=>
      source.id = "jvm.non_heap.cms_perm"
      source.name = "Used CMS Perm Gen Space"
      source.description = "The space containing all the reflective data of the virtual machine itself, such as class and method objects."
    }
    jmxDataSource("java.lang:name=Code Cache,type=MemoryPool", "Usage", "used").foreach { source=>
      source.id = "jvm.non_heap.code"
      source.name = "Used Code Cache Space"
      source.description = "The space containing memory that is used for compilation and storage of native code."
    }
    jmxDataSource("java.lang:type=Memory", "NonHeapMemoryUsage", "used").foreach { source=>
      source.id = "jvm.non_heap.summary"
      source.name = "Used in All Non-Heap Pools"
      source.description = "The the amount of memory being used in all the non-heap pools."
    }
    jmxDataSource("java.lang:type=Memory", "NonHeapMemoryUsage", "committed").foreach { source=>
      source.id = "jvm.non_heap.committed"
      source.name = "Allocated in All Non-Heap Pools"
      source.description = "The the amount of memory allocated for all the non-heap pools."
    }

    // other stuff
    jmxDataSource("java.lang:type=Threading", "ThreadCount").foreach { source=>
      source.id = "jvm.threading.count"
      source.name = "Threads Used"
      source.description = "The number of threads being used by the JVM."
    }
    jmxDataSource("java.lang:type=OperatingSystem", "OpenFileDescriptorCount").foreach { source=>
      source.id = "jvm.os.fd"
      source.name = "Open File Descriptors"
      source.description = "The number of open file descriptors."
    }
  }

}