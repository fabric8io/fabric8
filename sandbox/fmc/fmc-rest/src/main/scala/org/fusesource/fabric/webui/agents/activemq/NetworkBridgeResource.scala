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
package io.fabric8.webui.agents.activemq

import org.codehaus.jackson.annotate.JsonProperty
import javax.ws.rs.{POST, Path}
import io.fabric8.webui.BaseResource
import io.fabric8.activemq.facade.NetworkBridgeViewFacade

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class NetworkBridgeResource(val self: NetworkBridgeViewFacade)
  extends BaseResource {

  @JsonProperty
  def id = self.getId

  @JsonProperty
  def dequeue_counter = self.getDequeueCounter

  @JsonProperty
  def enqueue_counter = self.getEnqueueCounter

  @JsonProperty
  def local_address = self.getLocalAddress

  @JsonProperty
  def local_broker_name = self.getLocalBrokerName

  @JsonProperty
  def remote_broker_name = self.getRemoteBrokerName

  @JsonProperty
  def remote_address = self.getRemoteAddress

  @POST
  @Path("stop")
  def stop: Unit = self.stop

  @POST
  @Path("start")
  def start: Unit = self.start

}
