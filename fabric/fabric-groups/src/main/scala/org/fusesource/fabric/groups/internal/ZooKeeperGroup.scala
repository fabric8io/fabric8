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

package org.fusesource.fabric.groups.internal

import org.apache.zookeeper._
import java.lang.String
import org.fusesource.fabric.groups.Group
import scala.collection.mutable.HashMap
import collection.JavaConversions._
import java.util.LinkedHashMap
import org.apache.zookeeper.KeeperException.{ConnectionLossException, NoNodeException}
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.{ChildData, PathChildrenCacheEvent, PathChildrenCacheListener, TreeCache}
import org.apache.curator.framework.state.{ConnectionState, ConnectionStateListener}

/**
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object ZooKeeperGroup {
  def members(curator: CuratorFramework, path: String):LinkedHashMap[String, Array[Byte]] = {
    var rc = new LinkedHashMap[String, Array[Byte]]
    ZooKeeperUtils.getChildren(curator, path).sortWith((a,b)=> a < b).foreach { node =>
      try {
        if( node.matches("""0\d+""") ) {
          rc.put(node, curator.getData().forPath(path+"/"+node))
        } else {
          None
        }
      } catch {
        case e:Throwable =>
          e.printStackTrace
      }
    }
    rc

  }


}

/**
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ZooKeeperGroup(val curator: CuratorFramework, val root: String) extends Group with ConnectionStateListener with PathChildrenCacheListener with ChangeListenerSupport {

  //val tree = new ZooKeeperTreeTracker[Array[Byte]](curator, new ZKByteArrayDataReader, root, 1)
  val tree = new TreeCache(curator, root, true, true)
  val joins = HashMap[String, Int]()

  var members = new LinkedHashMap[String, Array[Byte]]

  private def member_path_prefix = root + "/0"

  curator.getConnectionStateListenable.addListener(this)
  tree.getListenable.addListener(this)
  tree.start(TreeCache.StartMode.BUILD_INITIAL_CACHE)

  create(root)
  fire_cluster_change

  def childEvent(framework : CuratorFramework, event : PathChildrenCacheEvent): Unit = {
    fire_cluster_change
  }

  def close = this.synchronized {
    joins.foreach { case (path, version) =>
      try {
        curator.delete().withVersion(version).forPath(member_path_prefix + path)
      } catch {
        case x:NoNodeException => // Already deleted.
      }
    }
    joins.clear
    tree.getListenable.removeListener(this)
    tree.close
    curator.getConnectionStateListenable.removeListener(this)
  }

  def stateChanged(client :CuratorFramework, newState: ConnectionState) {
     newState match {
       case ConnectionState.CONNECTED => onConnected
       case ConnectionState.RECONNECTED => onConnected
       case _ => onDisconnected
    }
  }

  def connected = curator.getZookeeperClient.isConnected
  def onConnected() = fireConnected()
  def onDisconnected() = fireDisconnected()

  def join(data:Array[Byte]=null): String = this.synchronized {
    val id = ZooKeeperUtils.create(curator, member_path_prefix, data, CreateMode.EPHEMERAL_SEQUENTIAL).stripPrefix(member_path_prefix)
    joins.put(id, 0)
    id
  }

  def update(path:String, data:Array[Byte]=null): Unit = this.synchronized {
    joins.get(path) match {
      case Some(ver) =>  {
          val stat = curator.setData().withVersion(ver).forPath(member_path_prefix + path, data)
          joins.put(path, stat.getVersion)
      }
      case None => throw new IllegalArgumentException("Has not joined locally: "+path)
    }
  }

  def leave(path:String): Unit = this.synchronized {
    joins.remove(path).foreach {
      case version =>
          try {
            curator.delete().withVersion(version).forPath(member_path_prefix + path)
          } catch {
            case x: NoNodeException => // Already deleted.
            case x: ConnectionLossException => // disconnected
          }
    }
  }

  private def fire_cluster_change: Unit = {
    this.synchronized {
      val t = tree.getCurrentData.filterNot { x:ChildData =>
        x.getPath == root || !x.getPath.stripPrefix(root).matches("""/0\d+""")
      }

      this.members = new LinkedHashMap()
      t.sortWith((a,b)=> a.getPath < b.getPath ).foreach { x=>
        this.members.put(x.getPath.stripPrefix(member_path_prefix), x.getData)
      }
    }
    fireChanged()
  }

  private def create(path: String, count : java.lang.Integer = 0): Unit = {
    try {
      if (curator.checkExists().forPath(path) != null) {
        return
      }
      try {
        // try create given path in persistent mode
        ZooKeeperUtils.setData(curator, path, "")
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