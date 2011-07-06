/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.cluster

import org.apache.activemq.apollo.util._
import org.fusesource.fabric.apollo.cluster.dto.ClusterRouterDTO
import org.apache.activemq.apollo.broker._
import org.apache.activemq.apollo.broker.security.SecurityContext
import org.apache.activemq.apollo.dto._
import org.apache.activemq.apollo.util.path.Path
import org.fusesource.hawtdispatch._
import scala.collection.mutable.HashMap
import org.fusesource.fabric.apollo.cluster.util.HashRing

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ClusterRouterFactory extends RouterFactory.Provider {

  def create(host: VirtualHost): Router = host.config.router match {
    case config:ClusterRouterDTO=>
      new ClusterRouter(host)
    case _ => null
  }
}

object ClusterRouter extends Log

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ClusterRouter(host: VirtualHost) extends LocalRouter(host) with Router {
  import ClusterRouter._

  val broker = host.broker.asInstanceOf[ClusterBroker]
  def cluster = broker.cluster
  var hash_ring:HashRing[String, String] = broker.hash_ring

  val cluster_queue_domain = new ClusterDomain(queue_domain)
  val cluster_topic_domain = new ClusterDomain(topic_domain)

  def on_cluster_change(new_ring:HashRing[String, String]) = {
    info("Cluster membership changed.")
    assert_executing
    hash_ring = new_ring

    cluster_queue_domain.destination_by_id.values.foreach { dest =>
      dest.on_cluster_change
    }
  }

  override def domain(destination: DestinationDTO):Domain[_ <: DomainDestination] = destination match {
    case x:TopicDestinationDTO => cluster_topic_domain
    case x:QueueDestinationDTO => cluster_queue_domain
    case _ => throw new RuntimeException("Unknown domain type: "+destination.getClass)
  }

  class ClusterDomain[D <: DomainDestination](val actual:Domain[D]) extends Domain[ClusterDestination[D]] {

    val original_add_destination = actual.add_destination
    val original_remove_destination = actual.remove_destination

    actual.add_destination = (path:Path, dest:D) => {
      this.add_destination(path, new ClusterDestination(dest))
      original_add_destination(path, dest)
    }

    actual.remove_destination = (path:Path, dest:D) => {

      // remove the clustered dest..
      destination_by_id.get(dest.id).foreach(dest=> this.remove_destination(path, dest) )
      original_remove_destination(path, dest)

    }

    def clustered(d:D) = destination_by_id.get(d.id)

    def create_destination(path: Path, destination:DestinationDTO, security: SecurityContext) = {
      val rc = actual.create_destination(path, destination, security)
      rc.map_success(clustered(_).get)
    }

    def can_connect_one(path: Path, destination:DestinationDTO, producer: BindableDeliveryProducer, security: SecurityContext): Boolean = actual.can_connect_one(path, destination, producer, security)
    def can_bind_one(path: Path, destination:DestinationDTO, consumer: DeliveryConsumer, security: SecurityContext): Boolean = actual.can_bind_one(path, destination, consumer, security)

    def destroy_destination(path: Path, destination: DestinationDTO) = actual.destroy_destination(path, destination)

    def can_destroy_destination(path: Path, destination: DestinationDTO, security: SecurityContext): Option[String] = actual.can_destroy_destination(path, destination,security)

    def can_create_destination(path: Path, destination: DestinationDTO, security: SecurityContext): Option[String] = actual.can_create_destination(path, destination,security)
  }

  class ClusterDestination[D <: DomainDestination](val local:D) extends DomainDestination {

    def destination_dto:DestinationDTO = local.destination_dto
    def virtual_host: VirtualHost = host

    val dispatch_queue = createQueue()

    var master:DomainDestination = local

    def id = local.id

    var producers = HashMap[BindableDeliveryProducer, DestinationDTO]()
    var consumers = HashMap[DeliveryConsumer, DestinationDTO]()

    // Pick the right master asap..
    var master_id:String = _
    on_cluster_change


    def connect(destination: DestinationDTO, producer: BindableDeliveryProducer) = {
      assert_executing
      producers.put(producer, destination)
      master.connect(destination, producer)
    }
    def disconnect(producer: BindableDeliveryProducer) = {
      assert_executing
      producers.remove(producer)
      master.disconnect(producer)
    }

    def bind(destination: DestinationDTO, consumer: DeliveryConsumer) = {
      assert_executing

      consumers.put(consumer, destination)

      consumer match {
        case consumer:Peer#ClusterDeliveryConsumer =>
          local.bind(destination, consumer)
        case _ =>
          master.bind(destination, consumer)
      }

    }
    def unbind(consumer: DeliveryConsumer, persistent: Boolean) = {
      assert_executing
      consumers.remove(consumer)
      consumer match {
        case consumer:Peer#ClusterDeliveryConsumer =>
          local.unbind(consumer, persistent)
        case _ =>
          master.unbind(consumer, persistent)
      }
    }

    def on_cluster_change {
      var next_master_id = hash_ring.get(id)
      if( next_master_id!=master_id ) {

        // Disconnect the clients from the old master..
        consumers.keys.foreach(x=> master.unbind(x, false) )
        producers.keys.foreach(x=> master.disconnect(x) )

        // old master clean up..
        master match {
          case x:PeerDestination => x.close()
          case _ =>
        }

        val old_master_id = master_id
        master_id = next_master_id
        master = if( is_master ) {
          info("I am the master of: %s", id)
          local
        } else {
          info("Master moved from %s to %s for destination %s", old_master_id, master_id, id)
          new PeerDestination(local, broker.get_or_create_peer(master_id))
        }

        // If we are not the master, then any messages that make it into
        // the local queue need to be forwarded to the actual master.
        if ( ! is_master ) {



        }

        // reconnect the clients to the new master
        consumers.foreach(x=> master.bind(x._2, x._1) )
        producers.foreach(x=> master.connect(x._2, x._1) )

      }
    }

    def is_master = master_id == broker.id

    def update(on_completed: Runnable) = local.update(on_completed)
  }


}
