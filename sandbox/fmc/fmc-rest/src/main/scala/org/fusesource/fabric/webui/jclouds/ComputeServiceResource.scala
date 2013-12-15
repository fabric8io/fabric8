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

import org.jclouds.compute.ComputeService
import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.webui._
import scala.Some
import scala.Some
import io.fabric8.webui.{PaginatedArrayDTO, Services, BaseResource}
import org.jclouds.domain.Location
import collection.mutable.{ListBuffer, HashMap}
import javax.ws.rs._
import org.jclouds.compute.domain.ComputeMetadata
import io.fabric8.zookeeper.ZkPath
import io.fabric8.api.Container
import scala.Some
import org.jclouds.Context
import io.fabric8.zookeeper.utils.ZooKeeperUtils

/**
 *
 */
class RegionResource(self: Location) extends BaseResource {

  @JsonProperty
  def id = self.getId

}

class ZoneResource(self: Location) extends BaseResource {
  @JsonProperty
  def id = self.getId

  @JsonProperty
  def parent = self.getParent.getId
}

class NodeActionDTO {

  @JsonProperty
  var id: String = _

  @JsonProperty
  var action: String = _

}

class ComputeServiceResource(self: ComputeService) extends BaseResource {

  def context:Context = self.getContext().unwrap()

  @JsonProperty
  def id = context.getProviderMetadata.getId + " - " + context.getName

  @JsonProperty
  def name = context.getProviderMetadata.getName + " - " + context.getName

  @JsonProperty
  def endpoint = context.getProviderMetadata.getEndpoint

  @JsonProperty
  def regions = regions_resource.toArray

  @Path("regions")
  def regions_resource = self.listAssignableLocations.filter(_.getScope.name == "REGION").map(new RegionResource(_))

  @JsonProperty
  def zones = zones_resource.toArray

  @Path("zones")
  def zones_resource = self.listAssignableLocations.filter(_.getScope.name == "ZONE").map(new ZoneResource(_))

  def images = self.listImages.map(new ImageResource(_)).toArray

  @GET
  @Path("nodes")
  def nodes = self.listNodes

  def get_nodes(
                 @DefaultValue("1") @QueryParam("page") page: Int,
                 @DefaultValue("20") @QueryParam("page_size") page_size: Int
                 ) = {
    var start = (page - 1) * page_size
    var end = start + page_size

    val models = nodes.toArray

    val rc = new PaginatedArrayDTO

    if (start > models.length) {
      start = models.length
    }

    if (end > models.length) {
      end = models.length
    }

    rc.page = page
    rc.per_page = page_size
    rc.total = models.length
    rc.models = models.slice(start, end)

    rc
  }

  @GET
  @Path("nodes/{id}")
  def node(@PathParam("id") id: String) = nodes.find(_.getId == id) getOrElse not_found

  @POST
  @Path("nodes/{id}")
  def node_action(@PathParam("id") id: String, args: NodeActionDTO) = {
    args.action match {
      case "stop" =>
        self.suspendNode(args.id)
      case "start" =>
        self.resumeNode(args.id)
      case "destroy" =>
        self.destroyNode(args.id)
      case _ =>
        not_found
    }
  }

  @GET
  @Path("images")
  def get_images(
                  @DefaultValue("1") @QueryParam("page") page: Int,
                  @DefaultValue("20") @QueryParam("page_size") page_size: Int,
                  @DefaultValue("all") @QueryParam("region") region: String,
                  @DefaultValue("all") @QueryParam("os_family") os_family: String
                  ) = {

    var start = (page - 1) * page_size
    var end = start + page_size

    var models = if (region == "all") {
      images.toArray
    } else {
      images.filter(_.location_id == region).toArray
    }

    models = if (os_family == "all") {
      models
    } else {
      models.filter(_.os_family == os_family)
    }

    val rc = new PaginatedArrayDTO

    if (start > models.length) {
      start = models.length
    }

    if (end > models.length) {
      end = models.length
    }

    rc.page = page
    rc.per_page = page_size
    rc.total = models.length
    rc.models = models.slice(start, end)

    rc
  }

  @GET
  @Path("images/{id}")
  def image(@PathParam("id") id: String) = images.find(_.id == id) getOrElse not_found

  @DELETE
  def delete() {

    val id = context.getName
    val provider_id = context.getProviderMetadata.getId

    try {
      ZooKeeperUtils.deleteSafe(Services.curator, ZkPath.CLOUD_SERVICE.getPath(id))
    } catch {
      case _ => // Ignore
    }

    Services.configs_by_factory_pid("org.jclouds.compute").foreach((x) => {
      val props = x.getProperties
      val name = props.get("name")
      Option(name) match {
        case Some(name) =>
          if (name.equals(id)) {
            Option(props.get("api")) match {
              case Some(api) =>
                if (api.equals(provider_id)) {
                  x.delete()
                }
              case None =>
            }
            Option(props.get("provider")) match {
              case Some(provider) =>
                if (provider.equals(provider_id)) {
                  x.delete()
                }
              case None =>
            }


          }
        case None =>
      }
    })
  }

}
