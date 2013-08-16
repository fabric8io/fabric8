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
package org.fusesource.fabric.webui.agents.activemq

import org.fusesource.fabric.api.Container
import org.fusesource.fabric.service.{ContainerCachingJmxTemplate, ContainerTemplate}
import collection.JavaConversions._
import org.fusesource.fabric.webui.agents.{ManagementExtension, ManagementExtensionFactory}
import org.fusesource.fabric.webui.agents.{ManagementExtension, ManagementExtensionFactory}
import scala.Some
import javax.ws.rs.{GET, PathParam, Path}
import org.fusesource.fabric.activemq.facade._
import org.fusesource.fabric.webui.BaseResource
import org.fusesource.fabric.activemq.facade.JmxTemplateBrokerFacade

object ActiveMQAgentResource extends ManagementExtensionFactory {
  def create(a: Container, jmx_username: String, jmx_password: String) = {
    if (a.getJmxDomains.contains("org.apache.activemq")) {
      Some(new ActiveMQAgentResource(a, jmx_username, jmx_password))
    } else {
      None
    }
  }
}


class ActiveMQAgentResource(val agent: Container, jmx_username: String, jmx_password: String) extends BaseResource with ManagementExtension {

  private val facade = new JmxTemplateBrokerFacade(agent_template(agent, jmx_username, jmx_password).getJmxTemplate())

  def id = "activemq"

  @GET
  override
  def get = contexts

  def contexts = facade.getBrokers.map(new BrokerResource(_)).toArray

  @Path("{id}")
  def context(@PathParam("id") id: String) = contexts.find(_.id == id).getOrElse {
    not_found
  }

}
