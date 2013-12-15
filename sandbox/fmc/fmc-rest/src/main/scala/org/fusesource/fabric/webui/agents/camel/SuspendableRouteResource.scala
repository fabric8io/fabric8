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
import io.fabric8.camel.facade.mbean.CamelSuspendableRouteMBean

/**
 * Suspendable route resource.
 *
 * @author ldywicki
 */
class SuspendableRouteResource(mbean: CamelSuspendableRouteMBean) extends RouteResource(mbean) {

  @JsonProperty
  override def kind = "suspendable"

  @POST
  @Path("suspend")
  def suspend() = mbean.suspend

  @POST
  @Path("resume")
  def resume() = mbean.resume
}
