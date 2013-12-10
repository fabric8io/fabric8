/*
 * Copyright 2012 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.fusesource.fabric.webui.jclouds

import org.jclouds.providers.ProviderMetadata
import org.jclouds.apis.ApiMetadata
import org.fusesource.fabric.webui.Services
import org.fusesource.fabric.webui.jclouds.Utils._

/**
 * @author Stan Lewis
 */
object Utils {

  def asResource(x: ProviderMetadata) : ComputeProviderResource =  new ComputeProviderResource(x.getId, "provider")

  def asResource(x: ApiMetadata) : ComputeProviderResource =  new ComputeProviderResource(x.getId, "api")

}
