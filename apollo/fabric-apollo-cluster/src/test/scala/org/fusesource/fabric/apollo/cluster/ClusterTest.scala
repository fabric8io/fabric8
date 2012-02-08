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

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.FileSupport._
import org.fusesource.hawtdispatch._
import java.util.concurrent.TimeUnit._
import org.apache.activemq.apollo.stomp.StompClient
import java.net.InetSocketAddress
import org.apache.activemq.apollo.dto.{BrokerDTO, XmlCodec, QueueDestinationDTO}
import java.util.Properties
import org.apache.activemq.apollo.util.{SocketProxy, ServiceControl, Dispatched}
import org.apache.activemq.apollo.broker.{SimpleAddress, Broker, Queue}
import org.apache.activemq.apollo.util.path.Path

/**
 */
class ClusterTestSupport extends ZkFunSuiteSupport with ShouldMatchers {

  var brokers = List[Broker]()
  var client = new StompClient
  var clients = List[StompClient]()

  def cluster_connector(broker:Broker) = broker.connectors.values.find(_.isInstanceOf[ClusterConnector]).map(_.asInstanceOf[ClusterConnector]).get
  def cluster_connectors(a1:Broker, a2:Broker):(ClusterConnector,ClusterConnector) = (cluster_connector(a1), cluster_connector(a1))
  def cluster_connectors(brokers:List[Broker]):List[ClusterConnector] = brokers.map(cluster_connector(_))

  override protected def beforeEach(): Unit = {
    super.beforeEach
  }


  override protected def afterEach(): Unit = {
    clients.foreach(_.close)
    clients = Nil
    brokers.foreach(x=> ServiceControl.stop(x, "Stopping broker: "+x))
    brokers = Nil
    super.afterEach
  }

  def connect(broker:Broker):StompClient = connect(broker.get_socket_address.asInstanceOf[InetSocketAddress].getPort)

  def connect(port:Int):StompClient = {
    val c = new StompClient
    clients ::= c
    c.open("localhost", port )
    c.write(
      "CONNECT\n" +
      "accept-version:1.1\n" +
      "host:localhost\n" +
      "\n")
    val frame = c.receive
    frame should startWith("CONNECTED\n")
    c
  }

  def create_cluster_broker(file:String, id:String, overrides:Map[String,String]=Map()) = {
    val broker = new Broker
    val base = basedir / "node-data" / id

    val p = new Properties()
    p.setProperty("apollo.base", base.getCanonicalPath)
    p.setProperty("apollo.cluster.id", id)
    p.setProperty("zk.url", zk_url)

    for( (key,value)<-overrides ) {
      p.setProperty(key, value)
    }

    val config = using(getClass.getResourceAsStream(file)) { is =>
      XmlCodec.decode(classOf[BrokerDTO], is, p)
    }

    debug("Starting broker");
    broker.config = config
    broker.tmp = base / "tmp"
    broker.tmp.mkdirs
    ServiceControl.start(broker, "starting broker "+id)

    brokers ::= broker
    broker
  }

  def router(bs:Broker) = bs.default_virtual_host.router.asInstanceOf[ClusterRouter]

  def access[T](d:Dispatched)(action: =>T) = {
    (d.dispatch_queue !! { action }).await()
  }

}

class ClusterTest extends ClusterTestSupport {

  test("starting to brokers with the same node id") {

    // Lets use a socket proxy so we can simulate a network disconnect.
    val proxy = new SocketProxy(new java.net.URI("tcp://"+zk_url))
    val proxy_url = "127.0.0.1:"+proxy.getUrl.getPort

    // First broker should become the master..
    val broker_a = create_cluster_broker("apollo-cluster.xml", "n1", Map("zk.url"->proxy_url))
    val cluster_a = cluster_connector(broker_a)
    within(5, SECONDS) ( access(cluster_a){ cluster_a.master } should be === true )

    // 2nd Broker should become the slave..
    val broker_b = create_cluster_broker("apollo-cluster.xml", "n1")
    val cluster_b = cluster_connector(broker_b)

    within(5, SECONDS) {
      val cluster = access(cluster_b){ cluster_b }
      cluster.master should be === false
      cluster.master_info.map(_.cluster_address) should be === Some(cluster_a.cluster_address)
    }

    // simulate a disconnect of the master from zk
    var start = System.currentTimeMillis()
    proxy.suspend()

    // Default session timeout is set to 5 seconds..
    within(10, SECONDS) {
      val a_master = access(cluster_a){ cluster_a.master }
      val b_master = access(cluster_b){ cluster_b.master }
      if (a_master && b_master) {
        exit_within_with_failure("a and b cannot be master at the same time.")
      }
      a_master should be === false
    }
    var end = System.currentTimeMillis()
    System.out.println("Master shutdown in (ms): "+(end-start))

    start = System.currentTimeMillis()
    within(10, SECONDS) ( access(cluster_b){ cluster_b.master } should be === true )
    end = System.currentTimeMillis()
    System.out.println("Slave startup in (ms): "+(end-start))

  }

