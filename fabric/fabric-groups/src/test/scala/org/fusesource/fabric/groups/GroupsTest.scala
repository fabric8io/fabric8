/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.groups

import org.scalatest.matchers.ShouldMatchers
import org.apache.zookeeper.server.{ZooKeeperServer, NIOServerCnxnFactory}
import org.apache.zookeeper.server.persistence.FileTxnSnapLog
import org.linkedin.zookeeper.client.ZKClient
import org.linkedin.util.clock.Timespan
import java.net.InetSocketAddress
import scala.collection.immutable.List
import java.io.File
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, FunSuite}
import java.util.concurrent.TimeUnit
import collection.JavaConversions._

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@RunWith(classOf[JUnitRunner])
abstract class ZooKeeperFunSuiteSupport extends FunSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  var connector : NIOServerCnxnFactory = _

  override protected def beforeAll() = {
    println("Starting ZooKeeper")
    val zk_server = new ZooKeeperServer();
    val data_dir = new File(new File("target"), "test-data")

    zk_server.setTxnLogFactory(new FileTxnSnapLog(new File(data_dir, "zk-log"), new File(data_dir, "zk-data")))
    connector = new NIOServerCnxnFactory
    connector.configure(new InetSocketAddress(0), 100)
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

  private class BreakWithin(e:Throwable) extends RuntimeException(e)

  def breaks_within[T](func: => T) = {
    try {
      func
    } catch {
      case e:Throwable => throw new BreakWithin(e)
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
      case e:BreakWithin => throw e.getCause
      case e:Throwable => last = e
    }

    while( (System.currentTimeMillis-start) < amount ) {
      Thread.sleep(sleep_amount)
      try {
        func
        return
      } catch {
        case e:BreakWithin => throw e.getCause
        case e:Throwable => last = e
      }
    }

    throw last
  }

}


class GroupsTest extends ZooKeeperFunSuiteSupport with ShouldMatchers {

  test("cluster events") {

    val cluster1 = ZooKeeperGroupFactory.create(create_zk_client, "/example")
    val cluster2 = ZooKeeperGroupFactory.create(create_zk_client, "/example")
    import TimeUnit._

    val c1id1 = cluster1.join("1".getBytes)
    within(2, SECONDS) {
      expect(List("1"))(cluster1.members.toMap.values.map(new String(_)).toList)
    }

    val c2id2 = cluster2.join("2".getBytes)
    within(2, SECONDS) {
      expect(List("1", "2"))(cluster1.members.toMap.values.map(new String(_)).toList)
    }

    // Check the we can get the member list without creating a Group object
    expect(List("1", "2"))(ZooKeeperGroupFactory.members(create_zk_client, "/example").toMap.values.map(new String(_)).toList)

    // Check updating member data...
    expect("2")(new String(cluster1.members.get(c2id2)))
    cluster2.update(c2id2, "Hello!".getBytes())
    within(2, SECONDS) {
      expect("Hello!")(new String(cluster1.members.get(c2id2)))
    }

    // Check leaving the cluster
    cluster1.leave(c1id1)
    within(2, SECONDS) {
      expect(List("Hello!"))(cluster1.members.toMap.values.map(new String(_)).toList)
    }

  }

}
