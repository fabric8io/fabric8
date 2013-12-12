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

import org.hyperic.sigar.Sigar
import io.fabric8.monitor.api.{Poller, PollerFactory, DataSourceDTO}

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
      case SystemConstants.cpu_idle         => true
      case SystemConstants.cpu_sys          => true
      case SystemConstants.cpu_user         => true
      case SystemConstants.cpu_total        => true
      case SystemConstants.cpu_wait         => true
      case SystemConstants.cpu_nice         => true
      case SystemConstants.cpu_irq          => true
      case SystemConstants.mem_free         => true
      case SystemConstants.mem_ram          => true
      case SystemConstants.mem_total        => true
      case SystemConstants.mem_used         => true
      case SystemConstants.mem_actual_free  => true
      case SystemConstants.mem_actual_used  => true
      case SystemConstants.mem_free_percent => true
      case SystemConstants.mem_used_percent => true
      case _ => false
    }
    case _ => false
  }

  def create(s: DataSourceDTO) = new Poller {
    val source = s
    val dto = source.poll.asInstanceOf[SystemPollDTO]
    val sigar = new Sigar


    def close = {
      sigar.close
    }

    def poll = {
      lazy val cpu = sigar.getCpu
      lazy val mem = sigar.getMem

      dto.resource match {
        case SystemConstants.cpu_idle => cpu.getIdle.toDouble
        case SystemConstants.cpu_sys => cpu.getSys.toDouble
        case SystemConstants.cpu_user => cpu.getUser.toDouble
        case SystemConstants.cpu_total => cpu.getTotal.toDouble
        case SystemConstants.cpu_wait => cpu.getWait.toDouble
        case SystemConstants.cpu_nice => cpu.getNice.toDouble
        case SystemConstants.cpu_irq => cpu.getIrq.toDouble
        case SystemConstants.mem_free => mem.getFree.toDouble
        case SystemConstants.mem_ram => mem.getRam.toDouble
        case SystemConstants.mem_total => mem.getTotal.toDouble
        case SystemConstants.mem_used => mem.getUsed.toDouble
        case SystemConstants.mem_actual_free => mem.getActualFree.toDouble
        case SystemConstants.mem_actual_used => mem.getActualUsed.toDouble
        case SystemConstants.mem_free_percent => mem.getFreePercent
        case SystemConstants.mem_used_percent => mem.getUsedPercent
      }
    }

  }
}
