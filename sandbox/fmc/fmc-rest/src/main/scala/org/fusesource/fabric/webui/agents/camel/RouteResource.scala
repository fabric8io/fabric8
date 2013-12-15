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

import org.codehaus.jackson.annotate.JsonProperty
import javax.ws.rs.{POST, Path}
import io.fabric8.webui.{BaseResource, HasID}
import io.fabric8.webui.{HasID, BaseResource}
import io.fabric8.camel.facade.mbean.CamelRouteMBean

class RouteResource(val mbean: CamelRouteMBean) extends BaseResource with HasID {

  @JsonProperty
  def kind = "simple"

  @JsonProperty
  def id = mbean.getId

  @JsonProperty
  def description = mbean.getDescription

  @JsonProperty
  def state = mbean.getState

  @JsonProperty
  def tracing = mbean.getTracing

  @JsonProperty
  def endpoint_uri = mbean.getEndpointUri

  @JsonProperty
  def exchanges_completed = mbean.getExchangesCompleted

  @JsonProperty
  def exchanges_failed = mbean.getExchangesFailed

  @JsonProperty
  def exchanges_total = mbean.getExchangesTotal

  @POST
  @Path("start")
  def start: Unit = mbean.start

  @POST
  @Path("stop")
  def stop: Unit = mbean.stop

}
