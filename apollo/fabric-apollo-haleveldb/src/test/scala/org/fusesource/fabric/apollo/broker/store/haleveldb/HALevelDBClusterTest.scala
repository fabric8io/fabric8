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

import java.util.concurrent.TimeUnit._
import org.apache.activemq.apollo.util.SocketProxy
import org.fusesource.fabric.apollo.cluster.ClusterTestSupport

class HALevelDBClusterTest extends ClusterTestSupport {

  test("Standby haleveldb broker.") {

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

//    // simulate a disconnect of the master from zk
//    var start = System.currentTimeMillis()
//    proxy.suspend()
//
//    // Default session timeout is set to 5 seconds..
//    within(10, SECONDS) {
//      val a_master = access(cluster_a){ cluster_a.master }
//      val b_master = access(cluster_b){ cluster_b.master }
//      if (a_master && b_master) {
//        throw new ShortCircuitFailure("a and b cannot be master at the same time.")
//      }
//      a_master should be === false
//    }
//    var end = System.currentTimeMillis()
//    System.out.println("Master shutdown in (ms): "+(end-start))
//
//    start = System.currentTimeMillis()
//    within(10, SECONDS) ( access(cluster_b){ cluster_b.master } should be === true )
//    end = System.currentTimeMillis()
//    System.out.println("Slave startup in (ms): "+(end-start))

  }


}
