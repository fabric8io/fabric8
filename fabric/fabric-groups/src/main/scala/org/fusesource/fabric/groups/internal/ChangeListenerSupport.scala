/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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
        println("WARN: listerns are taking too long to process the events")
      }
    }
  }
  
}