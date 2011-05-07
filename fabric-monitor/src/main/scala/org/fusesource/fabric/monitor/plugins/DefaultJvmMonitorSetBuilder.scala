package org.fusesource.fabric.monitor.plugins

/**
 * Builds the default monitor set for a typical JVM
 */
class DefaultJvmMonitorSetBuilder extends MonitorSetBuilder("jvmDefault") {

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

    //
    // The heap memory pools.
    //
    jmxDataSource("java.lang:name=CMS Old Gen,type=MemoryPool", "Usage", "used").foreach { source=>
      source.id = "jvm.heap.cms_old_gen"
      source.name = "Old Generation Memory Pool Used"
      source.description = "The old generation holds tenured objects which are not expected to be garbage collected often."
    }
    jmxDataSource("java.lang:name=Par Survivor Space,type=MemoryPool", "Usage", "used").foreach { source=>
      source.id = "jvm.heap.par_survivor"
      source.name = "Survivor Memory Pool Used"
      source.description = "The survior memory pool holds object which survive past the eden pool"
    }
    jmxDataSource("java.lang:name=Par Eden Space,type=MemoryPool", "Usage", "used").foreach { source=>
      source.id = "jvm.heap.par_eden"
      source.name = "Eden Memory Pool Used"
      source.description = "The eden memory pool is where objects are first allocated."
    }
    jmxDataSource("java.lang:name=Par Eden Space,type=MemoryPool", "Usage", "used").foreach { source=>
      source.id = "jvm.heap.par_eden"
      source.name = "Eden Memory Pool Used"
      source.description = "The eden memory pool is where objects are first allocated."
    }
    jmxDataSource("java.lang:type=Memory", "HeapMemoryUsage", "used").foreach { source=>
      source.id = "jvm.heap.summary"
      source.name = "Heap Memory Used"
      source.description = "The the amount of heap memory used."
    }


    //
    // Non heap memory pools.
    //
    jmxDataSource("java.lang:name=CMS Perm Gen,type=MemoryPool", "Usage", "used").foreach { source=>
      source.id = "jvm.non_heap.cms_perm"
      source.name = "Permanent Generation Memory Pool Used"
      source.description = "The memory pool containing all the reflective data of the virtual machine itself, such as class and method objects."
    }
    jmxDataSource("java.lang:name=Code Cache,type=MemoryPool", "Usage", "used").foreach { source=>
      source.id = "jvm.non_heap.code"
      source.name = "Code Cache Memory Pool Used"
      source.description = "The memory pool containing memory that is used for compilation and storage of native code."
    }
    jmxDataSource("java.lang:type=Memory", "NonHeapMemoryUsage", "used").foreach { source=>
      source.id = "jvm.non_heap.summary"
      source.name = "Non-Heap Memory Used"
      source.description = "The the amount of non-heap memory used."
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