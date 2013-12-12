/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.monitor.plugins
package jmx

import collection.JavaConversions._
import collection.mutable.HashMap
import javax.management.{MBeanAttributeInfo, QueryExp, ObjectName}
import javax.management.openmbean.CompositeData
import JmxConstants._
import org.fusesource.scalate.util.Log
import io.fabric8.monitor.api.DataSourceDTO


object JmxDataSourceRegistry {
  val log = Log(classOf[JmxDataSourceRegistry])
}
import JmxDataSourceRegistry._

/**
 * Discovers the available values in JMX
 */
class JmxDataSourceRegistry extends JmxMixin {

  def createDataSource(objectName: String, attributeName: String, key:String=null): Option[DataSourceDTO] = {
    try {

      var dto = new DataSourceDTO
      dto.id = objectName + SEPARATOR + attributeName
      if ( key !=null ) {
        dto.id += SEPARATOR + key
      }

      val o = new ObjectName(objectName)
      val info = mbeanServer.getMBeanInfo(o)
      info.getAttributes.find(_.getName == attributeName) match {
        case Some(attr) =>
          dto.name = attr.getName
          dto.description = attr.getDescription
          dto.kind = "gauge"
          dto.heartbeat = "1s"
          dto.poll = new MBeanAttributePollDTO(o.getCanonicalName, attr.getName, key)
          DataSourceEnricher(dto)
          Some(dto)
        case _ => None
      }

    } catch {
      case e => log.warn(e, "Caught: " + e)
      None
    }
  }


  def findSources(name: String = null, query: QueryExp = null) = {
    val root = if (name != null) new ObjectName(name) else null
    val map = HashMap[String,DataSourceGroup]()
    val names = mbeanServer.queryNames(root, query)
    for (objectName <- names) {

      val domain = objectName.getDomain
      val domainGroup = map.getOrElseUpdate(domain, new DataSourceGroup(domain))
      val info = mbeanServer.getMBeanInfo(objectName)

      val name = objectName.getCanonicalName
      val objectGroup = new DataSourceGroup(name)
      objectGroup.description = info.getDescription
      domainGroup.children.add(objectGroup)

      for (attr <- info.getAttributes) {
        attr.getType match {

          case "javax.management.openmbean.CompositeData" =>

            val value = mbeanServer.getAttribute(objectName, attr.getName)
            value match {
              case c: CompositeData =>
                c.getCompositeType.keySet.foreach { key =>

                  // TODO: perhaps drill into the nested data types.
                  createDataSource(name, attr.getName, key).foreach {
                    objectGroup.data_sources.add(_)
                  }

                }
              case null => // no value set.. so we can't discover any more info.
              case _ =>
                log.warn("Did not CompositeData from the %s attribute %s.  Got %s", name, attr.getName, value)
            }

          case _ =>
            createDataSource(name, attr.getName).foreach {
              objectGroup.data_sources.add(_)
            }
        }
      }
    }
    map
  }
}
