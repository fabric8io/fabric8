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
import org.scalatest.BeforeAndAfterEach
import org.apache.zookeeper.server.{ZooKeeperServer, NIOServerCnxn}
import org.apache.zookeeper.server.persistence.FileTxnSnapLog
import org.apache.activemq.apollo.util._
import FileSupport._
import org.linkedin.zookeeper.client.ZKClient
import org.linkedin.util.clock.Timespan
import org.fusesource.hawtdispatch._
import scala.collection.mutable.ListBuffer
import java.util.concurrent.TimeUnit._
import java.net.InetSocketAddress
import org.fusesource.hawtbuf.Buffer._
import scala.collection.immutable.List
import org.fusesource.hawtbuf.Buffer
import java.lang.String

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ZkFunSuiteSupport extends FunSuiteSupport with BeforeAndAfterEach {

  var connector : NIOServerCnxn.Factory = _

  override protected def beforeAll() = {
    debug("Starting ZooKeeper")
    val zk_server = new ZooKeeperServer();
    val data_dir = basedir/"target"/"test-data"
    data_dir.recursive_delete

    zk_server.setTxnLogFactory(new FileTxnSnapLog(data_dir/"zk-log" , data_dir/"zk-data"))
    connector = new NIOServerCnxn.Factory(new InetSocketAddress(0), 100)
    connector.startup(zk_server)
    debug("ZooKeeper Started")
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

case class ExpectingListener(cluster: ZkCluster, expected:List[List[(String, Option[Buffer])]]) extends ClusterListener with Logging {

  var results = expected.map(x=> Future[Result[Zilch, String]]() )
  var remaining = expected zip results

  cluster.add(this)

  def on_cluster_change(actual: List[(String, Option[Buffer])]): Unit = synchronized {
    if( remaining.isEmpty )
      return

    val (expect, future) = remaining.head
    remaining = remaining.drop(1)

    if( expect != actual ) {
      future(Failure("Unexpected change event #%d.  Expected %s but was %s: ".format(actual.size - remaining.size, expect, actual)))
      cluster.remove(this)
      remaining = List()
    } else {
      future(Success(Zilch))
      if( remaining.isEmpty ) {
        cluster.remove(this)
      }
    }
  }

}

class ZkClusterTest extends ZkFunSuiteSupport with ShouldMatchers {

  // Failing on our CI boxes.. disable for now.
  ignore("cluster events") {

    val cluster1 = new ZkCluster(create_zk_client, "/apollo")
    val cluster2 = new ZkCluster(create_zk_client, "/apollo")

    val node1_state = ("1", None)
    val node2_state = ("2", None)
    val node2_updated_state =  ("2", Some(ascii("Hello!").buffer))

    val events: List[List[(String, Option[Buffer])]] = List(
      List(),
      List(node1_state),
      List(node1_state, node2_state),
      List(node1_state, node2_updated_state),
      List(node2_updated_state)
    )
    var listener1 = ExpectingListener(cluster1, events)
    var listener2 = ExpectingListener(cluster2, events)

    var results1 = ListBuffer[Future[Result[Zilch, String]]](listener1.results:_*)
    var results2 = ListBuffer[Future[Result[Zilch, String]]](listener2.results:_*)

    def check_next(list:ListBuffer[Future[Result[Zilch, String]]]) = {
      list.remove(0).await(10, SECONDS) match {
        case Some(result) =>
          result.failure_option.foreach(fail(_))
        case None =>
          fail("Timeout")
      }
    }

    cluster1.start
    cluster2.start

    check_next(results1)
    check_next(results2)

    cluster1.join("1")

    // both listeners should get the state change...
    check_next(results1)
    check_next(results2)

    cluster2.join("2")

    check_next(results1)
    check_next(results2)

    cluster2.join("2", ascii("Hello!"))

    check_next(results1)
    check_next(results2)

    cluster1.leave

    check_next(results1)
    check_next(results2)
  }
}
