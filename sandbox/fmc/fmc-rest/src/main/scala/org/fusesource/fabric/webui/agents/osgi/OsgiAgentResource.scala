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
package io.fabric8.webui.agents.osgi

import io.fabric8.api.Container
import io.fabric8.webui.agents._
import scala.Some
import io.fabric8.webui.agents.{ManagementExtension, ManagementExtensionFactory}
import scala.Some
import javax.ws.rs.Path
import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.webui.BaseResource

object OsgiAgentResource extends ManagementExtensionFactory {
  def create(a: Container, jmx_username: String, jmx_password: String) = {
    if (a.getJmxDomains.contains("osgi.core")) {
      Some(new OsgiAgentResource(a, jmx_username, jmx_password))
    } else {
      None
    }
  }
}

/**
 * Resource representing karaf instance.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @author ldywicki
 */
class OsgiAgentResource(val agent: Container, jmx_username: String, jmx_password: String) extends BaseResource with ManagementExtension {
  def id = "osgi"

  @JsonProperty
  def bundles = new BundlesResource(agent, jmx_username, jmx_password).get

  @JsonProperty
  def services = new ServicesResource(agent, jmx_username, jmx_password).get

  @Path("bundles")
  def bundles_resource = new BundlesResource(agent, jmx_username, jmx_password)

  @Path("services")
  def services_resource = new ServicesResource(agent, jmx_username, jmx_password)

}



