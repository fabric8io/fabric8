package org.fusesource.fabric.monitor.plugins

import org.fusesource.fabric.monitor.api.{Poller, DataSourceDTO, PollerFactory}

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
      mbeanDTOs.map { dto => pollMbeanAttribute(dto) }
    }

    def pollMbeanAttribute(dto: MBeanAttributePollDTO) = {
      Double.NaN
    }
  }
}