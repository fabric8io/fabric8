/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.monitor.plugins.jmx

import management.ManagementFactory
import javax.management.MBeanServer

/**
 * For classes which need to communicate with a JMX MBeanServer
 */
trait JmxMixin {

  def mbeanServer: MBeanServer = ManagementFactory.getPlatformMBeanServer()

}

