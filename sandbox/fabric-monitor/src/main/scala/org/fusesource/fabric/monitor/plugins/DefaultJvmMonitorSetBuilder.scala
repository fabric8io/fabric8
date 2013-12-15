/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.monitor.plugins

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
    jmxDataSource("java.lang:name=CMS Old Gen,type=MemoryPool", "Usage", "max")
    jmxDataSource("java.lang:name=Par Survivor Space,type=MemoryPool", "Usage", "used")
    jmxDataSource("java.lang:name=Par Survivor Space,type=MemoryPool", "Usage", "max")
    jmxDataSource("java.lang:name=Par Eden Space,type=MemoryPool", "Usage", "used")
    jmxDataSource("java.lang:name=Par Eden Space,type=MemoryPool", "Usage", "max")
    jmxDataSource("java.lang:name=PS Old Gen,type=MemoryPool", "Usage", "used")
    jmxDataSource("java.lang:name=PS Old Gen,type=MemoryPool", "Usage", "max")
    jmxDataSource("java.lang:name=PS Survivor Space,type=MemoryPool", "Usage", "used")
    jmxDataSource("java.lang:name=PS Survivor Space,type=MemoryPool", "Usage", "max")
    jmxDataSource("java.lang:name=PS Eden Space,type=MemoryPool", "Usage", "used")
    jmxDataSource("java.lang:name=PS Eden Space,type=MemoryPool", "Usage", "max")

    jmxDataSource("java.lang:type=Memory", "HeapMemoryUsage", "used")
    jmxDataSource("java.lang:type=Memory", "HeapMemoryUsage", "committed")

    //
    // Non heap spaces.
    //
    jmxDataSource("java.lang:name=CMS Perm Gen,type=MemoryPool", "Usage", "used")
    jmxDataSource("java.lang:name=CMS Perm Gen,type=MemoryPool", "Usage", "max")
    jmxDataSource("java.lang:name=PS Perm Gen,type=MemoryPool", "Usage", "used")
    jmxDataSource("java.lang:name=PS Perm Gen,type=MemoryPool", "Usage", "max")

    jmxDataSource("java.lang:name=Code Cache,type=MemoryPool", "Usage", "used")
    jmxDataSource("java.lang:name=Code Cache,type=MemoryPool", "Usage", "max")
    jmxDataSource("java.lang:type=Memory", "NonHeapMemoryUsage", "used")
    jmxDataSource("java.lang:type=Memory", "NonHeapMemoryUsage", "committed")

    // other stuff
    jmxDataSource("java.lang:type=Threading", "ThreadCount")
    jmxDataSource("java.lang:type=OperatingSystem", "OpenFileDescriptorCount")
  }

}