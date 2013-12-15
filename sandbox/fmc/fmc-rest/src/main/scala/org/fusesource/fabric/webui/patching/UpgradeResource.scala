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

package io.fabric8.webui.patching

import javax.ws.rs._
import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.webui.Services

class ApplyUpgradesDTO {
  @JsonProperty
  var target_version: String = _
  @JsonProperty
  var upgrades: java.util.Map[String, String] = _

  override def toString = upgrades.toString
}

@Path("/upgrades")
class UpgradeResource extends BaseUpgradeResource {

  @GET
  override def get = patch_service.getPossibleUpgrades

  @POST
  def apply_upgrades(dto: ApplyUpgradesDTO): String = {
    val version = create_version(dto.target_version)
    Services.LOG.debug("Applying specified upgrades : {} to version : ", dto, version)
    patch_service.applyUpgrades(version, dto.upgrades)
    version.getId
  }

}
