package org.fusesource.fabric.monitor.plugins

import jmx.JmxConstants
import org.fusesource.fabric.monitor.api.DataSourceDTO

object DataSourceEnricher {
  var enrichers: List[DataSourceEnricher] = List(new DefaultDataSourceEnricher)

  def apply(dto: DataSourceDTO): DataSourceDTO = {
    var d = dto
    for (e <- enrichers) {
      d = e(d)
    }
    d
  }
}

/**
 * Enriches a given data source
 */
abstract class DataSourceEnricher extends Function1[DataSourceDTO, DataSourceDTO] {
}

class DefaultDataSourceEnricher extends DataSourceEnricher {

  def apply(source: DataSourceDTO): DataSourceDTO = {
    val split = source.id.split(JmxConstants.SEPARATOR)
    split match {

      // heap spaces
      case Array("java.lang:name=CMS Old Gen,type=MemoryPool", "Usage") =>
        // case "jvm.heap.cms_old_gen" =>
        source.name = "Used Old Gen Space"
        source.description = "This space holds tenured objects which are not expected to be garbage collected often."
      case Array("java.lang:name=Par Survivor Space,type=MemoryPool", "Usage") =>
        // case "jvm.heap.par_survivor" =>
        source.name = "Used Par Survivor Space"
        source.description = "This space holds object which survive past the eden pool"
      case Array("java.lang:name=Par Eden Space,type=MemoryPool", "Usage") =>
        // case "jvm.heap.par_eden" =>
        source.name = "Used Par Eden Space"
        source.description = "This space is where objects are first allocated."
      case Array("java.lang:type=Memory", "HeapMemoryUsage", "committed") =>
        // case "jvm.heap.committed" =>
        source.name = "Allocated in All Heap Pools"
        source.description = "The the amount of memory allocated for all the heap pools."
      case Array("java.lang:type=Memory", "HeapMemoryUsage") =>
        // case "jvm.heap.summary" =>
        source.name = "Used in All Heap Pools"
        source.description = "The the amount of memory being used in all the heap pools"


      //
      // Non heap spaces.
      //
      case Array("java.lang:name=CMS Perm Gen,type=MemoryPool", "Usage") =>
        // case "jvm.non_heap.cms_perm" =>
        source.name = "Used CMS Perm Gen Space"
        source.description = "The space containing all the reflective data of the virtual machine itself, such as class and method objects."
      case Array("java.lang:name=Code Cache,type=MemoryPool", "Usage") =>
        // case "jvm.non_heap.code" =>
        source.name = "Used Code Cache Space"
        source.description = "The space containing memory that is used for compilation and storage of native code."
      case Array("java.lang:type=Memory", "NonHeapMemoryUsage") =>
        // case "jvm.non_heap.summary" =>
        source.name = "Used in All Non-Heap Pools"
        source.description = "The the amount of memory being used in all the non-heap pools."
      case Array("java.lang:type=Memory", "NonHeapMemoryUsage", "committed") =>
        // case "jvm.non_heap.committed" =>
        source.name = "Allocated in All Non-Heap Pools"
        source.description = "The the amount of memory allocated for all the non-heap pools."

      // other stuff
      case Array("java.lang:type=Threading", "ThreadCount") =>
        // case "jvm.threading.count" =>
        source.name = "Threads Used"
        source.description = "The number of threads being used by the JVM."
      case Array("java.lang:type=OperatingSystem", "OpenFileDescriptorCount") =>
        // case "jvm.os.fd" =>
        source.name = "Open File Descriptors"
        source.description = "The number of open file descriptors."

      case _ =>
    }
    source
  }
}