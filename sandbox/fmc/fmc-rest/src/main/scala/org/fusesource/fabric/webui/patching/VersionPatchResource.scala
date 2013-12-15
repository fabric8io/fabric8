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

/**
 *
 */

@Path("/patches/versions")
class VersionPatchResource extends BaseUpgradeResource {

  @GET
  override def get = not_found

  @GET
  @Path("{id}")
  def possible_patches(@PathParam("id") id: String) = {
    val version = get_version(id)
    patch_service.getPossiblePatches(version)
  }

}
