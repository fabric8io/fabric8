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

import org.scalatest.matchers.ShouldMatchers
import org.apache.zookeeper.server.{ZooKeeperServer, NIOServerCnxn}
import org.apache.zookeeper.server.persistence.FileTxnSnapLog
import org.linkedin.zookeeper.client.ZKClient
import org.linkedin.util.clock.Timespan
import java.net.InetSocketAddress
import scala.collection.immutable.List
import java.io.File
import org.fusesource.fabric.groups.GroupFactory
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, FunSuite}
import java.util.concurrent.TimeUnit

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@RunWith(classOf[JUnitRunner])
abstract class ZooKeeperFunSuiteSupport extends FunSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  var connector : NIOServerCnxn.Factory = _

  override protected def beforeAll() = {
    println("Starting ZooKeeper")
    val zk_server = new ZooKeeperServer();
    val data_dir = new File(new File("target"), "test-data")

    zk_server.setTxnLogFactory(new FileTxnSnapLog(new File(data_dir, "zk-log"), new File(data_dir, "zk-data")))
    connector = new NIOServerCnxn.Factory(new InetSocketAddress(0), 100)
    connector.startup(zk_server)
    println("ZooKeeper Started")
  }

  override protected def afterAll() = {
    if( connector!=null ) {
      connector.shutdown
      connector = null
    }
  }

  var zk_clients = List[ZKClient]()

  def create_zk_client() = {
    val client = new ZKClient("localhost:"+connector.getLocalPort, Timespan.parse("30s"), null)
    client.start
    zk_clients ::= client
    client.waitForStart(Timespan.parse("30s"))
    client
  }

  override protected def afterEach(): Unit = {
    zk_clients.foreach{ client=>
      try {
        client.close
      } catch {
        case _ =>
      }
    }
    zk_clients = List()
  }

}


class GroupsTest extends ZooKeeperFunSuiteSupport with ShouldMatchers {

  test("cluster events") {

    val cluster1 = GroupFactory.create(create_zk_client, "/example")
    val cluster2 = GroupFactory.create(create_zk_client, "/example")
    import TimeUnit._

    cluster1.join("1", null)
    within(2, SECONDS) {
      expect(List("1"))(cluster1.members.map(_.id).toList)
    }

    cluster2.join("2", null)
    within(2, SECONDS) {
      expect(List("1", "2"))(cluster1.members.map(_.id).toList)
    }

    // Check the we can get the member list without creating a Group object
    expect(List("1", "2"))(GroupFactory.members(create_zk_client, "/example").map(_.id).toList)

    // Check updating member data...
    expect(null)(cluster1.members.apply(1).data)
    cluster2.join("2", "Hello!".getBytes())
    within(2, SECONDS) {
      expect("Hello!")(new String(cluster1.members.apply(1).data))
    }

    // Check leaving the cluster
    cluster1.leave("1")
    within(2, SECONDS) {
      expect(List("2"))(cluster1.members.map(_.id).toList)
    }

  }

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
}
