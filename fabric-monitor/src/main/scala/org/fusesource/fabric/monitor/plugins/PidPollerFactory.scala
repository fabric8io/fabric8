/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.monitor.plugins

import org.fusesource.fabric.monitor.api.{Poller, DataSourceDTO, PollerFactory}
import org.hyperic.sigar.Sigar
import collection.mutable.ListBuffer

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object ProcessPollerFactory extends PollerFactory {

  def jaxb_package = getClass.getName.replaceAll("""\.[^\.]*$""", "")


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
      rc
    }

    rc += source("threads", "Thread Count", "The number of threads running in the process")
    rc += source("cpu-percent")
    rc += source("cpu-sys")
    rc += source("cpu-total")
    rc += source("cpu-sys")
    rc += source("cpu-last")
    rc += source("cpu-start")
    rc += source("fd-total")
    rc += source("mem-resident")
    rc += source("mem-share")
    rc += source("mem-size")
    rc += source("mem-major-faults")
    rc += source("mem-minor-faults")
    rc.toArray

  }

  def accepts(source: DataSourceDTO) = source.poll match {
    case x:ProcessPollDTO => x.resource match {
      case "threads"          => true
      case "cpu-percent"      => true
      case "cpu-sys"          => true
      case "cpu-total"        => true
      case "cpu-last"         => true
      case "cpu-start"        => true
      case "fd-total"         => true
      case "mem-resident"     => true
      case "mem-share"        => true
      case "mem-size"         => true
      case "mem-major-faults" => true
      case "mem-minor-faults" => true
      case _ => false
    }
    case _ => false
  }

  def create(s: Array[DataSourceDTO]) = new Poller {

    val sigar = new Sigar

    def close = {
      sigar.close
    }

    def sources = s

    def poll = {
      sources.map { source =>
        val dto = source.poll.asInstanceOf[ProcessPollDTO]
        Option(dto.pid).map{ pid=>
          def state = sigar.getProcState(pid.longValue)
          def cpu = sigar.getProcCpu(pid.longValue)
          def fd = sigar.getProcFd(pid.longValue)
          def mem = sigar.getProcMem(pid.longValue)
          dto.resource match {
            case "threads"        => state.getThreads.toDouble
            case "cpu-percent"    => cpu.getPercent
            case "cpu-sys"        => cpu.getSys.toDouble
            case "cpu-total"      => cpu.getTotal.toDouble
            case "cpu-last"       => cpu.getLastTime.toDouble
            case "cpu-start"      => cpu.getStartTime.toDouble
            case "fd-total"       => fd.getTotal.toDouble
            case "mem-resident"   => mem.getResident.toDouble
            case "mem-share"      => mem.getShare.toDouble
            case "mem-size"       => mem.getSize.toDouble
            case "mem-major-faults"=> mem.getMajorFaults.toDouble
            case "mem-minor-faults"=> mem.getMinorFaults.toDouble
          }
        }.getOrElse(Double.NaN)
      }
    }

  }
}
