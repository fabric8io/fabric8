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
import org.codehaus.jackson.annotate.JsonProperty
import javax.ws.rs.{POST, DELETE}
import io.fabric8.webui.BaseResource

/**
 * Base class for configuration values. Provides write logic related to singular values in configuration.
 *
 * @author ldywicki
 */
class ConfigurationEntryResource(

                                  val profile: Profile,
                                  val config_pid: String,
                                  val _id: String,
                                  val _value: String
                                  ) extends BaseResource {

  @JsonProperty
  def id = _id

  @JsonProperty
  def value = _value

  @DELETE
  def delete: Unit = {
    val cfg = profile.getConfigurations
    cfg.get(config_pid).remove(id)
    profile.setConfigurations(cfg)
  }

  @POST
  def update(key: String, value: String): Unit = {
    val cfg = profile.getConfigurations
    if (!cfg.get(config_pid).containsKey(key)) {
      throw new IllegalArgumentException("Key " + key + " is missing")
    }
    if (key != key) {
      // when key is changed let check if we won't override existing one
      if (cfg.get(config_pid).containsKey(key)) {
        throw new IllegalArgumentException("Key " + key + " already exists")
      }
      cfg.get(config_pid).remove(key)
    }
    cfg.get(config_pid).put(key, value)
    profile.setConfigurations(cfg)
  }

}
