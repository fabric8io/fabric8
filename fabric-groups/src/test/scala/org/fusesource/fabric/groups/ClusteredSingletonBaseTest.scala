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
  def toString = new String(ClusteredSingletonBase.encode(this), "UTF-8")

}

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ClusteredSingletonBaseTest extends ZooKeeperFunSuiteSupport with ShouldMatchers {

  class AddressSingleton(val id:String, var address:String) extends ClusteredSingletonBase[AddressNodeState] {

    protected def nodeStateClass = classOf[AddressNodeState]

    def state = {
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
    singleton1.join(node1_group)
    within(600, SECONDS) {
      expect(true)(singleton1.active)
      expect(1)(singleton1.members.get("node").get.size)
    }

    // 2nd node in becomes a slave.
    singleton2.join(node2_group)
    within(10, SECONDS) {
      // They can never both be masters..
      breaks_within { expect(false)(singleton1.active && singleton2.active) }

      expect(true)(singleton1.active)
      expect(false)(singleton2.active)
      expect(2)(singleton2.members.get("node").get.size)
    }

    // Both should have the same view of the cluster now.
    expect(singleton1.members.get("node").map(_.map(_._2.address)))(singleton2.members.get("node").map(_.map(_._2.address)))

    // Members are kept in an ordered list of when they joined.
    expect(List("localhost:80", "localhost:81"))(singleton1.members.get("node").get.toList.map(_._2.address))

    // Check updating member data...
    singleton1.address = "localhost:82"
    singleton1.sendUpdate
    within(2, SECONDS) {
      expect(List("localhost:82", "localhost:81"))(singleton1.members.get("node").get.toList.map(_._2.address))
    }

    // Check leaving the cluster
    singleton1.leave
    within(10, SECONDS) {
      // They can never both be masters..
      breaks_within { expect(false)(singleton1.active && singleton2.active) }

      expect(false)(singleton1.active)
      expect(true)(singleton2.active)
    }

  }


}