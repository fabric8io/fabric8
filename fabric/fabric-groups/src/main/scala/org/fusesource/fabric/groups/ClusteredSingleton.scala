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
import reflect.BeanProperty

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
object ClusteredSupport {

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
class ClusteredSingletonWatcher[T <: NodeState](val stateClass:Class[T]) extends ChangeListenerSupport {
  import ClusteredSupport._
  
  protected var _group:Group = _
  def group = _group

  private val listener = new ChangeListener() {
    def changed() {
      val members = _group.members
      val t = new LinkedHashMap[String, T]()
      members.foreach {
        case (path, data) =>
          try {
            val value = decode(stateClass, data)
            t.put(path, value)
          } catch {
            case e: Throwable =>
              e.printStackTrace()
          }
      }
      changed_decoded(t)
    }

    def connected = {
      changed
      ClusteredSingletonWatcher.this.fireConnected
    }

    def disconnected = {
      changed
      ClusteredSingletonWatcher.this.fireDisconnected
    }
  }


  def start(group:Group) = this.synchronized {
    if(_group !=null )
      throw new IllegalStateException("Already started.")
    _group = group
    _group.add(listener)
  }

  def stop = this.synchronized {
    if(_group==null)
      throw new IllegalStateException("Not started.")
    _group.remove(listener)
    _members = HashMap[String, ListBuffer[(String,  T)]]()
    _group = null
  }

  def connected = this.synchronized {
    if(_group==null) {
      false
    } else {
      _group.connected
    }
  }

  protected var _members = HashMap[String, ListBuffer[(String,  T)]]()
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

  def masters = this.synchronized {
    _members.mapValues(_.head._2).toArray.map(_._2).toArray(new ClassManifest[T] {
      def erasure = stateClass
    })
  }

}
/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ClusteredSingleton[T <: NodeState ](stateClass:Class[T], @BeanProperty var id:String) extends ClusteredSingletonWatcher[T](stateClass) {
  import ClusteredSupport._

  def this(stateClass:Class[T]) = this(stateClass, null)

  private var _eid:String = _
  /** the ephemeral id of the node is unique within in the group */
  def eid = _eid

  override def stop = {
    this.synchronized {
      if(_eid==null) {
        leave
      }
      super.stop
    }
  }

  def join(state:T):Unit = this.synchronized {
    if(id==null)
      throw new IllegalArgumentException("id cannot be null")
    if(state==null)
      throw new IllegalArgumentException("State cannot be null")
    if(_group==null)
      throw new IllegalStateException("Not started.")
    if(_eid!=null)
      throw new IllegalStateException("Already joined")
    _eid = group.join(encode(state))
  }

  def leave:Unit = this.synchronized {
    if(id==null)
      throw new IllegalArgumentException("id cannot be null")
    if(_group==null)
      throw new IllegalStateException("Not started.")
    if(_eid==null)
      throw new IllegalStateException("Not joined")
    _group.leave(_eid)
    _eid = null
  }

  def update(state:T) = this.synchronized {
    if(state==null)
      throw new IllegalArgumentException("State cannot be null")
    if(_group==null)
      throw new IllegalStateException("Not started.")
    if(_eid==null)
      throw new IllegalStateException("Not joined")
    _group.update(_eid, encode(state))
  }

  def isMaster = this.synchronized {
    if(id==null)
      throw new IllegalArgumentException("id cannot be null")
    _members.get(id) match {
      case Some(nodes) =>
        nodes.headOption.map { x=>
          x._1 == _eid
        }.getOrElse(false)
      case None => false
    }
  }

  def master = this.synchronized {
    if(id==null)
      throw new IllegalArgumentException("id cannot be null")
    _members.get(id).map(_.head._2)
  }

  def slaves = this.synchronized {
    if(id==null)
      throw new IllegalArgumentException("id cannot be null")
    val rc = _members.get(id).map(_.toList).getOrElse(List())
    rc.drop(1).map(_._2)
  }

}
