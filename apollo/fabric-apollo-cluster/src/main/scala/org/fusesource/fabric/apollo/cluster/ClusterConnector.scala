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

import dto.{ClusterConnectorDTO, ClusterNodeDTO}
import org.fusesource.hawtdispatch._
import org.apache.activemq.apollo.broker._
import java.io.IOException
import org.apache.activemq.apollo.util._
import org.fusesource.fabric.apollo.cluster.protocol.ClusterProtocol
import org.apache.activemq.apollo.broker.protocol.{ProtocolHandler, AnyProtocol}
import org.fusesource.fabric.apollo.cluster.util.{HashRing, Hasher}
import java.net.SocketAddress
import org.linkedin.util.clock.Timespan
import org.linkedin.zookeeper.client.ZKClient
import org.fusesource.hawtbuf.AsciiBuffer
import org.apache.activemq.apollo.dto.{ConnectorStatusDTO, ServiceStatusDTO, ConnectorTypeDTO}
import collection.mutable.HashMap
import java.lang.{IllegalArgumentException, String}
import org.fusesource.fabric.groups._
import org.apache.activemq.apollo.broker.transport.TransportFactory

object ClusterConnectorFactory extends ConnectorFactory with Log {

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

  var config:ClusterConnectorDTO = _

  val connected = new LongCounter()
  val accepted = new LongCounter()

  var zk_client:ZKClient = _
  var hosts_stopped_due_to_disconnect = List[AsciiBuffer]()
  var cluster_group:Group = _
  var hash_ring:HashRing[String, String] = _

  var master = false
  var master_info:Option[ClusterNodeDTO] = None

  def dispatch_queue = broker.dispatch_queue

  def node_id:String = config.node_id
  var cluster_weight = 16
  var cluster_address:String = _
  var cluster_listeners:List[ClusterListener] = Nil

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

    cluster_listeners = ClusterListenerFactory.create(this)

    import org.apache.activemq.apollo.util.OptionSupport._
    def not_null(value:AnyRef, msg:String) = if (value==null) throw new IllegalArgumentException("The cluster connector's %s was not configured".format(msg))

    not_null(config.node_id, "node_id")
    not_null(config.zk_url, "zk_url")
    not_null(config.zk_directory, "zk_directory")

    cluster_weight = config.weight.getOrElse(16)
    cluster_address = Option(config.address).orElse {
      broker.get_connect_address
    }.getOrElse(throw new IllegalArgumentException("The cluster connector's address was not configured"))

    hash_ring = create_hash_ring()

    var timeout = Timespan.parse(Option(config.zk_timeout).getOrElse("5s"))
    zk_client = new ZKClient(config.zk_url, timeout, null)
    Broker.BLOCKABLE_THREAD_POOL {
      zk_client.start
      zk_client.waitForStart()
      cluster_group = ZooKeeperGroupFactory.create(zk_client, config.zk_directory+"/brokers")
      cluster_singleton.start(cluster_group)
      cluster_singleton.join
      dispatch_queue {
        on_completed.run
      }
    }
  }


  override def _stop(on_completed: Runnable): Unit = {
    if( cluster_group!=null ) {
      cluster_singleton.stop
      cluster_group = null
    }
    zk_client.close()
    cluster_listeners.foreach(_.close)
    cluster_listeners = Nil
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

        def session_id = None

        def protocol: String = "outbound"

        override def on_transport_connected = {
          on_complete(Success(outbound_connection))
        }

        override def on_transport_failure(error: IOException) = {
          on_complete(Failure(error))
          outbound_connection.stop
        }
      }

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

  object cluster_singleton extends ClusteredSingleton[ClusterNodeDTO](classOf[ClusterNodeDTO]) {

    def create_state = {
      val rc = new ClusterNodeDTO
      rc.id = node_id
      rc.weight = cluster_weight
      rc.cluster_address = cluster_address
      rc
    }

    def join:Unit= join(create_state)
    def update:Unit = update(create_state)

    add(new ChangeListener(){
      def connected = changed
      def changed = on_cluster_change
      def disconnected = {
        // Stop all the virtual hosts..
        broker.virtual_hosts.values.foreach { _ match {
          case host:ClusterVirtualHost =>
            host.dispatch_queue {
              host.make_slave(None)
            }
          case _ =>
        } }

        ClusterConnector.this.master = false
        master_info = None
        cluster_listeners.foreach {
          _.on_change
        }
      }
    })
  }

  def set_cluster_weight(value:Int): Unit = dispatch_queue {
    if( cluster_weight!= value ) {
      cluster_weight = value
      cluster_singleton.update
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
    peers.values.foreach { peer=>
      peer.check
    }
  }

  def on_cluster_change = dispatch_queue {
    val (active, activeNodeState, members_by_id) = cluster_singleton.synchronized {
      (cluster_singleton.isMaster, cluster_singleton.master, cluster_singleton.members.mapValues(_.map(_._2)))
    }
    master = active
    master_info = activeNodeState

    // are we switching to be the master?
    if( master  ) {
      master_info = None
      // notify all the virtual hosts so that they can startup..
      broker.virtual_hosts.values.foreach { host=>
        host match {
          case host:ClusterVirtualHost =>
            host.dispatch_queue {
              host.make_master
            }
          case _ =>
        }
      }
    } else {
      master = false

      // notify all the virtual hosts so that they can shutdown..
      broker.virtual_hosts.values.foreach { host=>
        host match {
          case host:ClusterVirtualHost =>
            host.dispatch_queue {
              host.make_slave( master_info.flatMap(x=> Option(x.client_address)) )
            }
          case _ =>
        }
      }
    }

    if(master) {

      val now = System.currentTimeMillis
      peers.values.foreach(x=> x.left_cluster_at = now )

      members_by_id.foreach { case (id, nodes) =>
        val peer = get_or_create_peer(id)
        peer.left_cluster_at = 0
        peer.peer_info = nodes.head
      }

      // Drop peers that have left the cluster.
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

    } else {
      // disconnect since we are not the master..
      peers.foreach{ case (id, peer) =>
        peer.close
        remove_peer(id)
      }
    }

    cluster_listeners.foreach {
      _.on_change
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