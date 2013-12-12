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

import io.fabric8.monitor.internal.Numbers
import Numbers._
import javax.management.ObjectName
import javax.management.openmbean.CompositeData
import io.fabric8.monitor.api.{Poller, PollerFactory, DataSourceDTO}

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
