package org.fusesource.fabric.monitor.plugins
package jmx

import org.fusesource.fabric.monitor.api.{Poller, DataSourceDTO, PollerFactory}
import management.ManagementFactory
import javax.management.{ObjectName, MBeanServer}

/**
 * A PollerFactory for dealing with JMX Attribute polls
 */
class JmxPollerFactory extends PollerFactory with JmxMixin {
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
      /*
      val servers = MBeanServerFactory.findMBeanServer(null)
      if( servers.isEmpty ) {
        mbeanDTOs.map (dto => Double.NaN)
      } else {
        val server = servers.get(0)
        mbeanDTOs.map { dto => pollMbeanAttribute(server, dto) }
      }
    }

    def pollMbeanAttribute(server:MBeanServer, dto: MBeanAttributePollDTO) = {
      val mbeanName = new ObjectName(dto.mbean);
      try {
        server.getAttribute(mbeanName, dto.attribute) match {
          case x:Number =>
            x.doubleValue()
          case x:AnyRef =>
            x.toString.toDouble
        }
      } catch {
        case e:Throwable =>
          Double.NaN
     */
      }
    }
  }
}

class IllegalMBeanAttributeType(val mbeanPoll: MBeanAttributePollDTO, val value: AnyRef)
        extends IllegalArgumentException("Invalid value " + value + " for " + mbeanPoll) {

}