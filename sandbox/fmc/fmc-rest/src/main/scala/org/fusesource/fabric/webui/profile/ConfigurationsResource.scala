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
package io.fabric8.webui.profile

import collection.JavaConversions._

import io.fabric8.api.Profile
import javax.ws.rs._
import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.webui.{ByID, BaseResource}
import io.fabric8.webui.{ByID, BaseResource}

class ConfigurationsResource(profile: Profile) extends BaseResource {

  @JsonProperty
  def entries: Array[ConfigurationResource] = profile.getConfigurations
    .filterKeys(_ != "io.fabric8.agent")
    .map {
    case (k, v) =>
      new ConfigurationResource(profile, k, v)
  }.toArray.sortWith(ByID(_, _))

  @Path("{id}")
  def pids(@PathParam("id") id: String): ConfigurationResource = {
    entries.find(_.id == id).getOrElse {
      not_found
    }
  }

  @PUT
  def create(@FormParam("pid") id: String) = {
    if (id == "io.fabric8.agent") {
      throw new IllegalArgumentException("Cannot override agent configuration");
    }

    val map = new java.util.HashMap[String, String]
    var configurations = profile.getConfigurations
    configurations.put(id, map)
    profile.setConfigurations(configurations)

    new ConfigurationResource(profile, id, map)
  }
}
