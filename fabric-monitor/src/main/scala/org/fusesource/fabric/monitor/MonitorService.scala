/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.monitor

import api.{FetchMonitoredViewDTO, JsonCodec, Monitor}
import internal.DefaultMonitor
import java.io.File
import plugins.DefaultJvmMonitorSetBuilder

trait MonitorServiceMBean {
  def fetch( fetch:Array[Byte] ):Array[Byte]
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
      val monitor:Monitor = new DefaultMonitor(datadir.getCanonicalPath+"/")
      monitor.poller_factories = MonitorDeamon.poller_factories

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
    val response = m.fetch(request)
    JsonCodec.encode(response)
  }

}

