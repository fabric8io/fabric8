/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.cluster

import org.apache.activemq.apollo.util._
import org.apache.activemq.apollo.broker._
import org.apache.activemq.apollo.broker.security.SecurityContext
import org.apache.activemq.apollo.util.path.Path
import org.fusesource.hawtdispatch._
import scala.collection.mutable.HashMap
import org.fusesource.fabric.apollo.cluster.util.HashRing
import org.apache.activemq.apollo.dto._

object ClusterRouter extends Log

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ClusterRouter(host: ClusterVirtualHost) extends LocalRouter(host) with Router {
  import ClusterRouter._

  def broker = host.broker

  var cluster_connector:ClusterConnector = _
  var hash_ring:HashRing[String, String] = _

  val cluster_queue_domain = new ClusterDomain(local_queue_domain)
  val cluster_topic_domain = new ClusterDomain(local_topic_domain)
  val cluster_dsub_domain = new ClusterDomain(local_dsub_domain)

  override def queue_domain = cluster_queue_domain
  override def topic_domain = cluster_topic_domain
  override def dsub_domain = cluster_dsub_domain

  def on_cluster_change(connector: ClusterConnector, new_ring:HashRing[String, String]) = {
    info("Cluster membership changed.")
    assert_executing

    cluster_connector = connector
    hash_ring = new_ring

    cluster_queue_domain.destination_by_id.values.foreach { dest =>
      dest.on_cluster_change
    }
  }

  class ClusterDomain[D <: DomainDestination](val actual:Domain[D]) extends Domain[ClusterDestination[D]] {

    val original_add_destination = actual.add_destination
    val original_remove_destination = actual.remove_destination

    actual.add_destination = (path:Path, dest:D) => {
      val clustered_dest = dest match {
        case queue:Queue=> new ClusterQueue(queue).asInstanceOf[ClusterDestination[D]]
        case _ => new ClusterDestination[D](dest)
      }
      this.add_destination(path, clustered_dest)
      original_add_destination(path, dest)
    }

    actual.remove_destination = (path:Path, dest:D) => {

      // remove the clustered dest..
      destination_by_id.get(dest.id).foreach(dest=> this.remove_destination(path, dest) )
      original_remove_destination(path, dest)

    }

    def clustered(d:D) = destination_by_id.get(d.id)

    def create_destination(address:DestinationAddress, security: SecurityContext) = {
      val rc = actual.create_destination(address, security)
      rc.map_success(clustered(_).get)
    }

    def destroy_destination(address: DestinationAddress, security: SecurityContext) = actual.destroy_destination(address, security)

    def can_create_destination(address: DestinationAddress, security: SecurityContext): Option[String] = actual.can_create_destination(address,security)

    def bind_action(consumer: DeliveryConsumer) = actual.bind_action(consumer)
  }

  class ClusterDestination[D <: DomainDestination](val local:D) extends DomainDestination {

    def address = local.address
    def virtual_host: VirtualHost = host

    val dispatch_queue = createQueue()

    var tail_destination:DomainDestination = local
    var tail_node:String = _

    var producers = HashMap[BindableDeliveryProducer, ConnectAddress]()
    var consumers = HashMap[DeliveryConsumer, BindAddress]()

    on_cluster_change

    def connect(connect_address: ConnectAddress, producer: BindableDeliveryProducer) = {
      assert_executing
      producers.put(producer, connect_address)
      tail_destination.connect(connect_address, producer)
    }
    def disconnect(producer: BindableDeliveryProducer) = {
      assert_executing
      producers.remove(producer)
      tail_destination.disconnect(producer)
    }

    def bind(bind_address: BindAddress, consumer: DeliveryConsumer) = {
      assert_executing

      consumers.put(consumer, bind_address)

      consumer match {
        case consumer:Peer#ClusterDeliveryConsumer =>
          local.bind(bind_address, consumer)
        case _ =>
          tail_destination.bind(bind_address, consumer)
      }

    }
    def unbind(consumer: DeliveryConsumer, persistent: Boolean) = {
      assert_executing
      consumers.remove(consumer)
      consumer match {
        case consumer:Peer#ClusterDeliveryConsumer =>
          local.unbind(consumer, persistent)
        case _ =>
          tail_destination.unbind(consumer, persistent)
      }
    }

    def pick_next_tail = {
      hash_ring.get(id)
    }

    def on_cluster_change {
      var next_tail_id = pick_next_tail
      if( next_tail_id!=tail_node ) {

        // Disconnect the clients from the old tail..
        consumers.keys.foreach(x=> tail_destination.unbind(x, false) )
        producers.keys.foreach(x=> tail_destination.disconnect(x) )

        // old tail clean up..
        tail_destination match {
          case x:PeerDestination => x.close()
          case _ =>
        }

        val old_tail_id = tail_node
        tail_node = next_tail_id

        tail_destination = if( is_tail ) {
          info("I am the tail node of: %s", id)
          local
        } else {
          info("Tail node moved from %s to %s for destination %s", old_tail_id, tail_node, id)
          new PeerDestination(local, cluster_connector.get_or_create_peer(tail_node))
        }

        // reconnect the clients to the new tail
        consumers.foreach(x=> tail_destination.bind(x._2, x._1) )
        producers.foreach(x=> tail_destination.connect(x._2, x._1) )

      }
    }

    def is_tail = cluster_connector.node_id == tail_node

    def update(on_completed: Runnable) = local.update(on_completed)

    def resource_kind = local.resource_kind
  }

  class ClusterQueue(local:Queue) extends ClusterDestination[Queue](local) {

    local.config

    val zk_path = cluster_connector.config.zk_directory + "/queues/" + local.id


  }

}
