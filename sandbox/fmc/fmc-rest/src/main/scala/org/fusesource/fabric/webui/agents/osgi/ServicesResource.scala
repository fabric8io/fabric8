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

import javax.ws.rs.{Path, PathParam}
import io.fabric8.api.Container
import io.fabric8.webui.BaseResource
import io.fabric8.internal.ContainerImpl

class ServicesResource(val agent: Container, jmx_username: String, jmx_password: String) extends BaseResource {

  private def services = agent.asInstanceOf[ContainerImpl].getServices(agent_template(agent, jmx_username, jmx_password))

  override def get: Array[ServiceResource] = services.map(new ServiceResource(_))

  @Path("{id}")
  def get(@PathParam("id") id: Int): ServiceResource = get.find(_.id == id) getOrElse not_found

}
