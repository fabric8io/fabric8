package org.fusesource.fabric.monitor.plugins

import org.fusesource.fabric.monitor.api.{Poller, DataSourceDTO, PollerFactory}
import management.ManagementFactory
import javax.management.{ObjectName, MBeanServer}

/**
 * A PollerFactory for dealing with JMX Attribute polls
 */
class JmxPollerFactory(val mbeanServer: MBeanServer = ManagementFactory.getPlatformMBeanServer) extends PollerFactory {
  def jaxb_package = getClass.getName.replaceAll("""\.[^\.]*$""", "")

  def accepts(source: DataSourceDTO) = source.poll match {
    case x: MBeanAttributePollDTO => true
    case _ => false
  }

  def create(s: DataSourceDTO) = new Poller {
    val source = s
    val mbeanPoll = source.poll.asInstanceOf[MBeanAttributePollDTO]
    val objectName = new ObjectName(mbeanPoll.mbean)
    val attribute = mbeanPoll.attribute

    def close = {
    }

    def poll = {
      mbeanServer.getAttribute(objectName, attribute) match {
        case n: Number => n.doubleValue
        case v => throw new IllegalMBeanAttributeType(mbeanPoll, v)
      }
    }
  }
}

class IllegalMBeanAttributeType(val mbeanPoll: MBeanAttributePollDTO, val value: AnyRef)
        extends IllegalArgumentException("Invalid value " + value + " for " + mbeanPoll) {

}