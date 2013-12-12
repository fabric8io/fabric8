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
package io.fabric8.monitor
package plugins
package jmx

import java.io.File
import io.fabric8.service.LocalJmxTemplate

import collection.JavaConversions._
import management.ManagementFactory
import javax.management.{MBeanServer, MBeanServerFactory, MBeanServerConnection, ObjectName}
import io.fabric8.monitor.api.{MonitorFacade, FetchMonitoredViewDTO}

/**
 * Test the default JVM RRD file generation
 */
class JmxRrdTest extends FunSuiteSupport {
  val debugging = false

  def findMBeanServer: MBeanServerConnection = {
    if (false) {
      val list = MBeanServerFactory.findMBeanServer(null)
      for (m <- list) {
        println("Found: " + m)
      }
      if (list.size > 0) {
        list.get(0).asInstanceOf[MBeanServerConnection]
      } else {
        ManagementFactory.getPlatformMBeanServer
      }
    } else {
      ManagementFactory.getPlatformMBeanServer
    }
  }

  test("Generate standard JVM RRD tool") {
    val stats_directory = new File("target")
    stats_directory.mkdirs()

    val beanServer = findMBeanServer
    val statName = new ObjectName("java.lang:type=Memory")
    println("object " + statName + " is registered " + beanServer.isRegistered(statName))

    // lets registry the mbean
    val name = new ObjectName("io.fabric8:type=Monitor")

    val mbean = new MonitorService()
    mbean.datadir = stats_directory
    //mbean.monitor = monitor
    mbean.start()

    beanServer match {
      case mb: MBeanServer =>
        mb.registerMBean(mbean, name)
      case _ =>
    }
    println("Registererd mbean " + mbean + " at " + name)

    println("Waiting for some values to be written")

    for (i <- 0 to 5) {
      Thread.sleep(1000L)

      val view = new FetchMonitoredViewDTO();

      view.data_sources.addAll(List(
        "java.lang:name=CMS Old Gen,type=MemoryPool@Usage@used",
        "java.lang:name=Par Survivor Space,type=MemoryPool@Usage@used",
        "java.lang:name=Par Eden Space,type=MemoryPool@Usage@used",
        "java.lang:type=Memory@HeapMemoryUsage@committed"

        /*
        "java.lang:name=CMS Old Gen,type=MemoryPool@Usage@used",
        "java.lang:name=CMS Old Gen,type=MemoryPool@Usage@max",
        "java.lang:name=Par Survivor Space,type=MemoryPool@Usage@used",
        "java.lang:name=Par Survivor Space,type=MemoryPool@Usage@max",
        "java.lang:name=Par Eden Space,type=MemoryPool@Usage@used",
        "java.lang:name=Par Eden Space,type=MemoryPool@Usage@max"
        */
      ))
      view.monitored_set = "jvm-default"

      val jmxTemplate = new LocalJmxTemplate()
      val answer = MonitorFacade.fetch(jmxTemplate, view)

      println("Got answer: " + answer)
    }

    if (debugging) {
      Thread.sleep(5000000L)
    }

    println("Done")
  }
}
