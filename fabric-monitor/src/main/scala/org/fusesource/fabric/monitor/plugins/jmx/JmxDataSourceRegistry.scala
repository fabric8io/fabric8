package org.fusesource.fabric.monitor.plugins
package jmx

import javax.management.{QueryExp, ObjectName}
import collection.JavaConversions._
import collection.mutable.{ListBuffer, HashMap}
import org.fusesource.fabric.monitor.api.{DataSourceGroupDTO, DataSourceDTO}

/**
 * Discovers the available values in JMX
 */
class JmxDataSourceRegistry(val name: String = null, val query: QueryExp = null) extends JmxMixin {
  val objectName = if (name != null) new ObjectName(name) else null

  val SEPARATOR = "/"

  def findSources = {
    val map = HashMap[String,DataSourceGroupDTO]()

    val answer = mbeanServer.queryNames(objectName, query)
    for (o <- answer) {
      val d = o.getDomain

      val domainGroup = map.getOrElseUpdate(d, new DataSourceGroupDTO(d))
      val info = mbeanServer.getMBeanInfo(o)

      val objectGroup = new DataSourceGroupDTO(o.getCanonicalName)
      objectGroup.description = info.getDescription

      for (attr <- info.getAttributes) {
        val dto = new DataSourceDTO
        val name = o.getCanonicalName
        val attributeName = attr.getName
        dto.id = name + SEPARATOR + attributeName
        dto.name = attributeName
        dto.description = attr.getDescription
        dto.poll = new MBeanAttributePollDTO(name,  attributeName)
        objectGroup.data_sources.add(dto)
      }
    }
    map
  }
}