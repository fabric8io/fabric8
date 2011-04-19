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

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object SystemPollerFactory extends PollerFactory {

  def jaxb_package = getClass.getName.replaceAll("""\.[^\.]*$""", "")

  def accepts(source: DataSourceDTO) = source.poll match {
    case x:SystemPollDTO => x.resource match {
      case "cpu-idle"         => true
      case "cpu-sys"          => true
      case "cpu-user"         => true
      case "cpu-total"        => true
      case "cpu-wait"         => true
      case "cpu-nice"         => true
      case "cpu-irq"          => true
      case "mem-free"         => true
      case "mem-ram"          => true
      case "mem-total"        => true
      case "mem-used"         => true
      case "mem-actual-free"  => true
      case "mem-actual-used"  => true
      case "mem-free-percent" => true
      case "mem-used-percent" => true
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

      lazy val cpu = sigar.getCpu
      lazy val mem = sigar.getMem
      sources.map { source =>
        source.poll.asInstanceOf[SystemPollDTO].resource match {
          case "cpu-idle"         => cpu.getIdle.toDouble
          case "cpu-sys"          => cpu.getSys.toDouble
          case "cpu-user"         => cpu.getUser.toDouble
          case "cpu-total"        => cpu.getTotal.toDouble
          case "cpu-wait"         => cpu.getWait.toDouble
          case "cpu-nice"         => cpu.getNice.toDouble
          case "cpu-irq"          => cpu.getIrq.toDouble
          case "mem-free"         => mem.getFree.toDouble
          case "mem-ram"          => mem.getRam.toDouble
          case "mem-total"        => mem.getTotal.toDouble
          case "mem-used"         => mem.getUsed.toDouble
          case "mem-actual-free"  => mem.getActualFree.toDouble
          case "mem-actual-used"  => mem.getActualUsed.toDouble
          case "mem-free-percent" => mem.getFreePercent
          case "mem-used-percent" => mem.getUsedPercent
        }
      }
    }

  }
}