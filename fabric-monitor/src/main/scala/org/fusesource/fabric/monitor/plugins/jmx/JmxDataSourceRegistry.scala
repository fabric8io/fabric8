package org.fusesource.fabric.monitor.plugins
package jmx

import collection.JavaConversions._
import collection.mutable.{ListBuffer, HashMap}
import org.fusesource.fabric.monitor.api.{DataSourceGroupDTO, DataSourceDTO}
import javax.management.{MBeanAttributeInfo, QueryExp, ObjectName}
import javax.management.openmbean.CompositeData
import JmxConstants._

/**
 * Discovers the available values in JMX
 */
class JmxDataSourceRegistry extends JmxMixin {

  def createDataSource(objectName: String, attributeName: String): Option[DataSourceDTO] = {
    try {
      val o = new ObjectName(objectName)
      val info = mbeanServer.getMBeanInfo(o)
      info.getAttributes.find(_.getName == attributeName) match {
        case Some(attrInfo) => Some(createDataSource(o, attrInfo))
        case _ => None
      }
    }
    catch {
      case e => println("Caught: " + e)
      None
    }
  }

  def createDataSource(objectName: ObjectName, attributeInfo: MBeanAttributeInfo): DataSourceDTO = {
    val name = objectName.getCanonicalName
    val dto = new DataSourceDTO
    val attributeName = attributeInfo.getName
    dto.id = name + SEPARATOR + attributeName
    dto.name = attributeName
    dto.description = attributeInfo.getDescription

    // TODO use special repo somewhere to figure out the accurage kinds???
    dto.kind = "gauge"
    dto.heartbeat = "1s"

    val dtoPoll = new MBeanAttributePollDTO(name, attributeName)
    dto.poll = dtoPoll

    val typeName = attributeInfo.getType
    if (typeName == classOf[CompositeData].getName) {
      mbeanServer.getAttribute(objectName, attributeInfo.getName) match {
        case c: CompositeData =>
          val t = c.getCompositeType()
          for (k <- t.keySet) {
            val kdto = new DataSourceDTO()
            kdto.id =  dto.id + SEPARATOR + k
            kdto.name = k
            kdto.description = k

            // TODO use special repo somewhere to figure out the accurage kinds???
            kdto.kind = "gauge"
            kdto.heartbeat = "1s"

            kdto.poll = new MBeanAttributeKeyPollDTO(name, attributeName, k)
            dto.children.add(kdto)
          }
        case a => println("MBean " + objectName + " attribute " + attributeName + " is not a CompositeData value: " + a)
      }
    }
    dto
  }

  def findSources(name: String = null, query: QueryExp = null) = {
    val objectName = if (name != null) new ObjectName(name) else null

    val map = HashMap[String,DataSourceGroupDTO]()

    val names = mbeanServer.queryNames(objectName, query)
    for (o <- names) {
      val d = o.getDomain

      val domainGroup = map.getOrElseUpdate(d, new DataSourceGroupDTO(d))
      val info = mbeanServer.getMBeanInfo(o)

      val objectGroup = new DataSourceGroupDTO(o.getCanonicalName)
      objectGroup.description = info.getDescription
      domainGroup.children.add(objectGroup)

      for (attr <- info.getAttributes) {
        val dto: DataSourceDTO = createDataSource(o, attr)
        objectGroup.data_sources.add(dto)
      }
    }
    map
  }
}