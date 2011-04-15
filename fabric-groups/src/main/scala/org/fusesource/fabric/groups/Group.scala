/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.groups

import scala.reflect.BeanProperty
import org.linkedin.zookeeper.client.IZKClient
import org.fusesource.fusemq.cluster.ZooKeeperGroup
/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object GroupFactory {
  def create(zk: IZKClient, path: String):Group = new ZooKeeperGroup(zk, path)
  def members(zk: IZKClient, path: String):Array[Member] = ZooKeeperGroup.members(zk, path)
}

/**
 * <p>
 *   Used the join a cluster group and to monitor the memberships
 *   of that group.
 * </p>
 * <p>
 *   This object is not thread safe.  You should are responsible for
 *   synchronizing access to it across threads.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
trait Group {

  /**
   * Adds a member to the group with some associated data.  Both the
   * id and the data will be visible to anyone listing the members of the
   * group.
   *
   * If you join
   */
  def join(id:String, data:Array[Byte]):Unit

  /**
   * Removes a previously added member.
   */
  def leave(id:String):Unit

  /**
   * Lists all the members currently in the group.
   */
  def members():Array[Member]

  /**
   * Registers a change listener which will be called
   * when the cluster membership changes.
   */
  def add(listener:ChangeListener)

  /**
   * Removes a previously added change listener.
   */
  def remove(listener:ChangeListener)

  /**
   * A group should be closed to release aquired resources used
   * to monitor the group membership.
   *
   * Whe the Group is closed, any memberships registered via this
   * Group will be removed from the group.
   */
  def close:Unit

}

/**
 * <p>
 *   Callback interface used to get notifications of changes
 *   to a cluster group.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
trait ChangeListener {

  /**
   * Processing this change even is not allowed to block for very long.
   */
  def changed(members:Array[Member]):Unit
}

/**
 * <p>
 *   Represents a member in the cluster group.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
case class Member(
   @BeanProperty
   id:String,
   @BeanProperty
   data:Array[Byte]
)