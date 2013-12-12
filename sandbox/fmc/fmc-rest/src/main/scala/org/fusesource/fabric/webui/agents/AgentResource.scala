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
package io.fabric8.webui.agents

import java.lang.String
import javax.ws.rs._
import core.Context
import scala.Array._
import io.fabric8.webui._
import io.fabric8.api.{Container, CreateContainerMetadata}
import org.codehaus.jackson.annotate.JsonProperty
import javax.servlet.http.HttpServletRequest
import io.fabric8.service.jclouds.CreateJCloudsContainerMetadata
import javax.security.auth.Subject
import java.security.PrivilegedExceptionAction

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class AgentResource(val agent: Container) extends BaseResource with HasID {

  def parent_agent = agent.getParent

  @JsonProperty
  def id = agent.getId

  ////////////////////////////////////////////////
  // Mapping to nice JSON objects.
  ////////////////////////////////////////////////
  @JsonProperty
  def location = agent.getLocation

  @JsonProperty
  def alive = agent.isAlive

  @JsonProperty
  def root = agent.isRoot

  @JsonProperty
  def version = agent.getVersion.getId

  @JsonProperty
  def ssh_url = agent.getSshUrl

  @JsonProperty
  def jmx_url = agent.getJmxUrl

  @JsonProperty
  def _type = agent.getType

  @JsonProperty
  def ensemble_server = agent.isEnsembleServer

  @JsonProperty
  def provision_status = agent.getProvisionStatus.toLowerCase.split(" ").map(_.capitalize) mkString " "

  @JsonProperty
  def provision_complete = agent.isProvisioningComplete

  @JsonProperty
  def provision_pending = agent.isProvisioningPending

  @JsonProperty
  def provision_result = agent.getProvisionResult

  @JsonProperty
  def provision_exception = agent.getProvisionException

  @JsonProperty
  def local_ip = Option[String](agent.getLocalIp).getOrElse("")

  @JsonProperty
  def local_hostname = Option[String](agent.getLocalHostname).getOrElse("")

  @JsonProperty
  def public_ip = Option[String](agent.getPublicIp).getOrElse("")

  @JsonProperty
  def public_hostname = Option[String](agent.getPublicHostname).getOrElse("")

  @JsonProperty
  def manual_ip = Option[String](agent.getManualIp).getOrElse("")

  @JsonProperty
  def resolver = Option[String](agent.getResolver).getOrElse("")

  @GET
  @Path("provisioned_bundles")
  def provisioned_bundles = agent.getProvisionList

  @PUT
  @Path("resolver")
  def set_resolver(value: String): Unit = agent.setResolver(value)

  @PUT
  @Path("manual_ip")
  def set_manual_ip(value: String): Unit = agent.setManualIp(value)

  @PUT
  @Path("local_ip")
  def set_local_ip(value: String): Unit = agent.setLocalIp(value)

  @PUT
  @Path("local_hostname")
  def set_local_hostname(value: String): Unit = agent.setLocalHostname(value)

  @PUT
  @Path("public_ip")
  def set_public_ip(value: String): Unit = agent.setPublicIp(value)

  @PUT
  @Path("public_hostname")
  def set_public_hostname(value: String): Unit = agent.setPublicHostname(value)

  @JsonProperty
  def provision_indicator = if (!managed) {
    "spacer.gif"
  } else if (!alive) {
    "gray-dot.png"
  } else if (provision_pending) {
    "pending.gif"
  } else if (provision_status == "Success") {
    "green-dot.png"
  } else {
    "red-dot.png"
  }

  @JsonProperty
  def alive_status = if (alive) {
    "Online"
  } else {
    "Offline"
  }

  @JsonProperty
  def alive_indicator = if (alive) {
    "green-dot.png"
  } else {
    "gray-dot.png"
  }

  @JsonProperty
  def managed = agent.isManaged

  @JsonProperty
  @GET
  @Path("metadata")
  def metadata: CreateContainerMetadata[_] = agent.getMetadata

  def as_jcloud_metadata(metadata: CreateContainerMetadata[_]) = if (metadata.isInstanceOf[CreateJCloudsContainerMetadata]) {
    metadata.asInstanceOf[CreateJCloudsContainerMetadata]
  } else {
    not_found
  }

  @JsonProperty
  def has_pk = try {
    if (credential != null) {
      true
    } else {
      false
    }
  } catch {
    case t: Throwable => false
  }

  @GET
  @Path("pk.pem")
  @Produces(Array("application/x-pem-file"))
  def credential = {
    val jclouds_metadata = as_jcloud_metadata(metadata)
    if (jclouds_metadata.getCreateOptions.getProviderName == "aws-ec2") {
      jclouds_metadata.getCredential
    } else {
      not_found
    }
  }

  @GET
  @Path("password")
  def password = {
    val jclouds_metadata = as_jcloud_metadata(metadata)
    if (jclouds_metadata.getCreateOptions.getProviderName.startsWith("cloudservers")) {
      jclouds_metadata.getCredential
    } else {
      not_found
    }
  }


  //  @JsonProperty
  //  def jmx_domains = agent.getJmxDomains
  @JsonProperty
  def parent = Option(agent.getParent).map(_.getId).getOrElse(null)

  @JsonProperty
  def profiles = new ProfilesResource(agent).get

  @Path("profiles")
  def profiles_resource = new ProfilesResource(agent)

  @JsonProperty
  @GET
  @Path("extensions")
  def extensions: Array[String] = {
    ManagementExtensionFactory.extensions(agent, Services.jmx_username(request), Services.jmx_password(request)).map(_.id).toArray
  }

  @Path("extensions/{id}")
  def extensions(@PathParam("id") id: String) = {
    ManagementExtensionFactory.extensions(agent, Services.jmx_username(request), Services.jmx_password(request)).find(_.id == id) getOrElse not_found
  }

  @JsonProperty
  @GET
  @Path("children")
  def children: Array[String] = {
    fabric_service.getContainers.filter(a => a.getParent != null && a.getParent.getId == id).map(_.getId).toArray
  }

  @PUT
  @Path("location")
  def set_location(value: String): Unit = agent.setLocation(value)

  // common operations
  @POST
  @Path("stop")
  def stop: Unit = {
    Subject.doAs(subject, new PrivilegedExceptionAction[Object] {
      def run():Object = {
        agent.stop
        null
      }
    })
  }

  @POST
  @Path("start")
  def start: Unit = {
    Subject.doAs(subject, new PrivilegedExceptionAction[Object] {
      def run():Object = {
        agent.start
        null
      }
    })
  }

  @DELETE
  def delete = {
    Subject.doAs(subject, new PrivilegedExceptionAction[Object] {
      def run():Object = {
        agent.destroy
        null
      }
    })
    destroy
  }

  @POST
  @Path("destroy")
  def destroy: Unit = {
    stop
    delete
  }

}



