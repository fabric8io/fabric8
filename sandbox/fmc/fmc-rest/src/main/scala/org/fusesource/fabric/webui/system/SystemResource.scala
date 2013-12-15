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

package io.fabric8.webui.system

import javax.ws.rs._
import core.Context
import javax.servlet.http.{HttpSession, HttpServletRequest, HttpServletResponse}
import org.codehaus.jackson.annotate.JsonProperty
import com.sun.jersey.api.core.ResourceContext
import io.fabric8.webui.{Services, BaseResource}
import io.fabric8.boot.commands.service.Create
import io.fabric8.boot.commands.service.Join
import io.fabric8.zookeeper.ZkDefs
import org.jclouds.compute.reference.ComputeServiceConstants
import scala.concurrent.ops._
import collection.JavaConversions._
import javax.security.auth.Subject


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

class CreateEnsembleDTO {
  @JsonProperty
  var global_resolver: String = _

  @JsonProperty
  var local_resolver: String = _

  @JsonProperty
  var manualip: String = _

  @JsonProperty
  var zk_password: String = _

  @JsonProperty
  var username: String = _

  @JsonProperty
  var password: String = _

  @JsonProperty
  var role: String = _

  @JsonProperty
  var max_port: String = _

  @JsonProperty
  var min_port: String = _

}

class JoinEnsembleDTO {
  @JsonProperty
  var zk_url: String = _

  @JsonProperty
  var password: String = _
}

@Path("system")
class SystemResource extends BaseResource {

  @Context
  var resource_context: ResourceContext = null

  @POST
  @Path("login")
  def login(@Context request: HttpServletRequest, @Context response: HttpServletResponse, @FormParam("username") username: String, @FormParam("password") password: String): Boolean = {
    val auth: Authenticator = resource_context.getResource(classOf[Authenticator])

    Option[Subject](auth.authenticate(username, password)) match {
      case Some(subject) =>
        val session: HttpSession = request.getSession(true)
        session.setAttribute("subject", subject);
        true
      case None =>
        false
    }
  }

  @GET
  @Path("logout")
  def logout(@Context request: HttpServletRequest): Boolean = {
    Services.invalidate_session(request)
  }



  @GET
  @Path("whoami")
  def whoami(@Context request: HttpServletRequest): Principal = {
    val session: HttpSession = request.getSession(false)
    val principal = new Principal()
    if (session != null) {
      principal.username = Services.jmx_username(request);
    }
    return principal
  }

  @GET
  @Path("status")
  def connected(@Context request: HttpServletRequest): ConnectionStatusDTO = {
    val rc = new ConnectionStatusDTO
    try {
      val zk = Services.curator
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
        rc.client_connected = Services.curator.getZookeeperClient.isConnected
        if (!rc.client_connected) {
          rc.client_connection_error = Services.curator.getState.toString
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
      rc.zk_cluster_service_available = Services.zk_cluster_bootstrap != null
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

  def isInt(value:String):Boolean = {
    try {
      Integer.parseInt(value)
      true
    } catch {
      case _ => false
    }
  }

  @POST
  @Path("status/create_ensemble")
  def create_ensemble(options: CreateEnsembleDTO): Unit = {
    require(options.username != null, "Must supply initial user account")
    require(options.password != null, "Must supply initial user account password")
    require(options.role != null, "Must supply initial user account role")
    require(options.max_port == null || options.max_port.equals("") || isInt(options.max_port), "Max port must be an integer, null or an empty string")

    require(options.max_port == null || options.max_port.equals("") || isInt(options.max_port), "Min port must be an integer, null or an empty string")

    spawn {
      try {
        options.zk_password match {
          case "" =>
            options.zk_password = null
          case _ =>
        }
        options.max_port match {
          case "" =>
            options.max_port = null
          case "0" =>
            options.max_port = null
          case _ =>
        }
        options.min_port match {
          case "" =>
            options.min_port = null
          case "0" =>
            options.min_port = null
          case _ =>
        }

        val create = Services.get_service(classOf[Create])
        create.setClean(true)
        create.setProfiles(Set(Services.profile_name))
        create.setGenerateZookeeperPassword(true)
        create.setZookeeperPassword(options.zk_password)
        create.setNewUser(options.username)
        create.setNewUserPassword(options.password)
        create.setNewUserRole(options.role)
        Option(options.max_port).foreach(x => create.setMaximumPort(Integer.parseInt(x)))
        Option(options.min_port).foreach(x => create.setMinimumPort(Integer.parseInt(x)))
        Option(options.global_resolver).foreach(System.setProperty(ZkDefs.GLOBAL_RESOLVER_PROPERTY, _))
        Option(options.local_resolver).foreach(System.setProperty(ZkDefs.LOCAL_RESOLVER_PROPERTY, _))
        Option(options.manualip).foreach(System.setProperty(ZkDefs.MANUAL_IP, _))
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
        join.setZookeeperPassword(options.password)
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
