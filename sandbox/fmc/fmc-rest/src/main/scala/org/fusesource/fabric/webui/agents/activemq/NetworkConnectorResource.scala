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
import io.fabric8.activemq.facade.NetworkConnectorViewFacade
import io.fabric8.webui.BaseResource

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class NetworkConnectorResource(val self: NetworkConnectorViewFacade)
  extends BaseResource {

  @JsonProperty
  def id = self.getId

  @JsonProperty
  def name = self.getName


  // This property does not exist in ActiveMQ 5.9
  // @JsonProperty
  // def network_ttl = self.getNetworkTTL

  @JsonProperty
  def prefetch_size = self.getPrefetchSize

  @JsonProperty
  def bridge_temp_destinations = self.isBridgeTempDestinations

  @JsonProperty
  def conduit_subscriptions = self.isConduitSubscriptions

  @JsonProperty
  def decrease_network_consumer_priority = self.isDecreaseNetworkConsumerPriority

  @JsonProperty
  def dispatch_async = self.isDispatchAsync

  @JsonProperty
  def duplex = self.isDuplex

  @JsonProperty
  def dynamic_only = self.isDynamicOnly

  @JsonProperty
  def user_name = self.getUserName

  @POST
  @Path("stop")
  def stop: Unit = self.stop()

  @POST
  @Path("start")
  def start: Unit = self.start

}
