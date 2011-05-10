/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.monitor.plugins.jmx

import org.fusesource.fabric.monitor.api.DataSourceDTO
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import java.util.ArrayList
import java.util.List

/**
 * Represents a tree of data source values
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@XmlRootElement(name = "group")
@XmlAccessorType(XmlAccessType.FIELD) class DataSourceGroup {
  def this() {
    this ()
  }

  def this(id: String) {
    this ()
    `this`(id, id)
  }

  def this(id: String, name: String) {
    this ()
    this.id = id
    this.name = name
  }

  override def equals(o: AnyRef): Boolean = {
    if (this eq o) return true
    if (o eq null || getClass ne o.getClass) return false
    var that: DataSourceGroup = o.asInstanceOf[DataSourceGroup]
    if (if (id ne null) !id.equals(that.id) else that.id ne null) return false
    if (if (name ne null) !name.equals(that.name) else that.name ne null) return false
    if (if (description ne null) !description.equals(that.description) else that.description ne null) return false
    if (if (children ne null) !children.equals(that.children) else that.children ne null) return false
    if (if (data_sources ne null) !data_sources.equals(that.data_sources) else that.data_sources ne null) return false
    return true
  }

  override def hashCode: Int = {
    var result: Int = 0
    var temp: Long = 0L
    result = if (id ne null) id.hashCode else 0
    result = 31 * result + (if (name ne null) name.hashCode else 0)
    return result
  }

  override def toString: String = {
    return "DataSourceGroupDTO{" + "description='" + description + '\'' + ", id='" + id + '\'' + ", name='" + name + '\'' + '}'
  }

  @XmlAttribute var id: String = null
  @XmlAttribute var name: String = null
  @XmlAttribute var description: String = null
  @XmlElement(name = "children") var children: List[DataSourceGroup] = new ArrayList[DataSourceGroup]
  @XmlElement(name = "data_source") var data_sources: List[DataSourceDTO] = new ArrayList[DataSourceDTO]
}