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
package org.fusesource.fabric.webui.agents

import java.lang.String
import javax.ws.rs._
import core.{Context, MediaType}
import org.codehaus.jackson.annotate.JsonProperty
import org.codehaus.jackson.map.ObjectMapper;
import java.net.URI
import collection.JavaConversions
import org.fusesource.fabric.webui._
import org.fusesource.fabric.webui.{Services, ByID, JsonProvider, BaseResource}
import org.fusesource.fabric.api._
import javax.servlet.http.HttpServletRequest

/**
 * Resource representing root agents resource.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @author ldywicki
 */

class MigrateContainerDTO {
  @JsonProperty
  var containers: Array[String] = _
  @JsonProperty
  var version: String = _
}

@Path("/agents")
class AgentsResource extends BaseResource {

  @Context
  var request:HttpServletRequest = null


  @GET
  override def get: Array[AgentResource] = fabric_service.getContainers.map(new AgentResource(_, request)).sortWith(ByID(_, _))

  @Path("{id}")
  def get(@PathParam("id") id: String): AgentResource = {
    val agent = get.find(_.id == id)
    agent getOrElse not_found
  }

  import JsonProvider.mapper

  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_JSON))
  def create(options: java.util.Map[String, Object]): Array[CreateContainerMetadata[_ <: CreateContainerOptions]] = {
    Services.LOG.debug("Got : " + options.getClass().getName() + " : " + options)

    val providerType = options.get("providerType").asInstanceOf[String]
    val name = options.get("name").asInstanceOf[String]
    val requested_version = options.remove("version").asInstanceOf[String]
    val requested_profiles = options.remove("profiles").asInstanceOf[String].split(", ")

    require(providerType != null, "type must be set")
    require(name != null, "agent name must be set")
    require(requested_version != null, "version must be set")
    require(requested_profiles != null, "profiles must be set")

    val version = fabric_service.getVersion(requested_version)
    val profiles = requested_profiles.map(version.getProfile(_))

    val agents: Array[CreateContainerMetadata[_ <: CreateContainerOptions]] = if (providerType == "child") {

      val value: CreateContainerChildOptions = mapper.convertValue(options, classOf[CreateContainerChildOptions])
      require(value.getParent != null, "parent must be set")
      value.setResolver(null)
      value.setZookeeperUrl(fabric_service.getZookeeperUrl())
      value.setZookeeperPassword(fabric_service.getZookeeperPassword())
      value.setJmxUser(Services.jmx_username(request))
      value.setJmxPassword(Services.jmx_password(request))
      fabric_service.createContainers(value)

    } else if (providerType == "ssh") {

      val value: CreateSshContainerOptions = mapper.convertValue(options, classOf[CreateSshContainerOptions])
      require(value.getHost != null, "host must be set")
      value.setNumber(1)
      value.setZookeeperUrl(fabric_service.getZookeeperUrl())
      value.setZookeeperPassword(fabric_service.getZookeeperPassword())
      fabric_service.createContainers(value)

    } else if (providerType == "jclouds") {

      val value: CreateJCloudsContainerOptions = mapper.convertValue(options, classOf[CreateJCloudsContainerOptions])
      require(value.getProviderName != null, "provider name must be set")

      val name = value.getProviderName

      try {
        val Array(provider_name, context_name) = name.split(" - ")
        value.setProviderName(provider_name)
        value.setContextName(context_name)

      } catch {
        case me:MatchError =>
          throw new RuntimeException("Unexpected provider name format, should be \"provder_name\" - \"context_name\"")

      }

      value.setZookeeperUrl(fabric_service.getZookeeperUrl())
      value.setZookeeperPassword(fabric_service.getZookeeperPassword())
      fabric_service.createContainers(value)

    } else {
      throw new RuntimeException("Unexpected container type, only \"child\", \"ssh\" and \"cloud\" are recognized")
    }

    if (agents == null) {
      throw new RuntimeException("Error creating containers, instances returned from service is null")
    }

    try {
      agents.foreach((x) => {
        val container = x.getContainer
        if (container != null) {
          container.setVersion(version)
          container.setProfiles(profiles)
        }
      })
      agents
    } catch {
      case t: Throwable =>
        val ensemble_server = options.get("ensembleServer")
        if (ensemble_server != null && ensemble_server.asInstanceOf[Boolean]) {
          null
        } else {
          throw t
        }
    }
  }

  @POST
  @Path("migrate")
  def migrate_containers(args: MigrateContainerDTO): Boolean = {
    require(args.containers != null && args.containers.length > 0, "Must specify one or more containers")
    require(args.version != null, "Must specify target version")
    val version = fabric_service.getVersion(args.version)
    args.containers.foreach(fabric_service.getContainer(_).setVersion(version))
    true
  }


}


