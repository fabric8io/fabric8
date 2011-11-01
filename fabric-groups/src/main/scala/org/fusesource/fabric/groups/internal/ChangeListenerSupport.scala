package org.fusesource.fabric.groups.internal

import org.fusesource.fabric.groups.ChangeListener

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
    for (listener <- listeners) {
      listener.connected
    }
  }

  def fireDisconnected() = {
    val listener = this.synchronized { this.listeners }
    for (listener <- listeners) {
      listener.disconnected
    }
  }

  def fireChanged() = {
    val listener = this.synchronized { this.listeners }
    for (listener <- listeners) {
      listener.changed
    }
  }

}