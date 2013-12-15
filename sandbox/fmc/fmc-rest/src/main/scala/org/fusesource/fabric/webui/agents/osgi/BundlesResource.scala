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

import io.fabric8.api.data.BundleInfo
import io.fabric8.api.Container
import javax.ws.rs.{Path, GET, PathParam}
import io.fabric8.webui.BaseResource
import io.fabric8.internal.ContainerImpl

class BundlesResource(val agent: Container, jmx_username: String, jmx_password: String) extends BaseResource {

  private def bundles: Array[BundleInfo] = agent.asInstanceOf[ContainerImpl].getBundles(agent_template(agent, jmx_username, jmx_password))

  @GET
  override def get: Array[BundleResource] = {
    bundles.map(b => new BundleResource(b))
  }

  @Path("{id}")
  def get(@PathParam("id") id: Int): BundleResource = get.find(_.id == id) getOrElse not_found

}
