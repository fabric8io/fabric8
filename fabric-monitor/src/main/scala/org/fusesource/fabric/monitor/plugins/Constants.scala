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

trait CommonConstants {
  val cpu_idle = "cpu-idle"
  val cpu_sys = "cpu-sys"
  val cpu_user = "cpu-user"
  val cpu_total = "cpu-total"
  val cpu_wait = "cpu-wait"
  val cpu_nice = "cpu-nice"
  val cpu_irq = "cpu-irq"
  val mem_free = "mem-free"
  val mem_ram = "mem-ram"
  val mem_total = "mem-total"
  val mem_used = "mem-used"
  val mem_actual_free = "mem-actual-free"
  val mem_actual_used = "mem-actual-used"
  val mem_free_percent = "mem-free-percent"
  val mem_used_percent = "mem-used-percent"
}

object SystemConstants extends CommonConstants {
}

object ProcessConstants extends CommonConstants {
  val threads = "threads"
  val fd_total = "fd-total"

  val cpu_percent = "cpu-percent"
  val cpu_last = "cpu-last"
  val cpu_start = "cpu-start"
  val mem_resident = "mem-resident"
  val mem_share = "mem-share"
  val mem_size = "mem-size"
  val mem_major_faults = "mem-major-faults"
  val mem_minor_faults = "mem-minor-faults"
}

