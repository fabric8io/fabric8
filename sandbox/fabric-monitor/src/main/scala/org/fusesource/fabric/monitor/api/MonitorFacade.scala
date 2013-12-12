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

package io.fabric8.monitor.api

import javax.management.remote.JMXConnector
import io.fabric8.service.JmxTemplateSupport
import io.fabric8.monitor.api._

object MonitorFacade {

  def list(jmxTemplate: JmxTemplateSupport): Array[MonitoredSetDTO] = {
    jmxTemplate.execute(new JmxTemplateSupport.JmxConnectorCallback[Array[MonitoredSetDTO]] {
      def doWithJmxConnector(connector: JMXConnector) = {
        val monitor: MonitorServiceFacade = jmxTemplate.getMBean(connector, classOf[MonitorServiceFacade], "io.fabric8", "type", "Monitor")
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
        val monitor: MonitorServiceFacade = jmxTemplate.getMBean(connector, classOf[MonitorServiceFacade], "io.fabric8", "type", "Monitor")
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
