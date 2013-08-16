/*
 * Copyright 2010 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.fusesource.fabric.webui.agents.monitor

import org.fusesource.fabric.api.Container
import org.fusesource.fabric.monitor.api._
import org.fusesource.fabric.monitor.MonitorDeamon
import org.fusesource.fabric.monitor.internal.{DefaultMonitor, ClassFinder}
import org.fusesource.fabric.monitor.plugins.{DefaultJvmMonitorSetBuilder}
import java.io.File
import javax.ws.rs.core.Response.Status
import javax.ws.rs._
import core.Response.Status._
import core.{MediaType, Response}
import scala.Array._

@Path("/stats/fetch")
class StatsResource {

  @POST
  @Produces(Array(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML))
  @Consumes(Array(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML))
  def fetch(fetch: FetchMonitoredViewDTO) = MonitorService.monitor.fetch(fetch).getOrElse(result(NOT_FOUND))

  private def result[T](value: Status, message: Any = null): T = {
    val response = Response.status(value)
    if (message != null) {
      response.entity(message)
    }
    throw new WebApplicationException(response.build)
  }

}

class AgentStatsResource(agent: Container) {
  @POST
  @Produces(Array(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML))
  @Consumes(Array(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML))
  def fetch(fetch: FetchMonitoredViewDTO) = {
    "{ 'foo': 123 }"
  }

}

object MonitorService {

  val stats_directory = new File("target")
  stats_directory.mkdirs()

  val monitor = new DefaultMonitor(stats_directory.getCanonicalPath + "/")

  monitor.poller_factories = MonitorDeamon.poller_factories

  val monitorSet = new DefaultJvmMonitorSetBuilder().apply()
  monitor.configure(List(monitorSet))

}
