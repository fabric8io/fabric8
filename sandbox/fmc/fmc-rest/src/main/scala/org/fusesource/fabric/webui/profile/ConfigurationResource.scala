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

import io.fabric8.api.Profile
import javax.ws.rs._
import collection.mutable.Map
import collection.JavaConversions._
import com.sun.jersey.api.view.Viewable
import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.webui.{BaseResource, HasID}
import io.fabric8.webui.{HasID, BaseResource}

class CreateConfigurationEntryDTO {
  @JsonProperty
  var id: String = _
  @JsonProperty
  var value: String = _
}

/**
 * Base class for configuration resource.
 *
 * @author ldywicki
 */
class ConfigurationResource(
                             val profile: Profile,
                             val _id: String,
                             val values_map: Map[String, String]
                             ) extends BaseResource with HasID {

  @JsonProperty
  def id = _id

  @JsonProperty
  def entries = (values_map.map {
    case (k, v) => entry(k, v)
  }).toArray

  def entry(key: String, value: String) = new ConfigurationEntryResource(profile, id, key, value)

  @Path("{key}")
  def get(@PathParam("key") key: String) =
    entries.find(_.id == key).getOrElse(not_found)

  @PUT
  def put(dto: CreateConfigurationEntryDTO) = {
    val key = dto.id
    val value = dto.value

    val cfg = profile.getConfigurations
    if (cfg.getOrElseUpdate(id, Map[String, String]()).contains(key)) {
      throw new IllegalArgumentException("Configuration entry with key " + key + " already exists");
    }
    cfg.get(id).put(key, value)
    profile.setConfigurations(cfg)
    this.entry(key, value)
  }

  @DELETE
  def delete(): Viewable = {
    val cfg = profile.getConfigurations()
    cfg.remove(id)
    profile.setConfigurations(cfg)

    new Viewable("removed", this, this.getClass)
  }
}
