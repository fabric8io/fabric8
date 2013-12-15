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

import io.fabric8.webui.{Services, BaseResource}
import javax.ws.rs.{PathParam, GET, Path}
import org.jclouds.providers.ProviderMetadata
import org.jclouds.apis.ApiMetadata
import Utils._

@Path("/compute_providers")
class ComputeProvidersResource extends BaseResource {

  @GET
  override def get: Array[ComputeProviderResource] = {
    val providers = Services.compute_providers.map(asResource(_)).toList
    val apis = Services.compute_apis.map(asResource(_)).toList

    val rc = providers ::: apis
    rc.toArray
  }

  @Path("{type}")
  def get(@PathParam("type") _type:String) = {
    _type match {
      case "provider" => Services.compute_providers.map(Utils.asResource(_)).toArray
      case "api" => Services.compute_apis.map(asResource(_)).toArray
      case _ => not_found
    }
  }

  @Path("{type}/{id}")
  def get(@PathParam("type") _type:String, @PathParam("id") id: String): ComputeProviderResource = {
    val provider = get.find(x => x.id.equals(id) && x._type.equals(_type) )
    provider getOrElse not_found
  }


}
