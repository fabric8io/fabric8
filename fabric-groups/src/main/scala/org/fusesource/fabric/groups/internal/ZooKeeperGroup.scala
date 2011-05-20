/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.groups.internal

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
        if( node.matches("""0\d+""") ) {
          Option(zk.getData(path+"/"+node))
        } else {
          None
        }
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
    val t = tree.getTree.toList.filterNot { x =>
      // don't include the root node, or nodes that don't match our naming convention.
      (x._1 == root) || !x._1.stripPrefix(root).matches("""/0\d+""")
    }
    members = t.sortWith((a,b)=> a._1 < b._1 ).map(_._2).map { x=>
      x.getData
    }.toArray
    for (listener <- listeners) {
      listener.changed(members)
    }
  }

  private def create(path: String, count : java.lang.Integer = 0): Unit = {
    try {
      if (zk.exists(path, false) != null) {
        return
      }
      try {
        // try create given path in persistent mode
        zk.createOrSetWithParents(path, "", acl, CreateMode.PERSISTENT)
      } catch {
        case ignore: KeeperException.NodeExistsException =>
      }
    } catch {
      case ignore : KeeperException.SessionExpiredException => {
        if (count > 20) {
          // we tried enought number of times
          throw new IllegalStateException("Cannot create path " + path, ignore)
        }
        // try to create path with increased counter value
        create(path, count + 1)
      }
    }
  }

}