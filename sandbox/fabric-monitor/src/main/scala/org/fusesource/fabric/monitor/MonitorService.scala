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

package io.fabric8.monitor

import internal.DefaultMonitor
import java.io.File
import plugins.DefaultJvmMonitorSetBuilder
import plugins.jmx.JmxDataSourceRegistry
import io.fabric8.monitor.api._

trait MonitorServiceMBean extends MonitorServiceFacade {
}

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class MonitorService extends MonitorServiceMBean {

  var datadir:File = _

  @volatile
  var monitor:Monitor = _

  def setDatadir(datadir:File) = {
    this.datadir=datadir
  }

  def start():Unit = this.synchronized {
    if ( monitor==null ) {
      println("Starting Monitor Sevice at: "+datadir);
      datadir.mkdirs()
      monitor = new DefaultMonitor(datadir.getCanonicalPath+"/")
      monitor.poller_factories = MonitorDeamon.poller_factories

      // to dump the mbeans available...
//      val registry = new JmxDataSourceRegistry()
//      val answer = registry.findSources()
//      for ((d, a) <- answer) {
//        a.dump(0, true)
//      }

      val monitorSet = new DefaultJvmMonitorSetBuilder().apply()
      monitor.configure(List(monitorSet))
    }
  }

  def stop():Unit = this.synchronized {
    if ( monitor == null ) {
      println("Stopping Monitor Sevice");
      monitor.close
      monitor = null
    }
  }

  def fetch(fetch: Array[Byte]):Array[Byte] = {
    val m = monitor
    if ( m==null ) {
      return null;
    }
    val request = JsonCodec.decode(classOf[FetchMonitoredViewDTO], fetch)
    val result = m.fetch(request)
    result.map(JsonCodec.encode(_)).getOrElse(null)
  }

  def list: Array[Byte] = {
    val m = monitor
    if ( m==null ) {
      return null;
    }
    JsonCodec.encode(m.list)
  }
}

