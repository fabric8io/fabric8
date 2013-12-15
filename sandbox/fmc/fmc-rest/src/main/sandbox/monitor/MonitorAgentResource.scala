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
package io.fabric8.webui.agents.monitor

import io.fabric8.api.Container
import io.fabric8.webui.agents._
import scala.Some
import io.fabric8.webui.agents.{ManagementExtension, ManagementExtensionFactory}
import io.fabric8.service.{ContainerTemplate, ContainerCachingJmxTemplate}
import javax.ws.rs.{POST, Path}
import io.fabric8.monitor.api.{MonitorFacade, FetchMonitoredViewDTO}
import javax.ws.rs.core.Response
import io.fabric8.webui.BaseResource

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object MonitorAgentResource extends ManagementExtensionFactory {
  def create(a: Container, jmx_username: String, jmx_password: String) = {
    if (a.isAlive && a.getJmxDomains.contains("io.fabric8")) {
      Some(new MonitorAgentResource(a, jmx_username, jmx_password))
    } else {
      None
    }
  }
}

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class MonitorAgentResource(val agent: Container, jmx_username: String, jmx_password: String) extends BaseResource with ManagementExtension {

  def id: String = "monitor"

  @POST
  @Path("fetch")
  def fetch(fetch: FetchMonitoredViewDTO) = {

    val template = agent_template(agent, jmx_username, jmx_password)
    val response = MonitorFacade.fetch(template.getJmxTemplate(), fetch)
    if (response == null) {
      respond(Response.Status.NOT_FOUND)
    }
    response
  }

}
