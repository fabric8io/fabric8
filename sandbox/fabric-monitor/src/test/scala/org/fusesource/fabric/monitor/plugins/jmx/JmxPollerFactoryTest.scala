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

import io.fabric8.monitor.api.DataSourceDTO

class JmxPollerFactoryTest extends FunSuiteSupport {

  test("monitors some JMX stats") {
    val dto = new DataSourceDTO()
    dto.id = "jmx.threadCount"
    dto.name = "JVM Thread Count"
    dto.description = "JVM Thread Count"
    dto.kind = "gauge"
    dto.poll = new MBeanAttributePollDTO("java.lang:type=Threading", "ThreadCount")

    val pollerFactory = new JmxPollerFactory()

    assert(pollerFactory.accepts(dto), "factory: " + pollerFactory + "should accept: " + dto)

    val poller = pollerFactory.create(dto)
    val value = poller.poll

    poller.close
    // TODO have a close on the PollerFactory?

    println("value: " + value)
    assert(value != Double.NaN && value > 0, "Invalid thread count: " + value)
  }
}
