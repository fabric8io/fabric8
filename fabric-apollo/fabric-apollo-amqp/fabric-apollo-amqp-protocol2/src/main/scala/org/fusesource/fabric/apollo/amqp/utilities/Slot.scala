package org.fusesource.fabric.apollo.amqp.utilities

import collection.immutable.TreeSet
import collection.mutable.HashMap

/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

/**
 * Simple utility class that maintains a list of free IDs in
 * sequential order, allowing free IDs to be re-used as they
 * become available
 */
class Slot[B] {
  private var highest_value:Int = 0
  private var available = new TreeSet[Int]
  private val used = new HashMap[Int, B]

  def allocate(obj:B):Int = {
    if (!available.isEmpty) {
      val rc = available.head
      used.put(rc, obj)
      available = available - rc
      rc
    } else {
      while (used.keySet.contains(highest_value)) {
        highest_value = highest_value + 1
      }
      val rc = highest_value
      used.put(rc, obj)
      rc
    }
  }

  def get(key:Int):Option[B] = used.get(key)

  def free(key:Int):Option[B] = {
    val rc = used.remove(key)
    rc.foreach((x) => available = available + key)
    rc
  }

  def used_slots() = used.keySet
  def available_slots() = available.asInstanceOf[Set[Int]]

  override def toString = "available : " + available + " used : " + used
}
