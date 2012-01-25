/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
