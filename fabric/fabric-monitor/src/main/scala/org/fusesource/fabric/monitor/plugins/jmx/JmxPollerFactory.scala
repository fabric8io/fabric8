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
package jmx

import org.fusesource.fabric.api.monitor.{Poller, DataSourceDTO, PollerFactory}
import org.fusesource.fabric.internal.Numbers._
import javax.management.ObjectName
import javax.management.openmbean.CompositeData
import org.fusesource.fabric.api.monitor.DataSourceDTO

/**
 * A PollerFactory for dealing with JMX Attribute polls
 */
class JmxPollerFactory extends PollerFactory with JmxMixin {
  def jaxb_package = getClass.getName.replaceAll("""\.[^\.]*$""", "")

  def accepts(source: DataSourceDTO) = source.poll match {
    case x: MBeanAttributePollDTO => true
    case _ => false
  }

  def create(s: DataSourceDTO) = {
    s.poll match {
      case mbeanPoll: MBeanAttributePollDTO =>
        import mbeanPoll._
        val objectName = new ObjectName(mbean)
        new Poller {
          val source = s

          def close = {
          }

          def poll = {
            val value = mbeanServer.getAttribute(objectName, attribute)
            if (key==null) {
              toNumber(value, "MBean " + mbean + " attribute " + attribute)
            } else {
              def message = "MBean " + mbean + " attribute " + attribute + " key " + key
              value match {
                case cd: CompositeData => toNumber(cd.get(key), message)
                case _ => throw new IllegalArgumentException(message + " is not a CompositeData value")
              }
            }
          }
        }
      case p => throw new IllegalArgumentException("Cannot create a Poller for " + p)
    }
  }
}
