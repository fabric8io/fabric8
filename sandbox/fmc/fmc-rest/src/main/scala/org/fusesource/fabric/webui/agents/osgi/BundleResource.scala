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
package io.fabric8.webui.agents.osgi

import io.fabric8.api.data.BundleInfo
import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.api.data.BundleInfo.Header
import io.fabric8.webui.BaseResource

class HeaderResource(property: Header) {
  @JsonProperty
  def key = property.getKey

  @JsonProperty
  def value = property.getValue
}

class BundleResource(val bundle: BundleInfo) extends BaseResource {

  @JsonProperty
  def id = bundle.getId.longValue

  @JsonProperty
  def state = bundle.getState.name

  @JsonProperty
  def symbolic_name = bundle.getSymbolicName

  @JsonProperty
  def version = bundle.getVersion

  @JsonProperty
  def import_packages = bundle.getImportPackages

  @JsonProperty
  def export_packages = bundle.getExportPackages

  @JsonProperty
  def headers = bundle.getHeaders.map(new HeaderResource(_))

}
