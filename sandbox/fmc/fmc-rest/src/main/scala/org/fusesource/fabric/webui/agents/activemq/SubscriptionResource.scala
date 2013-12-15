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
import io.fabric8.activemq.facade.SubscriptionViewFacade
import io.fabric8.webui.BaseResource

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class SubscriptionResource(val self: SubscriptionViewFacade)
  extends BaseResource {

  @JsonProperty
  def id = self.getId

  //  def setSelector(p1: String) {}
  @JsonProperty
  def slow_consumer = self.isSlowConsumer

  @JsonProperty
  def retroactive = self.isRetroactive

  @JsonProperty
  def no_local = self.isNoLocal

  @JsonProperty
  def exclusive = self.isExclusive

  @JsonProperty
  def durable = self.isDurable

  @JsonProperty
  def active = self.isDurable

  @JsonProperty
  def subscription_name = self.getSubcriptionName

  @JsonProperty
  def subscription_id = self.getSubcriptionId

  @JsonProperty
  def selector = self.getSelector

  @JsonProperty
  def priority = self.getPriority

  @JsonProperty
  def prefetch_size = self.getPrefetchSize

  @JsonProperty
  def pending_queue_size = self.getPendingQueueSize

  @JsonProperty
  def message_count_awaiting_acknowledge = self.getMessageCountAwaitingAcknowledge

  @JsonProperty
  def maximum_pending_message_limit = self.getMaximumPendingMessageLimit

  @JsonProperty
  def dispatched_queue_size = self.getDispatchedQueueSize

  @JsonProperty
  def dispatched_counter = self.getDispatchedCounter

  @JsonProperty
  def destination_name = self.getDestinationName

  @JsonProperty
  def dequeue_counter = self.getDequeueCounter

  @JsonProperty
  def connection_id = self.getConnectionId

  @JsonProperty
  def sessionId = self.getSessionId

  @JsonProperty
  def client_id = self.getClientId

}
