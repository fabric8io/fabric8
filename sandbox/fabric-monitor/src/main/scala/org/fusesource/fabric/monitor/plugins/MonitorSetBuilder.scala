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

import jmx.JmxDataSourceRegistry
import io.fabric8.monitor.api.{PollDTO, MonitoredSetDTO, DataSourceDTO, ArchiveDTO}

/**
 * A helper class for building a monitor set
 */
abstract class MonitorSetBuilder(name: String) {
  var set: MonitoredSetDTO = _
  val jmxFactory = new JmxDataSourceRegistry()

  def apply(): MonitoredSetDTO = {
    set = new MonitoredSetDTO(name)
    configure
    set
  }

  def configure: Unit

  def archive( window: String, step: String = null,consolidation: String = "AVERAGE") = {
    val a = new ArchiveDTO(consolidation, step, window)
    set.archives.add(a)
    a
  }

  def dataSource(poll: PollDTO, id: String, name: String = null, description: String = null, kind: String = "gauge", heartbeat: String = "1s", min: Double = Double.NaN, max: Double = Double.NaN) = {
    var n = if (name == null) id else name
    var d = if (description == null) n else description
    val ds = DataSourceEnricher(new DataSourceDTO(id, n, d, kind, heartbeat, min, max, poll))
    addDataSource(ds)
    ds
  }

  def jmxDataSource(objectName: String, attributeName: String, key: String=null): Option[DataSourceDTO] = {
    def error_message = if (key==null)
      "No attribute " + attributeName + " in MBean " + objectName
    else
      "No key " + key + " in MBean " + objectName + " attribute " + attributeName

    val answer = jmxFactory.createDataSource(objectName, attributeName, key)
    addDataSource(answer, error_message)
  }

  def processPoll(name: String) = {
    // TODO!
    new ProcessPollDTO()
  }

  def addDataSource(ds: DataSourceDTO): Unit = {
    set.data_sources.add(ds)
  }

  def addDataSource(answer: Option[DataSourceDTO], message: => String): Option[DataSourceDTO] = {
    answer match {
      case Some(ds) => addDataSource(ds)
      case _ =>
    }
    answer
  }


}
