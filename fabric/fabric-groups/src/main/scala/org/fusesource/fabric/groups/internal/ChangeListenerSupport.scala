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

import org.fusesource.fabric.groups.ChangeListener
import java.util.concurrent.TimeUnit

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
trait ChangeListenerSupport {

  var listeners = List[ChangeListener]()

  def connected:Boolean

  def add(listener: ChangeListener): Unit = {
    val connected = this.synchronized {
      listeners ::= listener
      this.connected
    }
    if (connected) {
      listener.connected
    }
  }

  def remove(listener: ChangeListener): Unit = this.synchronized {
    listeners = listeners.filterNot(_ == listener)
  }

  def fireConnected() = {
    val listener = this.synchronized { this.listeners }
    check_elapsed_time {
      for (listener <- listeners) {
        listener.connected
      }
    }
  }

  def fireDisconnected() = {
    val listener = this.synchronized { this.listeners }
    check_elapsed_time {
      for (listener <- listeners) {
        listener.disconnected
      }
    }
  }

  def fireChanged() = {
    val listener = this.synchronized { this.listeners }
    val start = System.nanoTime()
    check_elapsed_time {
      for (listener <- listeners) {
        listener.changed
      }
    }
  }

  def check_elapsed_time[T](func: => T):T = {
    val start = System.nanoTime()
    try {
      func
    } finally {
      val end = System.nanoTime()
      val elapsed = TimeUnit.NANOSECONDS.toMillis(end-start)
      if( elapsed > 100 ) {
        println("WARN: listeners are taking too long to process the events")
      }
    }
  }
  
}