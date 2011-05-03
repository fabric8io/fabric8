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

import org.scalatest.matchers.ShouldMatchers
import org.apache.activemq.apollo.util.FileSupport._
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.CreateMode
import org.fusesource.hawtdispatch._
import java.util.concurrent.TimeUnit._
import java.util.concurrent.TimeUnit
import org.apache.activemq.apollo.util.Dispatched
import org.apache.activemq.apollo.stomp.StompClient
import org.apache.activemq.apollo.dto.QueueDestinationDTO
import org.apache.activemq.apollo.broker.Queue

/**
 */
class ClusterBrokerTest extends ZkFunSuiteSupport with ShouldMatchers {

  var cluster_brokers = List[ClusterBrokerService]()
  var broker_service_a:ClusterBrokerService = _
  var broker_service_b:ClusterBrokerService = _

  var client = new StompClient
  var clients = List[StompClient]()

  override protected def beforeAll() = {
    super.beforeAll

    // Store the cluster configuration in ZK since that's where the cluster
    // brokers pull it down from.
    val client = create_zk_client
    val config = read_text(getClass.getResourceAsStream("apollo-cluster.xml"))
    client.createOrSetWithParents("/cluster_test/config/default", config, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach
    broker_service_a = create_cluster_broker("broker-a")
    broker_service_b = create_cluster_broker("broker-b")
  }

  override protected def afterEach(): Unit = {
    clients.foreach(_.close)
    clients = Nil

    cluster_brokers.foreach { x =>
      x.stop
    }
    cluster_brokers = Nil
    super.afterEach
  }

  def connect(broker:ClusterBrokerService):StompClient = connect(broker.broker.get_socket_address.getPort)

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

  def create_cluster_broker(id:String) = {
    val zk_client = create_zk_client

    val broker_service = new ClusterBrokerService
    cluster_brokers ::= broker_service
    broker_service.basedir = test_data_dir / "cluster" / id
    broker_service.zk_client = zk_client
    broker_service.zk_root = "/cluster_test"
    broker_service.id = id
    broker_service.start
    broker_service
  }

  test("sending and subscribe on a cluster slave") {

    // Both brokers should startup ...
    for( broker <- List(broker_service_a, broker_service_b) ) {
      within(5, SECONDS) ( access(broker){ broker.started_counter } should be === 1 )
    }

    // Both brokers should establish peer connections ...
    for( broker <- List(broker_service_a, broker_service_b) ) {
      within(5, SECONDS) ( access(broker){ broker.broker.peers.size } should be > 0 )
    }

    val router_a = router(broker_service_a)
    val router_b = router(broker_service_b)

    router_a should not( be === router_b )

    // Lets get a cluster destination.. only one of them should be picked as the master..
    val dest_a = access(router_a)(router_a._get_or_create_destination(new QueueDestinationDTO("test"), null)).success.asInstanceOf[ClusterRouter#ClusterDestination[Queue]]
    val dest_b = access(router_b)(router_b._get_or_create_destination(new QueueDestinationDTO("test"), null)).success.asInstanceOf[ClusterRouter#ClusterDestination[Queue]]

    (access(router_a)(dest_a.is_master) ^ access(router_b)(dest_b.is_master)) should be === true

    // sort out which is the master and which is the slave..
    val (master, slave, master_dest, slave_dest) = if (dest_a.is_master) {
      (broker_service_a, broker_service_b, dest_a, dest_b)
    } else {
      (broker_service_b, broker_service_a, dest_b, dest_a)
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

    // Both brokers should startup ...
    for( broker <- List(broker_service_a, broker_service_b) ) {
      within(5, SECONDS) ( access(broker){ broker.started_counter } should be === 1 )
    }

    // Both brokers should establish peer connections ...
    for( broker <- List(broker_service_a, broker_service_b) ) {
      within(5, SECONDS) ( access(broker){ broker.broker.peers.size } should be > 0 )
    }

    val router_a = router(broker_service_a)
    val router_b = router(broker_service_b)

    router_a should not( be === router_b )

    // Lets get a cluster destination.. only one of them should be picked as the master..
    val dest_a = access(router_a)(router_a._get_or_create_destination(new QueueDestinationDTO("test"), null)).success.asInstanceOf[ClusterRouter#ClusterDestination[Queue]]
    val dest_b = access(router_b)(router_b._get_or_create_destination(new QueueDestinationDTO("test"), null)).success.asInstanceOf[ClusterRouter#ClusterDestination[Queue]]

    (access(router_a)(dest_a.is_master) ^ access(router_b)(dest_b.is_master)) should be === true

    // sort out which is the master and which is the slave..
    val (master, slave, master_dest, slave_dest) = if (dest_a.is_master) {
      (broker_service_a, broker_service_b, dest_a, dest_b)
    } else {
      (broker_service_b, broker_service_a, dest_b, dest_a)
    }

    // Lets setup some stomp connections..
    val slave_client = connect(slave)

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
    println("Master is "+master.broker.id+", changing weight to convert to slave.")
    master.broker.set_cluster_weight(0)

//    Thread.sleep(1000*1000)
    // slave becomes master.. message has to move to the new queue.
    within(5, SECONDS) ( access(slave_queue){ slave_queue.queue_items } should be === 1 )

  }


  def router(bs:ClusterBrokerService) = bs.broker.default_virtual_host.router.asInstanceOf[ClusterRouter]

  def within[T](timeout:Long, unit:TimeUnit)(func: => Unit ):Unit = {
    val start = System.currentTimeMillis
    var amount = unit.toMillis(timeout)
    var sleep_amount = amount / 100
    var last:Throwable = null

    if( sleep_amount < 1 ) {
      sleep_amount = 1
    }
    try {
      func
      return
    } catch {
      case e:Throwable => last = e
    }

    while( (System.currentTimeMillis-start) < amount ) {
      Thread.sleep(sleep_amount)
      try {
        func
        return
      } catch {
        case e:Throwable => last = e
      }
    }

    throw last
  }

  def access[T](d:Dispatched)(action: =>T) = {
    (d.dispatch_queue !! { action }).await()
  }

}
