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

package io.fabric8.webui.jclouds

import org.jclouds.compute.domain.Image
import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.webui.BaseResource

/**
 *
 */


class ImageResource(self: Image) extends BaseResource {

  @JsonProperty
  def id = self.getId

  @JsonProperty
  def location_id = self.getLocation.getId

  @JsonProperty
  def os_name = self.getOperatingSystem.getName

  @JsonProperty
  def os_version = self.getOperatingSystem.getVersion

  @JsonProperty
  def os_family = self.getOperatingSystem.getFamily

  @JsonProperty
  def os_arch = self.getOperatingSystem.getArch

  @JsonProperty
  def os_description = self.getOperatingSystem.getDescription

  @JsonProperty
  def version = self.getVersion

  @JsonProperty
  def description = self.getDescription


}
