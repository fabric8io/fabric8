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
import collection.mutable.ListBuffer
import org.hyperic.sigar.{SigarNotImplementedException, Sigar}

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
      list ::= "threads"
    } catch { case x:SigarNotImplementedException => }

    try {
      sigar.getProcCpu(pid)
      list ::= "cpu-percent"
      list ::= "cpu-sys"
      list ::= "cpu-total"
      list ::= "cpu-last"
      list ::= "cpu-start"
    } catch { case x:SigarNotImplementedException => }

    try {
      sigar.getProcFd(pid)
      list ::= "fd-total"
    } catch { case x:SigarNotImplementedException => }

    try {
      sigar.getProcMem(pid)
      list ::= "mem-resident"
      list ::= "mem-share"
      list ::= "mem-size"
      list ::= "mem-major-faults"
      list ::= "mem-minor-faults"
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
      rc
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
              case "threads" => state.getThreads.toDouble
              case "cpu-percent" => cpu.getPercent
              case "cpu-sys" => cpu.getSys.toDouble
              case "cpu-total" => cpu.getTotal.toDouble
              case "cpu-last" => cpu.getLastTime.toDouble
              case "cpu-start" => cpu.getStartTime.toDouble
              case "fd-total" => fd.getTotal.toDouble
              case "mem-resident" => mem.getResident.toDouble
              case "mem-share" => mem.getShare.toDouble
              case "mem-size" => mem.getSize.toDouble
              case "mem-major-faults" => mem.getMajorFaults.toDouble
              case "mem-minor-faults" => mem.getMinorFaults.toDouble
            }
          } catch {
            case _ => Double.NaN
          }
      }.getOrElse(Double.NaN)
    }
  }
}
