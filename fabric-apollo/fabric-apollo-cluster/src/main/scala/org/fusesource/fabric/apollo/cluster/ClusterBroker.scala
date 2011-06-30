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

import dto.{ClusterNodeDTO, ClusterBrokerDTO}
import org.fusesource.hawtdispatch._
import java.lang.String
import org.apache.activemq.apollo.broker._
import java.io.IOException
import org.apache.activemq.apollo.transport.TransportFactory
import org.apache.activemq.apollo.util._
import org.fusesource.fabric.apollo.cluster.protocol.ClusterProtocol
import org.fusesource.hawtbuf.Buffer
import collection.mutable.HashMap
import org.apache.activemq.apollo.broker.protocol.{ProtocolHandler, AnyProtocol}
import org.fusesource.fabric.apollo.cluster.util.{HashRing, Hasher}
import org.fusesource.fabric.groups.{ChangeListener, Group}
import org.apache.activemq.apollo.dto.{ConnectorDTO, JsonCodec}
import java.net.SocketAddress

object ClusterBroker extends Log {
}

/**
 * A cluster broker is a broker which cooperates with peers in a cluster
 * to increase scalability and availability.
 *
 * This class in responsible for tracking the peers of the cluster.
 */
class ClusterBroker(override val id:String, val cluster:Group) extends Broker {
  import ClusterBroker._

  var hash_ring:HashRing[String, String] = _

  /**
   * This is a connector that handles establishing outbound connections.
   */
  object cluster_connector extends Connector {

    val connected = new LongCounter()
    val accepted = new LongCounter()

    def broker = ClusterBroker.this
    def id = "cluster"
    def config = null
    def dispatch_queue = ClusterBroker.this.dispatch_queue

    protected def _start(on_completed: Runnable) = {
      on_completed.run
    }

    def stopped(connection: BrokerConnection) = dispatch_queue {
      if( broker.connections.remove(connection.id).isDefined ) {
        connected.decrementAndGet()
      }
    }

    protected def _stop(on_completed: Runnable) = dispatch_queue {
      on_completed.run
    }

    def connect(location:String)(on_complete: Result[BrokerConnection, IOException]=>Unit) = {
      try {

        connected.incrementAndGet()
        val outbound_connection = new BrokerConnection(cluster_connector, connection_id_counter.incrementAndGet)
        outbound_connection.protocol_handler = new ProtocolHandler() {
          def protocol: String = "outbound"

          override def on_transport_connected = {
            on_complete(Success(outbound_connection))
          }

          override def on_transport_failure(error: IOException) = {
            on_complete(Failure(error))
            outbound_connection.stop
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

    def update(config: ConnectorDTO, on_complete: Runnable) = {
      on_complete.run()
    }

    def socket_address: SocketAddress = null
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
      connectors.put(cluster_connector.id, cluster_connector)
      update_cluster_state
      cluster.add(cluster_listener)
      cluster_connector.start(on_completed)
    })
  }

  override def _stop(on_completed: Runnable): Unit = {
    cluster.remove(cluster_listener)
    cluster.leave(id)
    cluster_connector.stop(^{
      connectors.remove(cluster_connector.id)
      super._stop(on_completed)
    })
  }

  def update_cluster_state: Unit = {
    val my_info = new ClusterNodeDTO
    my_info.id = id
    my_info.weight = cluster_weight
    config match {
      case c: ClusterBrokerDTO =>
        if (c.cluster_address != null)
          my_info.cluster_address = c.cluster_address
      case _ =>
    }

    // We can infer the cluster address if it's not set...
    if ( my_info.cluster_address==null ) {
      connectors.foreach { case (id, connector) =>
        connector match {
          case connector: AcceptingConnector =>
            connector.protocol match {
              case ClusterProtocol =>
                 my_info.cluster_address = connector.transport_server.getConnectAddress
              case x: AnyProtocol =>
                 my_info.cluster_address = connector.transport_server.getConnectAddress
              case _ => // Ignore other protocols..
            }
          case _ => // Ignore the outbound connector
        }
      }
      if (my_info.cluster_address!=null) {
        info("Cluster address infered to be: %s", my_info.cluster_address)
      } else {
        warn("Cluster address not set and it could not be infered.  Peer nodes may have problems connecting to us.")
      }
    }

    cluster.join(id, JsonCodec.encode(my_info).toByteArray)
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

  val cluster_listener = new ChangeListener(){
    def changed(members: Array[Array[Byte]]) {
      on_cluster_change(members.toList.flatMap{ data=>
        try {
          Some(JsonCodec.decode(new Buffer(data), classOf[ClusterNodeDTO]))
        } catch {
          case _ => None
        }
      })
    }

    def on_cluster_change(members: List[ClusterNodeDTO]) = dispatch_queue {
      val now = System.currentTimeMillis
      peers.values.foreach(x=> x.left_cluster_at = now )
      members.foreach { case node_dto =>
        if( node_dto.id != id ) {
          val peer = get_or_create_peer(node_dto.id)
          peer.left_cluster_at = 0
          peer.peer_info = node_dto
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
      if( x.peer_info.weight > 0 ) {
        rc.add(x.id, x.peer_info.weight)
      }
    }
    rc
  }

}
