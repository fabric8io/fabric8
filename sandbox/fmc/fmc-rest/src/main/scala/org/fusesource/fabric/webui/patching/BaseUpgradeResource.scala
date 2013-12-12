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

import io.fabric8.api.PatchService
import io.fabric8.api.Profile
import io.fabric8.api.Version
import io.fabric8.webui.{Services, BaseResource}

object BaseUpgradeResource {

  def last_version_id = {
    val versions = Services.fabric_service.getVersions.sortWith((a, b) => a.getSequence.compareTo(b.getSequence) < 0).iterator.toList
    versions.last.getName

  }

  def next_version_id = {
    val versions = Services.fabric_service.getVersions.sortWith((a, b) => a.getSequence.compareTo(b.getSequence) < 0).iterator.toList
    versions.last.getSequence.next.getName
  }

  def create_version(parent: String): Version = Services.fabric_service.createVersion(Services.fabric_service.getVersion(parent), next_version_id)

  def create_version: Version = Services.fabric_service.createVersion(next_version_id)

}


class BaseUpgradeResource extends BaseResource {

  def patch_service: PatchService = fabric_service.getPatchService

  def get_version(id: String): Version = Option[Version](fabric_service.getVersion(id)).getOrElse {
    not_found
  }

  def get_profile(id: String, version: Version) = Option[Profile](version.getProfile(id)).getOrElse {
    not_found
  }


  def create_version(parent: String): Version = BaseUpgradeResource.create_version(parent)

}
