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
package jmx

import collection.JavaConversions._
import io.fabric8.monitor.api.DataSourceDTO

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
