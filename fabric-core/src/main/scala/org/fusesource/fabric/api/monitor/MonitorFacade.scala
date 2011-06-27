/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.api.monitor

import javax.management.remote.JMXConnector
import org.fusesource.fabric.service.JmxTemplateSupport

object MonitorFacade {

  def list(jmxTemplate: JmxTemplateSupport): Array[MonitoredSetDTO] = {
    jmxTemplate.execute(new JmxTemplateSupport.JmxConnectorCallback[Array[MonitoredSetDTO]] {
      def doWithJmxConnector(connector: JMXConnector) = {
        val monitor: MonitorServiceFacade = jmxTemplate.getMBean(connector, classOf[MonitorServiceFacade], "org.fusesource.fabric", "type", "Monitor")
        val response = monitor.list
        if (response != null) {
          JsonCodec.decode(classOf[Array[MonitoredSetDTO]], response)
        } else {
          null
        }
      }
    })
  }

  def fetch(jmxTemplate: JmxTemplateSupport, fetch: FetchMonitoredViewDTO): MonitoredViewDTO = {
    jmxTemplate.execute(new JmxTemplateSupport.JmxConnectorCallback[MonitoredViewDTO] {
      def doWithJmxConnector(connector: JMXConnector) = {
        val monitor: MonitorServiceFacade = jmxTemplate.getMBean(connector, classOf[MonitorServiceFacade], "org.fusesource.fabric", "type", "Monitor")
        val request = JsonCodec.encode(fetch)
        val response = monitor.fetch(request)
        if (response != null) {
          JsonCodec.decode(classOf[MonitoredViewDTO], response)
        } else {
          null
        }
      }
    })
  }
}
