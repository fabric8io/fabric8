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

import collection.mutable.ListBuffer
import org.hyperic.sigar.{SigarNotImplementedException, Sigar}

import ProcessConstants._
import io.fabric8.monitor.api.{Poller, PollerFactory, DataSourceDTO}

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object ProcessPollerFactory extends PollerFactory {

  def jaxb_package = getClass.getName.replaceAll("""\.[^\.]*$""", "")

  val supported_stats = {
    var list = List[String]()

    val sigar = new Sigar
    val pid = sigar.getPid

    try {
      sigar.getProcState(pid)
      list ::= threads
    } catch { case x:SigarNotImplementedException => }

    try {
      sigar.getProcCpu(pid)
      list ::= cpu_percent
      list ::= cpu_sys
      list ::= cpu_total
      list ::= cpu_last
      list ::= cpu_start
    } catch { case x:SigarNotImplementedException => }

    try {
      sigar.getProcFd(pid)
      list ::= fd_total
    } catch { case x:SigarNotImplementedException => }

    try {
      sigar.getProcMem(pid)
      list ::= mem_resident
      list ::= mem_share
      list ::= mem_size
      list ::= mem_major_faults
      list ::= mem_minor_faults
    } catch { case x:SigarNotImplementedException => }

    sigar.close
    list
  }


  def discover(pid:java.lang.Long):Array[DataSourceDTO] = {
    val rc = ListBuffer[DataSourceDTO]()

    def source(resource:String, name:String="", description:String="", kind:String="gauge") = {
      val rc = new DataSourceDTO
      rc.id = resource
      rc.name = name
      rc.description = description
      rc.kind = kind
      val pp = new ProcessPollDTO
      pp.pid = pid
      pp.resource = resource
      rc.poll = pp
      DataSourceEnricher(rc)
    }

    supported_stats.map(source(_)).toArray
  }

  def accepts(source: DataSourceDTO) = source.poll match {
    case x:ProcessPollDTO => supported_stats.contains(x.resource)
    case _ => false
  }

  def create(s: DataSourceDTO) = new Poller {
    val source = s
    val dto = source.poll.asInstanceOf[ProcessPollDTO]
    val sigar = new Sigar

    def close = {
      sigar.close
    }

    def poll = {
      Option(dto.pid).map{
          pid =>
          try {
            def state = sigar.getProcState(pid.longValue)
            def cpu = sigar.getProcCpu(pid.longValue)
            def fd = sigar.getProcFd(pid.longValue)
            def mem = sigar.getProcMem(pid.longValue)

            dto.resource match {
              case ProcessConstants.threads => state.getThreads.toDouble
              case ProcessConstants.cpu_percent => cpu.getPercent
              case ProcessConstants.cpu_sys => cpu.getSys.toDouble
              case ProcessConstants.cpu_total => cpu.getTotal.toDouble
              case ProcessConstants.cpu_last => cpu.getLastTime.toDouble
              case ProcessConstants.cpu_start => cpu.getStartTime.toDouble
              case ProcessConstants.fd_total => fd.getTotal.toDouble
              case ProcessConstants.mem_resident => mem.getResident.toDouble
              case ProcessConstants.mem_share => mem.getShare.toDouble
              case ProcessConstants.mem_size => mem.getSize.toDouble
              case ProcessConstants.mem_major_faults => mem.getMajorFaults.toDouble
              case ProcessConstants.mem_minor_faults => mem.getMinorFaults.toDouble
            }
          } catch {
            case _ => Double.NaN
          }
      }.getOrElse(Double.NaN)
    }
  }
}
