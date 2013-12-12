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

package io.fabric8.monitor.internal

import javax.management.ObjectName
import org.fusesource.scalate.util.Measurements._
import io.fabric8.monitor.plugins.jmx.{JmxConstants, JmxDataSourceRegistry}
import io.fabric8.monitor.MonitorDeamon
import io.fabric8.monitor.api.DataSourceDTO

object Value {
  val jmxFactory = new JmxDataSourceRegistry()
  val pollers = MonitorDeamon.poller_factories

  def defined(values: Value*): List[Value] = {
    values.filter(_.isDefined).toList
  }

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
    // TODO deal with remote containers too...
    JmxObjectValue(key)
  }
}

import Value._

sealed abstract class Value {

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

  def double: Double = Numbers.toNumber(get, name)

  /*
  def percent(key1: String, key2: String, defaultValue: String = ""): String = {
    try {
      val n1 = apply(key1).double
      val n2 = apply(key2).double
      val percent = n1 / n2 * 100
      // TODO get the current locale
      percent
    } catch {
      case e => log.debug("Caught: " + e, e)
      defaultValue
    }
  }
  */
}

object NoValue extends Value {
  def apply(key: String) = this

  override def isDefined = false

  override def toString = "NoValue"
}

case class JmxObjectValue(oname: String) extends Value {
  val objectName = new ObjectName(oname)

  def apply(attr: String): Value = {
    jmxFactory.createDataSource(oname, attr) match {
      case Some(dto) => new JmxObjectAttributeValue(this, attr, dto)
      case None => NoValue
    }
  }
}

case class JmxObjectAttributeValue(parent:JmxObjectValue, attributeName: String, dto: DataSourceDTO) extends Value {
  def apply(key: String) = {
    jmxFactory.createDataSource(parent.oname, attributeName, key) match {
      case Some(dto) => new JmxObjectAttributeKeyValue(this, key, dto)
      case None => NoValue
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

  override def toString = "JmxObjectAttributeValue(" + parent.oname + ", " + attributeName + ")"
}

case class JmxObjectAttributeKeyValue(parent:JmxObjectAttributeValue, key: String, dto: DataSourceDTO) extends Value {

  def apply(subkey: String) = {
    val sub_key = key + JmxConstants.SEPARATOR + subkey
    jmxFactory.createDataSource(parent.parent.oname, parent.attributeName, sub_key) match {
      case Some(dto) => new JmxObjectAttributeKeyValue(parent, sub_key, dto)
      case None => NoValue
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

  override def toString = "JmxObjectAttributeKeyValue(" + parent.parent.oname + ", " + parent.attributeName + ", " + key + ")"
}
