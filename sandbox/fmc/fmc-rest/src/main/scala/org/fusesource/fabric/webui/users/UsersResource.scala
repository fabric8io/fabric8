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
package io.fabric8.webui.users

import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.webui.{Services, BaseResource}
import io.fabric8.webui.{Services, BaseResource}
import javax.ws.rs._
import core.Context
import org.apache.karaf.jaas.boot.principal.{UserPrincipal, RolePrincipal}
import com.sun.jersey.api.core.ResourceContext
import io.fabric8.webui.system.Authenticator
import javax.servlet.http.HttpServletRequest

class CreateUserDTO {
  @JsonProperty
  var password: String = _
}

class CreateRoleDTO {
  @JsonProperty
  var id: String = _
}

class RoleResource(@Context rc: ResourceContext, self: RolePrincipal, user: String) extends BaseResource {

  val backing_engine = rc.getResource(classOf[Authenticator]).auth_backing_engine

  @JsonProperty
  var id = self.getName

  @PUT
  def create(role: CreateRoleDTO) = {
    backing_engine.addRole(user, role.id)
  }

  @DELETE
  def delete: Unit = {
    backing_engine.deleteRole(user, id)
  }

}

class UserResource(@Context rc: ResourceContext) extends BaseResource {

  val backing_engine = rc.getResource(classOf[Authenticator]).auth_backing_engine

  @JsonProperty
  var id = ""

  @JsonProperty
  def roles = {
    iter(backing_engine.listRoles(new UserPrincipal(id))).map(new RoleResource(rc, _, id)).toArray
  }

  @DELETE
  def delete: Unit = {
    backing_engine.deleteUser(id)
    if (id.equals(Services.jmx_username(request))) {
      Services.invalidate_session(request)
    }
  }

  @PUT
  def create(user: CreateUserDTO) = {
    backing_engine.addUser(id, user.password)
    if (id.equals(Services.jmx_username(request))) {
      val session = Services.get_session(request)
      session.invalidate()
    }
  }

  @Path("roles/{role}")
  def assigned(@PathParam("role") role: String): RoleResource = {
    val roleResource = new RoleResource(rc, new RolePrincipal(role), id)
    roleResource
  }

}

@Path("/users")
class UsersResource(@Context rc: ResourceContext) extends BaseResource {

  val backing_engine = rc.getResource(classOf[Authenticator]).auth_backing_engine

  @GET
  override def get(): Array[UserResource] = {
    iter(backing_engine.listUsers()).map {
      principal =>
        val user = new UserResource(rc)
        user.request = request
        user.id = principal.getName
        user
    }.toArray
  }

  @Path("{id}")
  def assigned(@PathParam("id") id: String): UserResource = {
    val user = new UserResource(rc)
    user.request = request
    user.id = id
    user
  }

}
