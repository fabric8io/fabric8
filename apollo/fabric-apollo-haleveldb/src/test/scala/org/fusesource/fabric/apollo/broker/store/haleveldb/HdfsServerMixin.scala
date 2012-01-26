/**
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

package org.fusesource.fabric.apollo.broker.store.haleveldb

import org.scalatest.{Suite, BeforeAndAfterAll}
import org.apache.activemq.apollo.util._
import org.apache.hadoop.hdfs.MiniDFSCluster
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem

/**
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
trait HdfsServerMixin extends FunSuiteSupport with BeforeAndAfterAll with Logging {
this: Suite =>

  override protected def beforeAll(configMap: Map[String, Any]): Unit = {
    start_hdfs
    super.beforeAll(configMap)
  }

  override protected def afterAll(configMap: Map[String, Any]) = {
    super.afterAll(configMap)
    stop_hdfs
  }

  var cluster: MiniDFSCluster = _
  var fs:FileSystem = _

  private def start_hdfs = {

    val conf = new Configuration();
//    conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, 100);
//    conf.setInt(DFSConfigKeys.DFS_BYTES_PER_CHECKSUM_KEY, 1);
//    conf.setLong(DFSConfigKeys.DFS_HEARTBEAT_INTERVAL_KEY, 1);
//    conf.setInt(DFSConfigKeys.DFS_NAMENODE_REPLICATION_INTERVAL_KEY, 1);

    cluster = new MiniDFSCluster(conf, 1, true, null);
    cluster.waitActive();
    fs = cluster.getFileSystem()
  }

  private def stop_hdfs = {
    try {
      cluster.shutdown()
    } catch {
      case e:Throwable => info(e, "hdfs shutdown failed: "+e.getMessage)
    }
  }

}

