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
import org.apache.activemq.apollo.broker.protocol.{ProtocolHandler, AnyProtocol}
import org.fusesource.fabric.apollo.cluster.util.{HashRing, Hasher}
import java.net.SocketAddress
import org.linkedin.util.clock.Timespan
import org.fusesource.fabric.groups.{ZooKeeperGroupFactory, ChangeListener, Group}
import org.apache.zookeeper.{WatchedEvent, Watcher}
import org.linkedin.zookeeper.client.{LifecycleListener, ZKClient}
import org.fusesource.hawtbuf.{AsciiBuffer, Buffer}
import org.apache.activemq.apollo.dto.{ConnectorStatusDTO, ServiceStatusDTO, ConnectorTypeDTO, JsonCodec}
import collection.mutable.{ListBuffer, HashMap}
import java.lang.{Boolean, IllegalArgumentException, String}

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
  import ClusterConnector._

  var config:ClusterConnectorDTO = _

  val connected = new LongCounter()
  val accepted = new LongCounter()

  var zk_client:ZKClient = _
  var hosts_stopped_due_to_disconnect = List[AsciiBuffer]()
  var cluster_group:Group = _
  var node_group:Group = _
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
      // We can probably infer the cluster address if it's not set...
      // Try to get the first cluster connectable connector address.
      broker.connectors.flatMap { case (id, connector) =>
        connector match {
          case connector: AcceptingConnector =>
            connector.protocol match {
              case ClusterProtocol => Some(connector.transport_server.getConnectAddress)
              case x: AnyProtocol => Some(connector.transport_server.getConnectAddress)
              case _ => None
            }
          case _ => None
        }
      }.headOption
    }.getOrElse(throw new IllegalArgumentException("The cluster connector's address was not configured"))

    hash_ring = create_hash_ring()

    var timeout = Timespan.parse(Option(config.zk_timeout).getOrElse("5s"))
    zk_client = new ZKClient(config.zk_url, timeout, null)
    zk_client.registerListener(new LifecycleListener {

      def onDisconnected() = dispatch_queue {
        // TODO:
        // ZK is a HA service which allows us to resolve network splits,
        // if we can't stay connected to it, then we might be in a network
        // split situation so we should shutdown our services.

        // Start all the virtual hosts..
        broker.virtual_hosts.values.foreach { _ match {
          case host:ClusterVirtualHost =>
            host.dispatch_queue {
              host.make_slave(None)
            }
          case _ =>
        } }

        master = false
        master_info = None
        cluster_group = null

        cluster_listeners.foreach {
          _.on_change
        }
      }

      def onConnected() = dispatch_queue {

        hosts_stopped_due_to_disconnect.foreach { id =>
          broker.virtual_hosts.get(id).foreach(_.start)
        }
        hosts_stopped_due_to_disconnect = Nil

        if( cluster_group==null ) {
          cluster_group = ZooKeeperGroupFactory.create(zk_client, config.zk_directory+"/brokers")
          update_cluster_state
          cluster_group.add(change_listener)
        }
      }
    })

    zk_client.start
    on_completed.run
  }


  override def _stop(on_completed: Runnable): Unit = {
    if( cluster_group!=null ) {
      cluster_group.remove(change_listener)
      cluster_group.leave(node_id)
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
    my_info.cluster_address = cluster_address
    cluster_group.join(node_id, JsonCodec.encode(my_info).toByteArray)
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
    peers.values.foreach { peer=>
      peer.check
    }
  }

  val change_listener = new ChangeListener() {

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

      // Group the node entries by node id, only the first is the master,
      // all others can be ignored.
      val members_by_id = HashMap[String, ListBuffer[ClusterNodeDTO]]()
      members.foreach { case node =>
        members_by_id.getOrElseUpdate(node.id, ListBuffer[ClusterNodeDTO]()).append(node)
      }

      // Is this node a master??
      val our_node_members = members_by_id.remove(node_id)
      master_info = our_node_members.flatMap(_.headOption)
      val is_master = master_info.map(_.cluster_address == cluster_address).getOrElse(false)

      // are we switching to be the master?
      if( is_master  ) {
        master = true
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
      } else if( !is_master ) {
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

      if(is_master) {

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