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

import javax.ws.rs._
import core.MediaType
import io.fabric8.api.{Profile, Container}
import org.codehaus.jackson.annotate.JsonProperty
import scala.Array._
import io.fabric8.webui._
import scala.Some


class AddProfileDTO {
  @JsonProperty
  var client_ids: java.util.List[String] = _
}

/**
 * Resource which represents agent profiles.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @author ldywicki
 */

class ContainerProfileResource(val self:Profile, val container:Container)
  extends BaseResource with HasID {

  @JsonProperty
  def id = self.getId

  @JsonProperty
  def version = self.getVersion

  @JsonProperty
  def parents = self.getParents.map(_.getId)

  @JsonProperty
  def _abstract = self.isAbstract

  @JsonProperty
  def _hidden = self.isHidden

  @JsonProperty
  def _locked = self.isLocked

  @JsonProperty
  def profile_attributes = self.getAttributes

  @DELETE
  def delete = {
    val profiles = container.getProfiles
    container.setProfiles(profiles.filterNot(_.getId == self.getId).toArray)
  }

}

class ProfilesResource(val agent: Container)
  extends BaseResource {

  @GET
  override def get: Array[ContainerProfileResource] = agent.getProfiles.map(new ContainerProfileResource(_, agent)).sortWith(ByID(_, _))

  @Path("{id}")
  def assigned(@PathParam("id") id: String): ContainerProfileResource = {
    get.find(_.id == id).getOrElse(not_found)
  }

  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def add(profile: AddProfileDTO) = {

    val profiles = agent.getProfiles.toBuffer

    profile.client_ids.foreach((x) => {
      agent.getVersion.getProfiles.find(_.getId.equals(x)) match {
        case Some(p) =>
          profiles += p
        case None =>
          throw new IllegalArgumentException("Profile " + x + " cannot be found")
      }
    })

    agent.setProfiles(profiles.toArray)
    new ProfilesResource(agent)
  }

}
