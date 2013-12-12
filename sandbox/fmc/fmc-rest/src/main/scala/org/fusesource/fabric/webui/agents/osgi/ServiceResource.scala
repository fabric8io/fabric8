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

import io.fabric8.api.data.ServiceInfo
import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.api.data.ServiceInfo.Property
import io.fabric8.webui.BaseResource

class PropertyResource(property: Property) {
  @JsonProperty
  def key = property.getKey

  @JsonProperty
  def value = property.getValue
}

class ServiceResource(val service: ServiceInfo) extends BaseResource {

  @JsonProperty
  def id = service.getId.longValue

  @JsonProperty
  def bundle_id = service.getBundleId.longValue

  @JsonProperty
  def object_classes = service.getObjectClasses

  @JsonProperty
  def using_bundles = service.getUsingBundlesId map (_.longValue)

  @JsonProperty
  def properties = service.getProperties.map(new PropertyResource(_))

}
