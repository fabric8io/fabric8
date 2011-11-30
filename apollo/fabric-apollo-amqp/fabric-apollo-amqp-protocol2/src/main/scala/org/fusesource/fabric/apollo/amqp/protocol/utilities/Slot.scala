/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.utilities

import collection.immutable.TreeSet
import collection.mutable.HashMap

/**
 * Simple utility class that maintains a list of free IDs in
 * sequential order, allowing free IDs to be re-used as they
 * become available
 */
class Slot[B] {
  private var highest_value: Int = 0
  private var available = new TreeSet[Int]
  private val used = new HashMap[Int, B]
  private var reserved = new TreeSet[Int]

  def reserve(min:Int) = {
    0.to(min).foreach((x) => {
      if (x != min) {
        reserved = reserved + (x)
      }
    })
    highest_value = min
  }

  def allocate(obj: B): Int = {
    if ( !available.isEmpty ) {
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

  def foreach(func:(B) => Unit) = used.values.foreach((x) => func(x))

  def is_reserved(key:Int) = reserved.contains(key)

  def get(key: Int): Option[B] = {
    if (reserved.contains(key)) {
      throw new IllegalArgumentException("Key " + key + " has been reserved")
    }
    used.get(key)
  }

  def free(key: Int): Option[B] = {
    val rc = used.remove(key)
    rc.foreach((x) => available = available + key)
    rc
  }

  def used_slots() = used.keySet

  def available_slots() = available.asInstanceOf[Set[Int]]

  override def toString = "available : " + available + " used : " + used
}
