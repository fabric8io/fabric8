package org.fusesource.fabric.monitor.plugins

import org.fusesource.fabric.monitor.api.{Poller, DataSourceDTO, PollerFactory}
import javax.management.remote.JMXConnectorFactory
import javax.management.{ObjectName, MBeanServer, MBeanServerFactory}

/**
 * A PollerFactory for dealing with JMX Attribute polls
 */
class JmxPollerFactory extends PollerFactory {

  def jaxb_package = getClass.getName.replaceAll("""\.[^\.]*$""", "")

  def accepts(source: DataSourceDTO) = source.poll match {
    case x: MBeanAttributePollDTO => true
    case _ => false
  }

  def create(s: Array[DataSourceDTO]) = new Poller {

    val mbeanDTOs = s.map {source => source.poll.asInstanceOf[MBeanAttributePollDTO] }

    def close = {
    }

    def sources = s

    def poll = {
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
          case x:Number => x.doubleValue()
          case x:AnyRef => x.toString.toDouble
        }
      } catch {
        case _ => Double.NaN
      }
    }
  }
}