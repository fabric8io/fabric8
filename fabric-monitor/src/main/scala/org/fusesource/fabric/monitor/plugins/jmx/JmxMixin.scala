package org.fusesource.fabric.monitor.plugins.jmx

import management.ManagementFactory
import javax.management.MBeanServer

/**
 * For classes which need to communicate with a JMX MBeanServer
 *
 * Lets us
 */

trait JmxMixin {

  def mbeanServer: MBeanServer = ManagementFactory.getPlatformMBeanServer()

}