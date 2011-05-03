/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.cluster

import org.fusesource.hawtdispatch._
import java.lang.String
import org.fusesource.fusemq.cluster.dto.ClusterBrokerDTO
import org.apache.activemq.apollo.broker._
import java.io.IOException
import org.apache.activemq.apollo.transport.{DefaultTransportListener, TransportFactory}
import org.apache.activemq.apollo.util._
import org.fusesource.fusemq.cluster.protocol.ClusterProtocol
import org.fusesource.hawtbuf.Buffer
import org.fusesource.fusemq.cluster.model._
import collection.mutable.HashMap
import org.apache.activemq.apollo.broker.protocol.{ProtocolHandler, AnyProtocol}
import org.fusesource.fusemq.cluster.util.{HashRing, Hasher}

object ClusterBroker extends Log {
  implicit def encode_peer_info(value:PeerInfo.Buffer):Buffer = value.toFramedBuffer
  implicit def decode_peer_info(value:Buffer):PeerInfo.Buffer = PeerInfo.FACTORY.parseFramed(value)
}

/**
 * A cluster broker is a broker which cooperates with peers in a cluster
 * to increase scalability and availability.
 *
 * This class in responsible for tracking the peers of the cluster.
 */
class ClusterBroker(override val id:String, val cluster:ZkCluster) extends Broker {
  import ClusterBroker._

  var hash_ring:HashRing[String, String] = _

  /**
   * This is a connector that handles establishing outbound connections.
   */
  object cluster_connector extends Connector {

    val connections = new HashMap[Long, BrokerConnection]
    val connection_counter = new LongCounter()

    def broker = ClusterBroker.this
    def id = "cluster"
    def config = null
    def dispatch_queue = ClusterBroker.this.dispatch_queue

    protected def _start(on_completed: Runnable) = {
      on_completed.run
    }

    def stopped(connection: BrokerConnection) = dispatch_queue {
      connections.remove(connection.id)
    }

    protected def _stop(on_completed: Runnable) = dispatch_queue {
      on_completed.run
    }

    def connect(location:String)(on_complete: Result[BrokerConnection, IOException]=>Unit) = {
      try {

        val outbound_connection = new BrokerConnection(cluster_connector, connection_id_counter.incrementAndGet)
        outbound_connection.protocol_handler = new ProtocolHandler() {
          def protocol: String = "outbound"

          override def on_transport_connected = {
            on_complete(Success(outbound_connection))
          }

          override def on_transport_failure(error: IOException) = {
            broker.dispatch_queue {
              connections.remove(outbound_connection.id)
              outbound_connection.stop
            }
            on_complete(Failure(error))
          }
        }
        init_dispatch_queue(outbound_connection.dispatch_queue)

        outbound_connection.transport = TransportFactory.connect(location)
        connections.put(outbound_connection.id, outbound_connection)
        outbound_connection.start
      } catch {
        case error:IOException => on_complete(Failure(error))
      }
    }
  }

  var cluster_weight = 16

  override def _start(on_completed: Runnable) = {
    config match {
      case c: ClusterBrokerDTO =>
        if (c.cluster_weight != null) {
          cluster_weight = c.cluster_weight.intValue
        }
    }
    hash_ring = create_hash_ring()
    super._start(^{
      connectors ::= cluster_connector
      update_cluster_state
      cluster.add(cluster_listener)
      cluster_connector.start(on_completed)
    })
  }

  override def _stop(on_completed: Runnable): Unit = {
    cluster.remove(cluster_listener)
    cluster.leave
    cluster_connector.stop(^{
      connectors = connectors.filterNot(_ == cluster_connector)
      super._stop(on_completed)
    })
  }

  def update_cluster_state: Unit = {
    val my_info = new PeerInfo.Bean
    my_info.setWeight(cluster_weight)
    config match {
      case c: ClusterBrokerDTO =>
        if (c.cluster_address != null)
          my_info.setClusterAddress(c.cluster_address)
      case _ =>
    }

    // We can infer the cluster address if it's not set...
    if (!my_info.hasClusterAddress) {
      connectors.foreach { connector =>
        connector match {
          case connector: AcceptingConnector =>
            connector.protocol match {
              case ClusterProtocol =>
                my_info.setClusterAddress(connector.transport_server.getConnectAddress)
              case x: AnyProtocol =>
                my_info.setClusterAddress(connector.transport_server.getConnectAddress)
              case _ => // Ignore other protocols..
            }
          case _ => // Ignore the outbound connector
        }
      }
      if (my_info.hasClusterAddress) {
        info("Cluster address infered to be: %s", my_info.getClusterAddress)
      } else {
        warn("Cluster address not set and it could not be infered.  Peer nodes may have problems connecting to us.")
      }
    }

    cluster.join(id, my_info.freeze)
  }

  def set_cluster_weight(value:Int): Unit = dispatch_queue {
    if( cluster_weight!= value ) {
      cluster_weight = value
      update_cluster_state
    }
  }

  def get_peer(peer_id:String) = dispatch_queue ! {
    get_or_create_peer(peer_id)
  }

  private var _peers = HashMap[String, Peer]()
  def get_or_create_peer(peer_id:String) = _peers.synchronized {
    _peers.getOrElseUpdate(peer_id, new Peer(this, peer_id))
  }
  def remove_peer(peer_id:String) = _peers.synchronized {
    _peers.remove(peer_id)
  }

  def peers = _peers.synchronized { _peers.clone }

  def peer_check = {
    val now = System.currentTimeMillis
    peers.values.foreach { peer=>
      peer.check
    }
  }

  val cluster_listener = new ClusterListener(){
    def on_cluster_change(members: List[(String, Option[Buffer])]) = dispatch_queue {
      val now = System.currentTimeMillis
      peers.values.foreach(x=> x.left_cluster_at = now )
      members.foreach { case (peer_id, data) =>
        if( peer_id != id ) {
          assert( data.isDefined )
          val peer = get_or_create_peer(peer_id)
          peer.left_cluster_at = 0
          peer.peer_info = data.get
        }
      }
      peers.foreach{ case (id, peer) =>
        if( peer.left_cluster_at != 0 ) {
          peer.close
          remove_peer(id)
        }
      }


      val new_ring = create_hash_ring()
      if( new_ring!=hash_ring ) {
        hash_ring = new_ring

        import collection.JavaConversions._
        println("Cluster membership changed to: "+hash_ring.getNodes.mkString(", "))
        // notify the hosts of the hash ring update...
        virtual_hosts.values.foreach { host=>
          host.dispatch_queue {
            host.router.asInstanceOf[ClusterRouter].on_cluster_change(new_ring)
          }
        }
      }
      peer_check
    }
  }

  def create_hash_ring() = {
    val rc = new HashRing[String, String](new Hasher.ToStringHasher(Hasher.JENKINS));
    if( cluster_weight > 0) {
      rc.add(id, cluster_weight)
    }
    peers.values.foreach{ x=>
      if( x.peer_info.getWeight > 0 ) {
        rc.add(x.id, x.peer_info.getWeight)
      }
    }
    rc
  }

}
