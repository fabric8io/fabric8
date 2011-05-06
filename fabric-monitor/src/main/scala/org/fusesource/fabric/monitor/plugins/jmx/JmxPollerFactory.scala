package org.fusesource.fabric.monitor.plugins
package jmx

import org.fusesource.fabric.monitor.api.{Poller, DataSourceDTO, PollerFactory}
import javax.management.openmbean.CompositeData
import javax.management.ObjectName

/**
 * A PollerFactory for dealing with JMX Attribute polls
 */
class JmxPollerFactory extends PollerFactory with JmxMixin {
  def jaxb_package = getClass.getName.replaceAll("""\.[^\.]*$""", "")

  def accepts(source: DataSourceDTO) = source.poll match {
    case x: MBeanAttributePollDTO => true
    case _ => false
  }

  protected def toNumber(value: AnyRef, message: String): Double = {
    value match {
      case n: Number => n.doubleValue
      case x: AnyRef =>
        x.toString.toDouble
      case v => throw new ValueNotNumberException(message, v)
    }
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
            toNumber(value, "MBean " + mbean + " attribute " + attribute)
          }
        }
      case mbeanPoll: MBeanAttributeKeyPollDTO =>
        import mbeanPoll._
        val objectName = new ObjectName(mbean)
        new Poller {
          val source = s

          def close = {
          }

          def poll = {
            lazy val message = "MBean " + mbean + " attribute " + attribute + " key " + key
            mbeanServer.getAttribute(objectName, attribute) match {
              case cd: CompositeData => toNumber(cd.get(key), message)
              case _ => throw new IllegalArgumentException(message + " is not a CompositeData value")
            }
          }
        }
      case p => throw new IllegalArgumentException("Cannot create a Poller for " + p)
    }
  }
}

class ValueNotNumberException(message: String, val value: AnyRef)
        extends IllegalArgumentException(message + " is not a number " + value + (if (value != null) {
          " of type " + value.getClass.getName
        } else " it was null")) {

}