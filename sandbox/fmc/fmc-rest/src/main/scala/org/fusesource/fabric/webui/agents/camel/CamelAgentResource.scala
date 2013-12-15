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
package io.fabric8.webui.agents.camel

import io.fabric8.api.Container
import io.fabric8.service.{ContainerCachingJmxTemplate, ContainerTemplate}
import collection.JavaConversions._
import io.fabric8.webui.agents._
import scala.Some
import io.fabric8.webui.agents.{ManagementExtension, ManagementExtensionFactory}
import scala.Some
import javax.ws.rs.{GET, PathParam, Path}
import io.fabric8.webui.{ByID, BaseResource}
import io.fabric8.webui.{ByID, BaseResource}
import io.fabric8.camel.facade.JmxTemplateCamelFacade

object CamelAgentResource extends ManagementExtensionFactory {
  def create(a: Container, jmx_username: String, jmx_password: String) = {
    if (a.getJmxDomains.contains("org.apache.camel")) {
      Some(new CamelAgentResource(a, jmx_username, jmx_password))
    } else {
      None
    }
  }
}


class CamelAgentResource(val agent: Container, jmx_username: String, jmx_password: String) extends BaseResource with ManagementExtension {

  private val facade = new JmxTemplateCamelFacade(agent_template(agent, jmx_username, jmx_password).getJmxTemplate())

  def id = "camel"

  @GET
  override
  def get = contexts

  def contexts = facade.getCamelContexts.map(new CamelContextResource(facade, _)).toArray.sortWith(ByID(_, _))

  @Path("{id}")
  def context(@PathParam("id") id: String) = contexts.find(_.id == id).getOrElse {
    not_found
  }

}
