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
package io.fabric8.webui.agents.camel

import collection.JavaConversions._
import org.codehaus.jackson.annotate.JsonProperty
import javax.ws.rs.{PathParam, GET, POST, Path}
import io.fabric8.camel.facade.mbean._
import io.fabric8.camel.facade.mbean.{CamelBrowsableEndpointMBean, CamelSuspendableRouteMBean, CamelContextMBean}
import io.fabric8.webui._
import io.fabric8.webui.{ByID, HasID, BaseResource}
import io.fabric8.camel.facade.CamelFacade

class CamelContextResource(val facade: CamelFacade, val mbean: CamelContextMBean)
  extends BaseResource with HasID {

  @JsonProperty
  def version = mbean.getCamelVersion

  @JsonProperty
  def id = mbean.getManagementName

  @JsonProperty
  def name = mbean.getCamelId

  @JsonProperty
  def uptime = mbean.getUptime

  @JsonProperty
  def state = mbean.getState

  @JsonProperty
  def properties = mbean.getProperties

  @JsonProperty
  def routesAsXML = mbean.dumpRoutesAsXml

  @JsonProperty
  def routeStatsAsXML = mbean.dumpRoutesStatsAsXml(true, true)

  @JsonProperty
  @Path("endpoints")
  def endpoints = {
    val rc = facade.getEndpoints(id).map(_ match {
      case mbean: CamelBrowsableEndpointMBean => new BrowsableEndpointResource(mbean)
      case mbean => new EndpointResource(mbean)
    }).toArray
    rc.sortWith(ByID(_, _))
  }

  @JsonProperty
  @Path("components")
  def components = {
    facade.getComponents(id).map(new ComponentResource(_)).toArray.sortWith(ByID(_, _))
  }

  @JsonProperty
  @GET
  @Path("routes")
  def routes = {
    facade.getRoutes(id).map(_ match {
      case mbean: CamelSuspendableRouteMBean => new SuspendableRouteResource(mbean)
      case mbean => new RouteResource(mbean)
    }).toArray.sortWith(ByID(_, _))
  }

  @Path("routes/{id}")
  def route(@PathParam("id") id: String) = {
    var rc: Option[RouteResource] = routes.find(_.id == id)
    rc.getOrElse {
      not_found
    }
  }

  @POST
  @Path("stop")
  def stop: Unit = mbean.stop

  @POST
  @Path("start")
  def start: Unit = mbean.start

  @POST
  @Path("suspend")
  def suspend() = mbean.suspend

  @POST
  @Path("resume")
  def resume() = mbean.resume
}
