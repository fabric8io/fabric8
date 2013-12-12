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
import io.fabric8.webui.{Services, BaseResource}
import org.codehaus.jackson.annotate.JsonProperty
import javax.ws.rs.{POST, PathParam, Path, GET}
import io.fabric8.service.jclouds.internal.CloudUtils._
import java.util.UUID

/**
 *
 */

class CreateComputeServiceDTO {

  @JsonProperty
  var id: String = _

  @JsonProperty
  var serviceId: String = _

  @JsonProperty
  var provider: String = _

  @JsonProperty
  var _type: String = _

  @JsonProperty
  var endpoint: String = _

  @JsonProperty
  var identity: String = _

  @JsonProperty
  var credential: String = _

  @JsonProperty
  var options: String = _

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

    require(args.identity != null && !args.identity.equals(""), "Must specify cloud provider/api identity")
    require(args.credential != null && !args.credential.equals(""), "Must specify cloud provider/api credential")

    val options = args.options.split('\n').map(_.trim)
    val props = parseProviderOptions(options)

    def generate_service_id (provider:String) = provider + "-" + UUID.randomUUID().toString

    val service_id = Option(args.serviceId) match {
      case Some(id) =>
        if (id.equals(""))
          generate_service_id(args.provider)
        else
          id
      case None =>
          generate_service_id(args.provider)
    }

    args._type match {
      case "provider" =>
        registerProvider(Services.curator,
                         Services.config_admin,
                         service_id,
                         args.provider,
                         args.identity,
                         args.credential,
                         props)
      case "api" =>
        require(args.endpoint != null && !args.endpoint.equals(""), "Must specify endpoint URI when registering a cloud API")
        registerApi(Services.curator,
                    Services.config_admin,
                    service_id,
                    args.provider,
                    args.endpoint,
                    args.identity,
                    args.credential,
                    props)

      case _ =>
        throw new RuntimeException("Unknown cloud api/provider type")
    }

  }


}
