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

import org.apache.activemq.broker.jmx._
import org.codehaus.jackson.annotate.JsonProperty
import io.fabric8.webui._
import io.fabric8.webui.{HasID, BaseResource}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
abstract class DestinationResource(val self: DestinationViewMBean)
  extends BaseResource with HasID {

  //  def setUseCache(p1: Boolean) {}
  //  def setProducerFlowControl(p1: Boolean) {}
  //  def setMemoryUsagePortion(p1: Float) {}
  //  def setMemoryLimit(p1: Long) {}
  //  def setMaxProducersToAudit(p1: Int) {}
  //  def setMaxPageSize(p1: Int) {}
  //  def setMaxAuditDepth(p1: Int) {}
  //  def setBlockedProducerWarningInterval(p1: Long) {}
  //  def sendTextMessage(p1: String, p2: String, p3: String) = ""
  //  def sendTextMessage(p1: String) = ""
  //  def sendTextMessage(p1: Map[_, _], p2: String, p3: String, p4: String) = ""
  //  def sendTextMessage(p1: Map[_, _], p2: String) = ""
  //  def resetStatistics() {}
  //  def browseMessages(p1: String) = null
  //  def browseAsTable(p1: String) = null
  //  def browse(p1: String) = null

  //  def browseMessages = self.browseMessages()
  //  def browseAsTable = self.browseAsTable()
  //  def browse = self.browse()
  @JsonProperty
  def id: String

  @JsonProperty
  def name = self.getName

  @JsonProperty
  def use_cache = self.isUseCache

  @JsonProperty
  def producer_flow_control = self.isProducerFlowControl

  @JsonProperty
  def prioritized_messages = self.isPrioritizedMessages

  @JsonProperty
  def subscriptions = self.getSubscriptions

  @JsonProperty
  def slow_consumer_strategy = self.getSlowConsumerStrategy

  @JsonProperty
  def queue_size = self.getQueueSize

  @JsonProperty
  def producer_count = self.getProducerCount

  @JsonProperty
  def min_enqueue_time = self.getMinEnqueueTime

  @JsonProperty
  def memory_usage_portion = self.getMemoryUsagePortion

  @JsonProperty
  def memory_percent_usage = self.getMemoryPercentUsage

  @JsonProperty
  def memory_limit = self.getMemoryLimit

  @JsonProperty
  def max_producers_to_audit = self.getMaxProducersToAudit

  @JsonProperty
  def max_page_size = self.getMaxPageSize

  @JsonProperty
  def max_enqueue_time = self.getMaxEnqueueTime

  @JsonProperty
  def max_audit_depth = self.getMaxAuditDepth

  @JsonProperty
  def in_flight_count = self.getInFlightCount

  @JsonProperty
  def expired_count = self.getExpiredCount

  @JsonProperty
  def enqueue_count = self.getEnqueueCount

  @JsonProperty
  def dispatch_count = self.getDispatchCount

  @JsonProperty
  def dequeue_count = self.getDequeueCount

  @JsonProperty
  def consumer_count = self.getConsumerCount

  @JsonProperty
  def blocked_producer_warning_interval = self.getBlockedProducerWarningInterval

  @JsonProperty
  def average_enqueue_time = self.getAverageEnqueueTime

}
