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

import dto.{ClusterVirtualHostDTO, ClusterConnectorDTO, ClusterNodeDTO}
import org.fusesource.hawtdispatch._
import org.apache.activemq.apollo.broker._
import java.io.IOException
import org.apache.activemq.apollo.transport.TransportFactory
import org.apache.activemq.apollo.util._
import org.fusesource.fabric.apollo.cluster.protocol.ClusterProtocol
import collection.mutable.HashMap
import org.apache.activemq.apollo.broker.protocol.{ProtocolHandler, AnyProtocol}
import org.fusesource.fabric.apollo.cluster.util.{HashRing, Hasher}
import java.net.SocketAddress
import org.linkedin.util.clock.Timespan
import org.fusesource.fabric.groups.{ZooKeeperGroupFactory, ChangeListener, Group}
import org.apache.zookeeper.{WatchedEvent, Watcher}
import org.linkedin.zookeeper.client.{LifecycleListener, ZKClient}
import org.fusesource.hawtbuf.{AsciiBuffer, Buffer}
import org.apache.activemq.apollo.dto.{ConnectorStatusDTO, ServiceStatusDTO, ConnectorTypeDTO, JsonCodec}
import java.lang.{IllegalArgumentException, String}

object ClusterConnectorFactory extends ConnectorFactory.Provider with Log {

  def create(broker: Broker, dto: ConnectorTypeDTO): Connector = dto match {
    case dto:ClusterConnectorDTO =>
      val rc = new ClusterConnector(broker, dto.id)
      rc.config = dto
      rc
    case _ => null
  }
}

object ClusterConnector extends Log

/**
 * This is a connector that handles establishing outbound connections.
 */
class ClusterConnector(val broker:Broker, val id:String) extends Connector {
  import ClusterConnector._

  var config:ClusterConnectorDTO = _

  val connected = new LongCounter()
  val accepted = new LongCounter()

  var zk_client:ZKClient = _
  var hosts_stopped_due_to_disconnect = List[AsciiBuffer]()
  var cluster:Group = _
  var hash_ring:HashRing[String, String] = _

  def dispatch_queue = broker.dispatch_queue

  def node_id:String = config.node_id
  var cluster_weight = 16


  def status: ServiceStatusDTO = {
    val result = new ConnectorStatusDTO
    result.id = id.toString
    result.state = service_state.toString
    result.state_since = service_state.since
    result.connection_counter = accepted.get
    result.connected = connected.get
    result.protocol = "cluster"
    result.local_address = "none"
    result
  }

  protected def _start(on_completed: Runnable) = {

    import org.apache.activemq.apollo.util.OptionSupport._
    def not_null(value:AnyRef, msg:String) = if (value==null) throw new IllegalArgumentException("The cluster connector's %s was not configured".format(msg))
    not_null(config.node_id, "node_id");
    not_null(config.zk_url, "zk_url");
    not_null(config.zk_group_path, "zk_group_path");

    cluster_weight = config.weight.getOrElse(16)
    hash_ring = create_hash_ring()

    var timeout = Timespan.parse(Option(config.zk_timeout).getOrElse("30s"))
    zk_client = new ZKClient(config.zk_url, timeout, null)
    zk_client.registerListener(new LifecycleListener {

      def onDisconnected() = dispatch_queue {
        // TODO:
        // ZK is our HA service which allows us to resolve network splits,
        // if we can't stay connected to it, then we might be in a network
        // split situation so we should shutdown our services.

        // Stop all the virtual hosts..
        broker.virtual_hosts.foreach { case (id, host) =>
          if (host.service_state.is_starting_or_started) {
            hosts_stopped_due_to_disconnect ::= id
            host.stop
          }
        }
        cluster = null
      }

      def onConnected() = dispatch_queue {

        hosts_stopped_due_to_disconnect.foreach { id =>
          broker.virtual_hosts.get(id).foreach(_.start)
        }
        hosts_stopped_due_to_disconnect = Nil

        if( cluster==null ) {
          cluster = ZooKeeperGroupFactory.create(zk_client, config.zk_group_path)
          update_cluster_state
          cluster.add(cluster_listener)
        }
      }
    })

    zk_client.start
    on_completed.run
  }


  override def _stop(on_completed: Runnable): Unit = {
    cluster.remove(cluster_listener)
    cluster.leave(node_id)
    zk_client.close()
    on_completed.run()
  }

  def stopped(connection: BrokerConnection) = dispatch_queue {
    if( broker.connections.remove(connection.id).isDefined ) {
      connected.decrementAndGet()
    }
  }

  def connect(location:String)(on_complete: Result[BrokerConnection, IOException]=>Unit) = {
    try {

      connected.incrementAndGet()
      val outbound_connection = new BrokerConnection(this, broker.connection_id_counter.incrementAndGet)
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
      broker.init_dispatch_queue(outbound_connection.dispatch_queue)

      outbound_connection.transport = TransportFactory.connect(location)
      broker.connections.put(outbound_connection.id, outbound_connection)
      outbound_connection.start

    } catch {
      case error:IOException => on_complete(Failure(error))
    }
  }

  def update(config: ConnectorTypeDTO, on_complete: Runnable) = {
    on_complete.run()
  }

  def socket_address: SocketAddress = null


  def update_cluster_state: Unit = {
    val my_info = new ClusterNodeDTO
    my_info.id = node_id
    my_info.weight = cluster_weight
    my_info.cluster_address = config.address

    // We can infer the cluster address if it's not set...
    if ( my_info.cluster_address==null ) {
      broker.connectors.foreach { case (id, connector) =>
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

    cluster.join(node_id, JsonCodec.encode(my_info).toByteArray)
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
        if( node_dto.id != node_id ) {
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
        broker.virtual_hosts.values.foreach { host=>
          host match {
            case host:ClusterVirtualHost =>
              host.dispatch_queue {
                host.router.on_cluster_change(ClusterConnector.this, new_ring)
              }
            case _ =>
          }
        }
      }
      peer_check
    }
  }

  def create_hash_ring() = {
    val rc = new HashRing[String, String](new Hasher.ToStringHasher(Hasher.JENKINS));
    if( cluster_weight > 0) {
      rc.add(node_id, cluster_weight)
    }
    peers.values.foreach{ x=>
      if( x.peer_info.weight > 0 ) {
        rc.add(x.id, x.peer_info.weight)
      }
    }
    rc
  }

}