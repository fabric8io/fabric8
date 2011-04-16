/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fusemq.cluster

import org.apache.zookeeper._
import java.lang.String
import collection.JavaConversions._
import org.linkedin.zookeeper.client.IZKClient
import org.linkedin.zookeeper.tracker._
import org.apache.zookeeper.KeeperException.Code
import org.fusesource.fabric.groups.{ChangeListener, Group}
import scala.collection.mutable.HashMap
import org.apache.zookeeper.data.ACL
import java.util.Collection

/**
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object ZooKeeperGroup {
  def members(zk: IZKClient, path: String):Array[Array[Byte]] = {
    zk.getAllChildren(path).sortWith((a,b)=> a < b).flatMap { node =>
      val rc:Option[Array[Byte]] = try {
        Option(zk.getData(path+"/"+node))
      } catch {
        case e:Throwable =>
          e.printStackTrace
          None
      }
      rc
    }.toArray

  }


}

/**
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ZooKeeperGroup(val zk: IZKClient, val root: String, val acl:java.util.List[ACL]) extends Group {

  val tree = new ZooKeeperTreeTracker[Array[Byte]](zk, new ZKByteArrayDataReader, root, 1)
  val joins = HashMap[String, (String,Int)]()

  @volatile
  var members = Array[Array[Byte]]()
  @volatile
  var listeners = List[ChangeListener]()

  private def member_path_prefix = root + "/0"

  create(root)
  tree.track(new NodeEventsListener[Array[Byte]]() {
    def onEvents(events: Collection[NodeEvent[Array[Byte]]]): Unit = {
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

    def action:Unit = {
      joins.get(id) match {
        case None =>
          try {
            val my_path = zk.create(member_path_prefix, data, acl, CreateMode.EPHEMERAL_SEQUENTIAL)
            joins.put(id, (my_path, 0))
          } catch {
            case e: KeeperException  =>
              e.code match {
                case Code.NONODE =>
                  // crate and try again..
                  create(root)
                  action
                case _ =>
                  e.printStackTrace
              }
          }

        case Some((my_path, my_path_version)) =>
          try {
            val stat = zk.setData(my_path, data, my_path_version)
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
    val t = tree.getTree.toList.filterNot( _._1 == root)
    members = t.sortWith((a,b)=> a._1 < b._1 ).map(_._2).map { x=>
      x.getData
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
        val p = zk.create(cur, new Array[Byte](0), acl, CreateMode.PERSISTENT)
      } catch {
        case ignore: KeeperException.NodeExistsException =>
      }
    }
  }

}