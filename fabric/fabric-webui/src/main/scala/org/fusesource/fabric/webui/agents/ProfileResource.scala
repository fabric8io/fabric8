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

import org.fusesource.fabric.api.{Profile, Container}
import javax.ws.rs._
import org.codehaus.jackson.annotate.JsonProperty
import org.fusesource.fabric.webui.{BaseResource, HasID}
import org.fusesource.fabric.webui.{HasID, BaseResource}

/**
 * Resource which represents agent profiles.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @author ldywicki
 */
class ProfileResource(val profile: Profile, val agent: Container)
  extends BaseResource with HasID {

  @JsonProperty
  def id = profile.getId

  @JsonProperty
  def version = profile.getVersion

  @DELETE
  def delete: Unit = {
    val profiles = agent.getProfiles
    agent.setProfiles(profiles.filterNot(_.getId == profile.getId).toArray)
  }

}

