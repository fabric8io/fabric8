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
import javax.ws.rs.{PathParam, GET, POST, Path}
import io.fabric8.webui.{ByID, BaseResource}
import io.fabric8.webui.{ByID, BaseResource}
import io.fabric8.activemq.facade.BrokerFacade

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class BrokerResource(val self: BrokerFacade)
  extends BaseResource {

  def admin = self.getBrokerAdmin

  @JsonProperty
  def id = self.getId

  @JsonProperty
  def name = admin.getBrokerName

  @JsonProperty
  def version = admin.getBrokerVersion

  @JsonProperty
  def data_directory = admin.getDataDirectory

  @JsonProperty
  def vm_url = admin.getVMURL

  @JsonProperty
  def openwire_url = admin.getOpenWireURL

  @JsonProperty
  def ssl_url = admin.getSslURL

  @JsonProperty
  def stomp_url = admin.getStompURL

  @JsonProperty
  def stomp_ssl_url = admin.getStompSslURL

  @JsonProperty
  def memory_limit = admin.getMemoryLimit

  @JsonProperty
  def memory_percent_usage = admin.getMemoryPercentUsage

  @JsonProperty
  def store_limit = admin.getStoreLimit

  @JsonProperty
  def store_percent_usage = admin.getStorePercentUsage

  @JsonProperty
  def temp_limit = admin.getTempLimit

  @JsonProperty
  def temp_percent_usage = admin.getTempPercentUsage

  @JsonProperty
  def total_producer_count = admin.getTotalProducerCount

  @JsonProperty
  def total_consumer_count = admin.getTotalConsumerCount

  @JsonProperty
  def total_dequeue_count = admin.getTotalDequeueCount

  @JsonProperty
  def total_enqueue_count = admin.getTotalEnqueueCount

  @JsonProperty
  def total_message_count = admin.getTotalMessageCount

  @JsonProperty
  @GET
  @Path("connections")
  def connections = iter(self.getConnections).map(new ConnectionResource(_)).toArray

  @GET
  @Path("connections/{id}")
  def connection(@PathParam("id") id: String) = connections.find(_.id == id) getOrElse {
    not_found
  }


  @JsonProperty
  @GET
  @Path("connectors")
  def connectors = iter(self.getConnectors).map(x => new ConnectorResource(self.getConnector(x))).toArray

  @GET
  @Path("connectors/{id}")
  def connector(@PathParam("id") id: String) = connectors.find(_.id == id) getOrElse {
    not_found
  }

  @JsonProperty
  @GET
  @Path("network_connectors")
  def network_connectors = iter(self.getNetworkConnectors).map(new NetworkConnectorResource(_)).toArray

  @GET
  @Path("network_connectors/{id}")
  def network_connector(@PathParam("id") id: String) = network_connectors.find(_.id == id) getOrElse {
    not_found
  }

  @JsonProperty
  @GET
  @Path("network_bridges")
  def network_bridges = iter(self.getNetworkBridges).map(new NetworkBridgeResource(_)).toArray

  @GET
  @Path("network_bridges/{id}")
  def network_bridge(@PathParam("id") id: String) = network_bridges.find(_.id == id) getOrElse {
    not_found
  }

  @JsonProperty
  @GET
  @Path("topics")
  def topics = iter(self.getTopics).map(new TopicResource(_)).toArray.sortWith(ByID(_, _))

  @GET
  @Path("topics/{id}")
  def topic(@PathParam("id") id: String) = topics.find(_.id == id) getOrElse {
    not_found
  }

  @JsonProperty
  @GET
  @Path("queues")
  def queues = iter(self.getQueues).map(new QueueResource(_)).toArray.sortWith(ByID(_, _))

  @GET
  @Path("queues/{id}")
  def queue(@PathParam("id") id: String) = queues.find(_.id == id) getOrElse {
    not_found
  }

  //  @JsonProperty
  //  def scheduled_jobs = iter(self.getScheduledJobs).map(new JobResource(_)).toArray

  @JsonProperty
  @GET
  @Path("durable_topic_subscribers")
  def durable_topic_subscribers = iter(self.getDurableTopicSubscribers).map(new DurableSubscriptionResource(_)).toArray

  @GET
  @Path("durable_topic_subscribers/{id}")
  def durable_topic_subscriber(@PathParam("id") id: String) = durable_topic_subscribers.find(_.id == id) getOrElse {
    not_found
  }

  @JsonProperty
  @GET
  @Path("inactive_durable_topic_subscribers")
  def inactive_durable_topic_subscribers = iter(self.getInactiveDurableTopicSubscribers).map(new DurableSubscriptionResource(_)).toArray

  @GET
  @Path("inactive_durable_topic_subscribers/{id}")
  def inactive_durable_topic_subscriber(@PathParam("id") id: String) = inactive_durable_topic_subscribers.find(_.id == id) getOrElse {
    not_found
  }

  @POST
  @Path("stop")
  def stop: Unit = admin.stop()

  @POST
  @Path("start")
  def start: Unit = admin.start

}
