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

import org.scalatest.BeforeAndAfterEach
import org.apache.zookeeper.server.{ZooKeeperServer, NIOServerCnxnFactory}
import org.apache.zookeeper.server.persistence.FileTxnSnapLog
import org.apache.activemq.apollo.util._
import FileSupport._
import org.linkedin.zookeeper.client.ZKClient
import org.linkedin.util.clock.Timespan
import java.net.InetSocketAddress
import scala.collection.immutable.List
/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ZkFunSuiteSupport extends FunSuiteSupport with BeforeAndAfterEach {

  var connector : NIOServerCnxnFactory = _

  override protected def beforeAll() = {
    debug("Starting ZooKeeper")
    val zk_server = new ZooKeeperServer();
    val data_dir = basedir/"target"/"test-data"
    data_dir.recursive_delete

    zk_server.setTxnLogFactory(new FileTxnSnapLog(data_dir/"zk-log" , data_dir/"zk-data"))
    connector = new NIOServerCnxnFactory
    connector.configure(new InetSocketAddress(0), 100)
    connector.startup(zk_server)

    create_zk_client().close()
    debug("ZooKeeper Started")
  }

  override protected def afterAll() = {
    if( connector!=null ) {
      connector.shutdown
      connector = null
    }
  }

  var zk_clients = List[ZKClient]()

  def zk_url = "localhost:" + connector.getLocalPort

  def create_zk_client() = {
    val client = new ZKClient(zk_url, Timespan.parse("30s"), null)
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
