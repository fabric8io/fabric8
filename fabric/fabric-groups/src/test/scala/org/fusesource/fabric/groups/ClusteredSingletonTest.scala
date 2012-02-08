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
package org.fusesource.fabric.groups

import org.scalatest.matchers.ShouldMatchers
import java.util.concurrent.TimeUnit._
import org.codehaus.jackson.annotate.JsonProperty

class AddressNodeState extends NodeState {

  @JsonProperty
  var id:String = _

  @JsonProperty
  var address:String = _

  override
  def toString = new String(ClusteredSupport.encode(this), "UTF-8")

}

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ClusteredSingletonTest extends ZooKeeperFunSuiteSupport with ShouldMatchers {

  class AddressSingleton(id:String, var address:String) extends ClusteredSingleton[AddressNodeState](classOf[AddressNodeState]) {
    def join:Unit= join(create_state)
    def update:Unit = update(create_state)

    def create_state = {
      val rc = new AddressNodeState
      rc.id = id
      rc.address = address
      rc
    }
  }


  test("cluster events") {

    val node1_group = ZooKeeperGroupFactory.create(create_zk_client, "/example")
    val singleton1 = new AddressSingleton("node", "localhost:80")
    val node2_group = ZooKeeperGroupFactory.create(create_zk_client, "/example")
    val singleton2 = new AddressSingleton("node", "localhost:81")

    // First node in becomes the master.
    singleton1.start(node1_group)
    singleton1.join
    within(600, SECONDS) {
      expect(true)(singleton1.isMaster)
      expect(1)(singleton1.members.get("node").get.size)
    }

    // 2nd node in becomes a slave.
    singleton2.start(node2_group)
    singleton2.join
    within(10, SECONDS) {
      // They can never both be masters..
      breaks_within { expect(false)(singleton1.isMaster && singleton2.isMaster) }

      expect(true)(singleton1.isMaster)
      expect(false)(singleton2.isMaster)
      expect(2)(singleton2.members.get("node").get.size)
    }

    // Both should have the same view of the cluster now.
    expect(singleton1.members.get("node").map(_.map(_._2.address)))(singleton2.members.get("node").map(_.map(_._2.address)))

    // Members are kept in an ordered list of when they joined.
    expect(List("localhost:80", "localhost:81"))(singleton1.members.get("node").get.toList.map(_._2.address))

    // Check updating member data...
    singleton1.address = "localhost:82"
    singleton1.update
    within(2, SECONDS) {
      expect(List("localhost:82", "localhost:81"))(singleton1.members.get("node").get.toList.map(_._2.address))
    }

    // Check leaving the cluster
    singleton1.leave
    within(10, SECONDS) {
      // They can never both be masters..
      breaks_within { expect(false)(singleton1.isMaster && singleton2.isMaster) }

      expect(false)(singleton1.isMaster)
      expect(true)(singleton2.isMaster)
    }

  }


}