  test("sending and subscribe on a cluster slave") {
    val broker_a = create_cluster_broker("apollo-cluster.xml", "a")
    val broker_b = create_cluster_broker("apollo-cluster.xml", "b")
    val (cluster_a,cluster_b) = cluster_connectors(broker_a, broker_b)

    // Both brokers should establish peer connections ...
    for( broker <- List(cluster_a, cluster_b) ) {
      within(5, SECONDS) ( access(broker){ broker.peers.size } should be > 0 )
    }

    val router_a = router(broker_a)
    val router_b = router(broker_b)

    router_a should not( be === router_b )

    // Lets get a cluster destination.. only one of them should be picked as the master..
    val dest_a = access(router_a)(router_a.get_or_create_destination(SimpleAddress("queue", Path("test")), null)).success.asInstanceOf[ClusterRouter#ClusterDestination[Queue]]
    val dest_b = access(router_b)(router_b.get_or_create_destination(SimpleAddress("queue", Path("test")), null)).success.asInstanceOf[ClusterRouter#ClusterDestination[Queue]]

    (access(router_a)(dest_a.is_tail) ^ access(router_b)(dest_b.is_tail)) should be === true

    // sort out which is the master and which is the slave..
    val (master, slave, master_dest, slave_dest) = if (dest_a.is_tail) {
      (broker_a, broker_b, dest_a, dest_b)
    } else {
      (broker_b, broker_a, dest_b, dest_a)
    }

    // Lets setup some stomp connections..
    val slave_client = connect(slave)

    val MESSAGE_COUNT = 1000

    // send a messages on the slave.. it should get sent to the master.
    for( i <- 1 to MESSAGE_COUNT ) {
      slave_client.write(
        """|SEND
           |destination:/queue/test
           |
           |#%d""".format(i).stripMargin)

    }

    // that message should end up on the the master's queue...
    val master_queue = master_dest.local
    within(10, SECONDS) ( access(master_queue){ master_queue.queue_items } should be === MESSAGE_COUNT )

    println("============================================================")
    println("subscribing...")
    println("============================================================")

    // receive a message on the slave..
    slave_client.write(
      """|SUBSCRIBE
         |destination:/queue/test
         |id:0
         |
         |""".stripMargin)

    println("waiting for messages...")
    for( i <- 1 to MESSAGE_COUNT ) {
      val frame = slave_client.receive()
      frame should startWith("MESSAGE\n")
      frame should endWith("\n\n#"+i)
    }

    // master queue should drain.
    within(5, SECONDS) ( access(master_queue){ master_queue.queue_items } should be === 0 )

  }

  ignore("Migrate a queue") {

    val broker_a = create_cluster_broker("apollo-cluster.xml", "a")
    val broker_b = create_cluster_broker("apollo-cluster.xml", "b")
    val (cluster_a,cluster_b) = cluster_connectors(broker_a, broker_b)

    // Both brokers should establish peer connections ...
    for( broker <- List(cluster_a, cluster_b) ) {
      within(5, SECONDS) ( access(broker){ broker.peers.size } should be > 0 )
    }

    val router_a = router(broker_a)
    val router_b = router(broker_b)

    router_a should not( be === router_b )

    // Lets get a cluster destination.. only one of them should be picked as the master..
    val dest_a = access(router_a)(router_a.get_or_create_destination(SimpleAddress("queue", Path("test")), null)).success.asInstanceOf[ClusterRouter#ClusterDestination[Queue]]
    val dest_b = access(router_b)(router_b.get_or_create_destination(SimpleAddress("queue", Path("test")), null)).success.asInstanceOf[ClusterRouter#ClusterDestination[Queue]]

    (access(router_a)(dest_a.is_tail) ^ access(router_b)(dest_b.is_tail)) should be === true

    // sort out which is the master and which is the slave..
    val (master, slave, master_dest, slave_dest) = if (dest_a.is_tail) {
      (cluster_a, cluster_b, dest_a, dest_b)
    } else {
      (cluster_b, cluster_a, dest_b, dest_a)
    }

    // Lets setup some stomp connections..
    val slave_client = connect(slave.broker)

    // send a message on the slave.. it should get sent to the master.
    slave_client.write(
      """|SEND
         |destination:/queue/test
         |
         |#1""".stripMargin)

    // that message should end up on the the master's queue...
    val master_queue = master_dest.local
    val slave_queue = slave_dest.local

    // verify master queue has the message.
    within(5, SECONDS) ( access(master_queue){ master_queue.queue_items } should be === 1 )
    // slave queue should not have the message.
    access(slave_queue){ slave_queue.queue_items } should be === 0

    // Changing the cluster weight of the master to zero, should make him a slave.
    println("Master is "+master.node_id+", changing weight to convert to slave.")
    master.set_cluster_weight(0)

//    Thread.sleep(1000*1000)
    // slave becomes master.. message has to move to the new queue.
    within(5, SECONDS) ( access(slave_queue){ slave_queue.queue_items } should be === 1 )

  }

}