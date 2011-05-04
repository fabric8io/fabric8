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

import org.apache.zookeeper.ZooDefs.Ids
import scala.collection.mutable.ListBuffer
import org.fusesource.hawtdispatch._
import org.apache.zookeeper._
import java.lang.String
import collection.JavaConversions._
import org.apache.activemq.apollo.util.{Log, BaseService}
import org.linkedin.zookeeper.client.{ZKData, IZKClient}
import org.apache.activemq.apollo.broker.Broker
import org.linkedin.zookeeper.tracker._
import org.apache.zookeeper.KeeperException.Code
import java.util.Collection
import org.fusesource.fabric.apollo.cluster.model.MemberRecord
import org.fusesource.hawtbuf.Buffer

trait ClusterListener {
  def on_cluster_change(members:List[(String, Option[Buffer])]):Unit
}

/**
 *
 * @author chirino
 * @org.apache.xbean.XBean element="zookeeperCluster"
 */
object ZkCluster extends Log {

  implicit def encode_member_record(value:MemberRecord.Buffer):Array[Byte] = value.toFramedByteArray
  implicit def decode_member_record(value:Array[Byte]):MemberRecord.Buffer = MemberRecord.FACTORY.parseFramed(value)


  object MemberRecordReader extends ZKDataReader[MemberRecord.Buffer] {
    def readData(zkClient: IZKClient, path: String, watcher: Watcher): ZKData[MemberRecord.Buffer] = {
      val data = zkClient.getZKByteData(path, watcher)
      if( data.getData.isEmpty ) {
        new ZKData(null, data.getStat())
      } else {
        new ZKData(decode_member_record(data.getData), data.getStat());
      }
    }

    def isEqual(data1: MemberRecord.Buffer, data2: MemberRecord.Buffer): Boolean = {
      data1 == data2
    }
  }

}

class ZkCluster(val zk: IZKClient, val base_path: String) extends BaseService {
  import ZkCluster._
  
  val dispatch_queue = createQueue("cluster state manager")

  var tree:ZooKeeperTreeTracker[MemberRecord.Buffer] = _
  var members = List[(String, Option[Buffer])]()
  var listeners = ListBuffer[ClusterListener]()

  @volatile
  var my_state: MemberRecord.Buffer = null
  @volatile
  var my_path: String = null
  @volatile
  var my_path_version = 0

  private def member_path_prefix = base_path + "/0"

  protected def _start(on_completed: Runnable) = dispatch_queue {
    Broker.BLOCKABLE_THREAD_POOL {
      create(base_path)
      tree = new ZooKeeperTreeTracker(zk, MemberRecordReader, base_path)
      tree.track(new NodeEventsListener[MemberRecord.Buffer]() {
        def onEvents(events: Collection[NodeEvent[MemberRecord.Buffer]]): Unit = {
          fire_cluster_change
        }
      })
      fire_cluster_change
      on_completed.run
    }
  }

  protected def _stop(on_completed: Runnable) = {
    // I wish the tree tracker could be stopped...
    on_completed.run
  }

  def add(listener: ClusterListener): Unit = dispatch_queue {
    listeners += listener
    listener.on_cluster_change(members)
  }

  def remove(listener: ClusterListener): Unit = dispatch_queue {
    listeners -= (listener)
  }

  def leave(): Unit = dispatch_queue {
    def action:Unit = {
      if (my_path != null) {
        zk.delete(my_path, my_path_version)
        my_path = null
        my_path_version = 0
      }
    }
    Broker.BLOCKABLE_THREAD_POOL {
      action
    }
  }

  def join(id:String, data:Buffer=null): Unit = dispatch_queue {

    val t = new MemberRecord.Bean
    t.setId(id)
    if( data!=null ) {
      t.setData(data)
    }
    val state = t.freeze
    this.my_state = state

    def action:Unit = {
      if (my_path == null) {
        try {
          my_path = zk.create(member_path_prefix, state, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL)
          debug("joined cluster as zookeeper node: "+my_path)
        } catch {
          case e: KeeperException  =>
            e.code match {
              case Code.NONODE =>
                // crate and try again..
                create(base_path)
                Broker.BLOCKABLE_THREAD_POOL {
                  action
                }
              case _ =>
                e.printStackTrace
            }
        }
      } else {
        try {
          debug("updating cluster state of zookeeper node: "+my_path)
          val stat = zk.setData(my_path, state, my_path_version)
          my_path_version = stat.getVersion
        } catch {
          case e: KeeperException  =>
            e.code match {
              case Code.NONODE =>
                // Looks like someone remove our node..  retry
                // next time we will try to create it.
                my_path = null
                Broker.BLOCKABLE_THREAD_POOL {
                  action
                }
              case _ =>
                e.printStackTrace
            }
        }
      }
    }
    Broker.BLOCKABLE_THREAD_POOL {
      action
    }
  }

  private def fire_cluster_change: Unit = dispatch_queue {

    val members = tree.getTree.toList.sortWith((a,b)=> a._1 < b._1 ).map(_._2).flatMap { x=>
      Option(x.getData).map(x=> (x.getId, Option(x.getData)))
    }.toList

    if( members!=this.members ) {
      this.members = members
      for (listener <- listeners) {
        listener.on_cluster_change(members)
      }
    } else {
      debug("no change")
    }
  }

  private def create(path: String): Unit = {
    if (zk.exists(path, false) != null) {
      return
    }
    var cur: String = ""
    for (node <- path.stripPrefix("/").split("/")) {
      cur += "/" + node
      try {
        val p = zk.create(cur, new Array[Byte](0), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
      } catch {
        case ignore: KeeperException.NodeExistsException =>
      }
    }
  }

}
