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

package org.fusesource.fabric.webui.jclouds

import org.fusesource.fabric.webui.{Services, BaseResource}
import org.fusesource.fabric.webui.{Services, BaseResource}
import org.codehaus.jackson.annotate.JsonProperty
import javax.ws.rs.{POST, PathParam, Path, GET}
import org.fusesource.fabric.service.jclouds.internal.CloudUtils

/**
 *
 */

class CreateComputeServiceDTO {

  @JsonProperty
  var serviceId: String = _

  @JsonProperty
  var provider: String = _

  @JsonProperty
  var api: String = _

  @JsonProperty
  var endpoint: String = _

  @JsonProperty
  var identity: String = _

  @JsonProperty
  var credential: String = _

  @JsonProperty
  var options: String = _

  override def toString = "provider : " + provider + " identity: " + identity + " credential : " + credential + " options : " + options

}

@Path("/compute_services")
class ComputeServicesResource extends BaseResource {

  @GET
  override def get: Array[ComputeServiceResource] = Services.compute_services.map(new ComputeServiceResource(_)).toArray

  @Path("{id}")
  def get(@PathParam("id") id: String): ComputeServiceResource = {
    val service = get.find(_.id == id)
    service getOrElse not_found
  }

  @POST
  def create(args: CreateComputeServiceDTO): Unit = {
    val options = args.options.split('\n').map(_.trim)
    val props = CloudUtils.parseProviderOptions(options)

    if (!Option(args.serviceId).isDefined && Option(args.provider).isDefined) {
      args.serviceId = args.provider
    } else if (!Option(args.serviceId).isDefined && Option(args.provider).isDefined) {
      args.serviceId = args.api
    }

    if (Option(args.provider).isDefined) {
      CloudUtils.registerProvider(Services.zoo_keeper, Services.config_admin, args.serviceId, args.provider, args.identity, args.credential, props)
    } else if (Option(args.api).isDefined && Option(args.endpoint).isDefined) {
      CloudUtils.registerApi(Services.zoo_keeper, Services.config_admin, args.serviceId, args.api, args.endpoint, args.identity, args.credential, props)
    }
    //System.out.printf("Registering new provider with %s\n", args)

    //System.out.printf("Waiting...\n")
    CloudUtils.waitForComputeService(Services.bundle_context, args.serviceId)
    //System.out.printf("Done!\n")
  }


}
