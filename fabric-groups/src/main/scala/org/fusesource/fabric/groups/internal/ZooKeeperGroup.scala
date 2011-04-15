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

import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper._
import java.lang.String
import collection.JavaConversions._
import org.linkedin.zookeeper.client.{ZKData, IZKClient}
import org.linkedin.zookeeper.tracker._
import org.apache.zookeeper.KeeperException.Code
import org.fusesource.hawtbuf.Buffer
import org.fusesource.fabric.groups.internal.GroupData
import org.fusesource.fabric.groups.{Member, ChangeListener, Group}
import scala.collection.mutable.HashMap
import org.apache.zookeeper.data.ACL
import java.util.{ArrayList, Collection}

/**
 *
 * @author chirino
 * @org.apache.xbean.XBean element="zookeeperCluster"
 */
object ZooKeeperGroup {

  implicit def encode_group_data(value:GroupData.Buffer):Array[Byte] = value.toFramedByteArray
  implicit def decode_group_data(value:Array[Byte]):GroupData.Buffer = GroupData.FACTORY.parseFramed(value)


  object GroupDataReader extends ZKDataReader[GroupData.Buffer] {
    def readData(zkClient: IZKClient, path: String, watcher: Watcher): ZKData[GroupData.Buffer] = {
      val data = zkClient.getZKByteData(path, watcher)
      if( data.getData.isEmpty ) {
        new ZKData(null, data.getStat())
      } else {
        new ZKData(decode_group_data(data.getData), data.getStat());
      }
    }

    def isEqual(data1: GroupData.Buffer, data2: GroupData.Buffer): Boolean = {
      data1 == data2
    }
  }

  def members(zk: IZKClient, path: String):Array[Member] = {
    zk.getAllChildren(path).sortWith((a,b)=> a < b).flatMap { node =>
      val rc:Option[Member] = try {
        val group_data = decode_group_data(zk.getData(path+"/"+node))
        Some(Member(group_data.getId, Option(group_data.getData).map(_.toByteArray).getOrElse(null)))
      } catch {
        case e:Throwable =>
          e.printStackTrace
          None
      }
      rc
    }.toArray

  }


}

class ZooKeeperGroup(val zk: IZKClient, base_path: String) extends Group {
  import ZooKeeperGroup._
  
  var acl_list: ArrayList[ACL] = Ids.OPEN_ACL_UNSAFE
  val tree = new ZooKeeperTreeTracker[GroupData.Buffer](zk, GroupDataReader, base_path)
  val joins = HashMap[String, (String,Int)]()

  @volatile
  var members = Array[Member]()
  @volatile
  var listeners = List[ChangeListener]()

  private def member_path_prefix = base_path + "/0"

  create(base_path)
  tree.track(new NodeEventsListener[GroupData.Buffer]() {
    def onEvents(events: Collection[NodeEvent[GroupData.Buffer]]): Unit = {
      fire_cluster_change
    }
  })
  fire_cluster_change

  def close = {
    joins.values.foreach { case (my_path, my_path_version) =>
      zk.delete(my_path, my_path_version)
    }
    joins.clear
    // I wish the tree tracker could be stopped...
  }

  def add(listener: ChangeListener): Unit = {
    listeners ::= listener
    listener.changed(members)
  }

  def remove(listener: ChangeListener): Unit = {
    listeners = listeners.filterNot(_ == listener)
  }

  def leave(id:String): Unit = {
    joins.remove(id).foreach { case (my_path,my_path_version) =>
      zk.delete(my_path, my_path_version)
    }
  }

  def join(id:String, data:Array[Byte]=null): Unit = {

    val t = new GroupData.Bean
    t.setId(id)
    if( data!=null ) {
      t.setData(new Buffer(data))
    }
    val state = t.freeze

    def action:Unit = {
      joins.get(id) match {
        case None =>
          try {
            val my_path = zk.create(member_path_prefix, state, acl_list, CreateMode.EPHEMERAL_SEQUENTIAL)
            joins.put(id, (my_path, 0))
          } catch {
            case e: KeeperException  =>
              e.code match {
                case Code.NONODE =>
                  // crate and try again..
                  create(base_path)
                  action
                case _ =>
                  e.printStackTrace
              }
          }

        case Some((my_path, my_path_version)) =>
          try {
            val stat = zk.setData(my_path, state, my_path_version)
            joins.put(id, (my_path, stat.getVersion))
          } catch {
            case e: KeeperException  =>
              e.code match {
                case Code.NONODE =>
                  // Looks like someone remove our node..  retry
                  // next time we will try to create it.
                  joins.remove(id)
                  action
                case _ =>
                  e.printStackTrace
              }
          }
      }
    }
    action
  }

  private def fire_cluster_change: Unit = {
    members = tree.getTree.toList.sortWith((a,b)=> a._1 < b._1 ).map(_._2).flatMap { x=>
      Option(x.getData).map(x=> Member(x.getId, Option(x.getData).map(_.toByteArray).getOrElse(null)))
    }.toArray
    for (listener <- listeners) {
      listener.changed(members)
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
        val p = zk.create(cur, new Array[Byte](0), acl_list, CreateMode.PERSISTENT)
      } catch {
        case ignore: KeeperException.NodeExistsException =>
      }
    }
  }

}