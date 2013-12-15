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
import io.fabric8.api._
import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.webui.Services

class ApplyPatchesDTO {
  @JsonProperty
  var target_version: String = _
  @JsonProperty
  var patch_ids: List[String] = _
}

@Path("/patches")
class PatchResource extends BaseUpgradeResource {

  @GET
  override def get = patch_service.getPossiblePatches

  def find_patch_by_id(id: String): Patch = {
    val patches = patch_service.getPossiblePatches
    var i = patches.iterator()
    while (i.hasNext) {

      val patch = i.next

      if (id.equals(patch.getId())) {
        return patch;
      }
    }
    null
  }

  @POST
  def apply_patches(dto: ApplyPatchesDTO): String = {
    val version = create_version(dto.target_version)
    Services.LOG.debug("Applying specified patches : {} to version : ", dto, version)

    val patches = new java.util.HashSet[Patch]()

    dto.patch_ids.foreach((x) => {
      Option[Patch](find_patch_by_id(x)) match {
        case Some(patch) =>
          patches.add(patch)
        case None =>
      }
    })
    patch_service.applyPatches(version, patches)
    version.getId
  }

}
