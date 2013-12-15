/*
 * Copyright 2010 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.webui.agents.jvm

import io.fabric8.api.Container
import io.fabric8.service.ContainerTemplate
import io.fabric8.webui.agents.{ManagementExtension, ManagementExtensionFactory}
import io.fabric8.webui.agents.{ManagementExtension, ManagementExtensionFactory}
import javax.ws.rs.{GET, Path}
import javax.management.ObjectName
import javax.management.openmbean.CompositeData
import io.fabric8.service.JmxTemplateSupport.JmxConnectorCallback
import javax.management.remote.JMXConnector
import io.fabric8.webui.BaseResource

object JVMAgentResource extends ManagementExtensionFactory {
  def create(a: Container, jmx_username: String, jmx_password: String) = {
    if (a.getJmxDomains.contains("java.lang")) {
      Some(new JVMAgentResource(a, jmx_username, jmx_password))
    } else {
      None
    }
  }
}


class JVMAgentResource(val agent: Container, jmx_username: String, jmx_password: String) extends BaseResource with ManagementExtension {

  private val template = agent_template(agent, jmx_username, jmx_password)

  def id = "jvm"

  @GET
  override
  def get = Array("metrics")

  @GET
  @Path("metrics")
  def metrics = {
    template.execute(new JmxConnectorCallback[JvmMetricsDTO] {
      def doWithJmxConnector(connector: JMXConnector): JvmMetricsDTO = {
        val mbean_server = connector.getMBeanServerConnection;

        val rc = new JvmMetricsDTO
        attempt(
          rc.os_name =
            mbean_server.getAttribute("java.lang:type=OperatingSystem", "Name").toString +
              " " +
              mbean_server.getAttribute("java.lang:type=OperatingSystem", "Version")
        )
        attempt(
          rc.jvm_name =
            mbean_server.getAttribute("java.lang:type=Runtime", "VmName").toString +
              " (" +
              mbean_server.getAttribute("java.lang:type=Runtime", "VmVendor") + ")"
        )

        implicit def to_object_name(value: String): ObjectName = new ObjectName(value)
        implicit def to_long(value: AnyRef): Long = value.asInstanceOf[java.lang.Long].longValue()
        implicit def to_int(value: AnyRef): Int = value.asInstanceOf[java.lang.Integer].intValue()
        implicit def to_double(value: AnyRef): Double = value.asInstanceOf[java.lang.Double].doubleValue()

        def attempt(func: => Unit) = {
          try {
            func
          } catch {
            case _ => // ignore
          }
        }

        attempt(rc.uptime = mbean_server.getAttribute("java.lang:type=Runtime", "Uptime"))
        attempt(rc.start_time = mbean_server.getAttribute("java.lang:type=Runtime", "StartTime"))
        attempt(rc.runtime_name = mbean_server.getAttribute("java.lang:type=Runtime", "Name").toString)

        attempt(rc.spec_name = mbean_server.getAttribute("java.lang:type=Runtime", "SpecName").toString)
        attempt(rc.spec_vendor = mbean_server.getAttribute("java.lang:type=Runtime", "SpecVendor").toString)
        attempt(rc.spec_version = mbean_server.getAttribute("java.lang:type=Runtime", "SpecVersion").toString)

        attempt(rc.vm_name = mbean_server.getAttribute("java.lang:type=Runtime", "VmName").toString)
        attempt(rc.vm_vendor = mbean_server.getAttribute("java.lang:type=Runtime", "VmVendor").toString)
        attempt(rc.vm_version = mbean_server.getAttribute("java.lang:type=Runtime", "VmVersion").toString)

        attempt(rc.os_arch = mbean_server.getAttribute("java.lang:type=OperatingSystem", "Arch").toString)

        attempt(rc.os_fd_open = mbean_server.getAttribute("java.lang:type=OperatingSystem", "OpenFileDescriptorCount"))
        attempt(rc.os_fd_max = mbean_server.getAttribute("java.lang:type=OperatingSystem", "MaxFileDescriptorCount"))

        attempt(rc.os_memory_total = mbean_server.getAttribute("java.lang:type=OperatingSystem", "TotalPhysicalMemorySize"))
        attempt(rc.os_memory_free = mbean_server.getAttribute("java.lang:type=OperatingSystem", "FreePhysicalMemorySize"))

        attempt(rc.os_swap_free = mbean_server.getAttribute("java.lang:type=OperatingSystem", "FreeSwapSpaceSize"))
        attempt(rc.os_swap_free = mbean_server.getAttribute("java.lang:type=OperatingSystem", "TotalSwapSpaceSize"))

        attempt(rc.os_load_average = mbean_server.getAttribute("java.lang:type=OperatingSystem", "SystemLoadAverage"))
        attempt(rc.os_cpu_time = mbean_server.getAttribute("java.lang:type=OperatingSystem", "ProcessCpuTime"))
        attempt(rc.os_processors = mbean_server.getAttribute("java.lang:type=OperatingSystem", "AvailableProcessors"))

        attempt(rc.classes_loaded = mbean_server.getAttribute("java.lang:type=ClassLoading", "LoadedClassCount"))
        attempt(rc.classes_unloaded = mbean_server.getAttribute("java.lang:type=ClassLoading", "UnloadedClassCount"))

        attempt(rc.threads_peak = mbean_server.getAttribute("java.lang:type=Threading", "PeakThreadCount"))
        attempt(rc.threads_current = mbean_server.getAttribute("java.lang:type=Threading", "ThreadCount"))

        def memory_metrics(data: CompositeData) = {
          val rc = new MemoryMetricsDTO
          rc.alloc = data.get("committed").asInstanceOf[java.lang.Long].longValue()
          rc.used = data.get("used").asInstanceOf[java.lang.Long].longValue()
          rc.max = data.get("max").asInstanceOf[java.lang.Long].longValue()
          rc
        }

        attempt(rc.heap_memory = memory_metrics(mbean_server.getAttribute("java.lang:type=Memory", "HeapMemoryUsage").asInstanceOf[CompositeData]))
        attempt(rc.non_heap_memory = memory_metrics(mbean_server.getAttribute("java.lang:type=Memory", "NonHeapMemoryUsage").asInstanceOf[CompositeData]))

        rc
      }
    });
  }

}
