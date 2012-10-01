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

package org.fusesource.fabric.webui.system

import javax.ws.rs._
import core.Context
import javax.servlet.http.{HttpSession, HttpServletRequest, HttpServletResponse}
import org.codehaus.jackson.annotate.JsonProperty
import com.sun.jersey.api.core.ResourceContext
import org.fusesource.fabric.webui.{Services, BaseResource}
import org.fusesource.fabric.boot.commands.service.Create
import org.fusesource.fabric.boot.commands.service.Join
import org.jclouds.compute.reference.ComputeServiceConstants
import scala.concurrent.ops._
import org.fusesource.fabric.zookeeper.ZkDefs


class Principal extends BaseResource {
  @JsonProperty var username: String = ""
  var password: String = ""
}

class ConnectionStatusDTO {
  @JsonProperty
  var client_valid: Boolean = _
  @JsonProperty
  var client_connected: Boolean = _
  @JsonProperty
  var client_connection_error: String = _
  @JsonProperty
  var zk_cluster_service_available: Boolean = _
  @JsonProperty
  var provision_complete: Boolean = _
  @JsonProperty
  var managed:Boolean = _
  @JsonProperty
  var has_backing_engine:Boolean = _
}

class JoinEnsembleDTO {
  @JsonProperty
  var zk_url: String = _
}

@Path("system")
class SystemResource extends BaseResource {

  @Context
  var resource_context: ResourceContext = null

  @POST
  @Path("login")
  def login(@Context request: HttpServletRequest, @Context response: HttpServletResponse, @FormParam("username") username: String, @FormParam("password") password: String): Boolean = {
    val auth: Authenticator = resource_context.getResource(classOf[Authenticator]);
    if (auth.authenticate(username, password)) {
      val session: HttpSession = request.getSession(true);
      session.setAttribute("username", username)
      return true;
    } else {
      return false;
    }
  }

  @GET
  @Path("logout")
  def logout(@Context request: HttpServletRequest): Boolean = {
    val session: HttpSession = request.getSession(false);
    if (session != null) {
      session.invalidate()
    }
    true
  }

  @GET
  @Path("whoami")
  def whoami(@Context request: HttpServletRequest): Principal = {
    val session: HttpSession = request.getSession(false);
    val principal = new Principal()
    if (session != null && session.getAttribute("username") != null) {
      principal.username = session.getAttribute("username").asInstanceOf[String]
    }
    return principal
  }

  @GET
  @Path("status")
  def connected(@Context request: HttpServletRequest): ConnectionStatusDTO = {
    val rc = new ConnectionStatusDTO
    try {
      val zk = Services.zoo_keeper
      if (zk != null) {
        rc.client_valid = true
      } else {
        rc.client_valid = false
      }
    } catch {
      case t: Throwable =>
        rc.client_valid = false
    }
    if (rc.client_valid) {
      try {
        rc.client_connected = Services.zoo_keeper.isConnected
        if (!rc.client_connected) {
          rc.client_connection_error = Services.zoo_keeper.getState.toString
        }

      } catch {
        case t: Throwable =>
          rc.client_connected = false
          rc.client_connection_error = t.getMessage
      }
    }
    try {
      rc.provision_complete = fabric_service.getCurrentContainer.isProvisioningComplete
    } catch {
      case t: Throwable =>
        rc.provision_complete = false
    }

    try {
      rc.managed = Services.managed
    } catch {
      case t: Throwable =>
        rc.managed = false
    }
    try {
      rc.zk_cluster_service_available = Services.zk_cluster_service != null
    } catch {
      case t: Throwable =>
        rc.zk_cluster_service_available = false
    }

    try {
      rc.has_backing_engine = resource_context.getResource(classOf[Authenticator]).auth_backing_engine != null
    } catch {
      case t:Throwable =>
      rc.has_backing_engine = false
    }
    rc
  }

  @POST
  @Path("status/create_ensemble")
  def create_ensemble: Unit = {
    spawn {
      try {
        val create = Services.get_service(classOf[Create])
        create.setClean(true)
        create.setProfile(Services.profile_name)
        System.setProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY, Services.resolver)
        create.setNonManaged(!Services.managed)
        create.run
      } catch {
        case t: Throwable => Services.LOG.warn("Exception creating ensemble: {}", t)
      }
    }
  }

  @POST
  @Path("status/join_ensemble")
  def join_ensemble(options: JoinEnsembleDTO): Unit = {
    spawn {
      try {

        val join = Services.get_service(classOf[Join])
        join.setZookeeperUrl(options.zk_url)
        join.setProfile(Services.profile_name)
        join.setNonManaged(true)
        //join.setNonManaged(!Services.managed)
        join.run
      } catch {
        case t: Throwable => Services.LOG.warn("Exception joining ensemble: {}", t)
      }
    }
  }

  @GET
  @Path("os_and_versions_map")
  def getOSAndVersions: String = new ComputeServiceConstants.ReferenceData().osVersionMapJson

}
