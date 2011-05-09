package org.fusesource.fabric.monitor
package api

import plugins.jmx.JmxDataSourceRegistry
import javax.management.ObjectName
import org.fusesource.scalate.util.Measurements._

object Value {
  val jmxFactory = new JmxDataSourceRegistry()
  val pollers = MonitorDeamon.poller_factories

  def apply(keys: String*): Value = {
    if (keys.size == 0) {
      NoValue
    } else {
      var answer = root(keys.head)
      for (k <- keys.tail) {
        answer = answer(k)
      }
      answer
    }
  }

  def root(key: String): Value = {
    // TODO deal with remote agents too...
    JmxObjectValue(key)
  }
}

import Value._

abstract class Value {

  /**
   * Navigates to a child value
   */
  def apply(key: String): Value

  /**
   * Returns the actual value
   */
  def get: Any = None

  def name: String = ""

  def description: String = ""

  /**
   * Returns true if this value is defined
   */
  def isDefined: Boolean = true


  // helper methods to display the values as nicely formatted values
  def bytes(defaultValue: String): String = byte(get, defaultValue)
  def millis(defaultValue: String): String = milli(get, defaultValue)
  def seconds(defaultValue: String): String = second(get, defaultValue)

  def bytes: String = bytes("")
  def millis: String = millis("")
  def seconds: String = seconds("")

}

object NoValue extends Value {
  def apply(key: String) = this

  override def isDefined = false

  override def toString = "NoValue"
}

case class JmxObjectValue(oname: String) extends Value {
  val objectName = new ObjectName(oname)

  def apply(key: String): Value = {
    jmxFactory.createDataSource(objectName, key) match {
      case Some(dto) => new JmxObjectAttributeValue(objectName, key, dto)
      case None => NoValue
    }
  }
}

class JmxObjectAttributeValue(objectName: ObjectName, attributeName: String, dto: DataSourceDTO) extends Value {
  def apply(key: String) = {
    val childDto = dto.getChild(key)
    if (childDto != null) {
       new JmxObjectAttributeKeyValue(objectName, attributeName, key, childDto)
    } else {
      NoValue
    }
  }

  override def name = dto.name

  override def description = dto.description

  override def get = {
    pollers.find(_.accepts(dto)) match {
      case Some(pf) =>
        val poller = pf.create(dto)
        val answer = poller.poll
        poller.close
        answer
      case _ => None
    }
  }

  override def toString = "JmxObjectAttributeValue(" + objectName + ", " + attributeName + ")"
}

class JmxObjectAttributeKeyValue(objectName: ObjectName, attributeName: String, key: String, dto: DataSourceDTO) extends JmxObjectAttributeValue(objectName, attributeName, dto) {
  override def toString = "JmxObjectAttributeKeyValue(" + objectName + ", " + attributeName + ", " + key + ")"
}