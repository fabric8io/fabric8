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


import collection.mutable.{ListBuffer, HashMap}
import internal.ChangeListenerSupport

import java.io._
import org.codehaus.jackson.map.ObjectMapper
import collection.JavaConversions._
import java.util.LinkedHashMap
import java.lang.{IllegalStateException, String}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
trait NodeState {

  /**
   * The id of the cluster node.  There can be multiple node with this ID,
   * but only the first node in the cluster will be the master for for it.
   */
  def id: String
}

/**
 *
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object ClusteredSingletonBase {

  private var mapper: ObjectMapper = new ObjectMapper

  def decode[T](t : Class[T], buffer: Array[Byte]): T = decode(t, new ByteArrayInputStream(buffer))
  def decode[T](t : Class[T], in: InputStream): T =  mapper.readValue(in, t)

  def encode(value: AnyRef): Array[Byte] = {
    var baos: ByteArrayOutputStream = new ByteArrayOutputStream
    encode(value, baos)
    return baos.toByteArray
  }

  def encode(value: AnyRef, out: OutputStream): Unit = {
    mapper.writeValue(out, value)
  }

}

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
abstract class ClusteredSingletonBase[T <: NodeState] extends ChangeListenerSupport {
  import ClusteredSingletonBase._
  
  private var _group:Group = _
  private var _eid:String = _

  /** the logical id of the singleton, multiple nodes in the same group using the same id have active/passive relationship  */
  def id:String
  /** the ephemeral id of the node is unique within in the group */
  def eid = _eid
  def group = _group

  protected def nodeStateClass:Class[T]

  private val listener = new ChangeListener() {
    def changed() {
      val members = _group.members
      val t = new LinkedHashMap[String, T]()
      members.foreach {
        case (path, data) =>
          try {
            val value = decode(nodeStateClass, data)
            t.put(path, value)
          } catch {
            case e: Throwable =>
              e.printStackTrace()
          }
      }
      changed_decoded(t)
    }

    def connected = ClusteredSingletonBase.this.fireConnected
    def disconnected = ClusteredSingletonBase.this.fireDisconnected
  }

  def join(group:Group) = this.synchronized {
    if(_group !=null )
      throw new IllegalStateException("Singleton has alerady joined a group.")
    _group = group
    _eid = group.join(encode(state))
    group.add(listener)
  }

  def connected = this.synchronized {
    if(_group==null) {
      false
    } else {
      _group.connected
    }
  }

  def sendUpdate = this.synchronized {
    if(_group==null)
      throw new IllegalStateException("Singleton has not joined a group.")
    _group.update(_eid, encode(state))
  }
  
  def leave = {
    this.synchronized {
      if(_group==null)
        throw new IllegalStateException("Singleton has not joined a group.")
      _group.remove(listener)
      _group.leave(_eid)
      _members = HashMap[String, ListBuffer[(String,  T)]]()
      _group = null
    }
    fireChanged
  }
  
  def state:T

  private var _members = HashMap[String, ListBuffer[(String,  T)]]()
  def members = this.synchronized { _members }

  def changed_decoded(m: LinkedHashMap[String, T]) = {
    this.synchronized {
      if( _group!=null ) {
        _members = HashMap[String, ListBuffer[(String,  T)]]()
        m.foreach { case node =>
          _members.getOrElseUpdate(node._2.id, ListBuffer[(String,  T)]()).append(node)
        }
      }
    }
    fireChanged
  }

  def active = this.synchronized {
    val rc = _members.get(id) match {
      case Some(nodes) =>
        nodes.headOption.map { x=>
          x._1 == _eid
        }.getOrElse(false)
      case None => false
    }
    rc
  }

  def activeNodeState = this.synchronized {
    _members.get(id).flatMap(_.headOption.map(_._2))
  }

}
