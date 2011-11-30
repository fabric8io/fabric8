/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.apollo.amqp.protocol

import org.fusesource.fabric.apollo.amqp.codec.types._
import Role.SENDER
import Role.RECEIVER
import collection.mutable.HashMap
import collection.SortedMap

class LinkStore {

  private val incoming_links = new HashMap[String, AmqpLink]()
  private val outgoing_links = new HashMap[String, AmqpLink]()

  private val local_handles = new HashMap[Int, Tuple2[Role, String]]()
  private val remote_handles = new HashMap[Int, Tuple2[Role, String]]()

  implicit def short2Int(value:Short) = value.asInstanceOf[Int]
  implicit def int2Short(value:Int) = value.asInstanceOf[Short]

  override def toString = {
    "incoming={" + incoming_links + "}\n" +
    "outgoing={" + outgoing_links + "}\n" +
    "local_handles={" + local_handles + "}" +
    "remote_handles={" + remote_handles + "}"
  }

  def foreach(func: AmqpLink => Unit) = {
    incoming_links.values.foreach((l) => func(l))
    outgoing_links.values.foreach((l) => func(l))
  }

  def apply_to_links(func:AmqpLink => Unit) = {
    incoming_links.foreach((link) => func(link._2))
    outgoing_links.foreach((link) => func(link._2))
  }

  def allocate_handle(handle:Short, link:AmqpLink): Short = {
    if (local_handles.keysIterator.contains(handle)) {
      allocate_handle(handle + 1, link)
    } else {
      local_handles += handle.asInstanceOf[Int] -> (link.role, link.name)
      handle
    }
  }

  def add_local(handle:Int, link:AmqpLink) = {
    local_handles += handle -> (link.role, link.name)
  }

  def add_remote(handle:Int, link:AmqpLink) = {
    remote_handles += handle -> (link.role, link.name)
  }

  def add_remote(role:Role, handle:Int, name:String, if_exists:Option[AmqpLink] => AmqpLink) = {
    if_exists(get_by_name(role, name))
  }

  def add(link:AmqpLink) = {
    link.role match {
      case SENDER =>
        outgoing_links += link.name -> link
      case RECEIVER =>
        incoming_links += link.name -> link
    }
    link.handle match {
      case Some(handle) =>
        add_local(handle, link)
      case None =>
    }
    link.remoteHandle match {
      case Some(handle) =>
        add_remote(handle, link)
      case None =>
    }
  }

  def get_by_name(role:Role, name:String) = {
    role match {
      case SENDER =>
        outgoing_links.get(name)
      case RECEIVER =>
        incoming_links.get(name)
    }
  }

  def remove_by_name(role:Role, name:String) = {
    role match {
      case SENDER =>
        outgoing_links.remove(name)
      case RECEIVER =>
        incoming_links.remove(name)
    }
  }

  private def get(handle:Int, map:HashMap[Int, Tuple2[Role, String]]) = {
    map.get(handle) match {
      case Some((role, name)) =>
        get_by_name(role, name)
      case None =>
        None
    }
  }

  private def remove(handle:Int, map:HashMap[Int, Tuple2[Role, String]]) = {
    map.remove(handle) match {
      case Some((role, name)) =>
        remove_by_name(role, name)
      case None =>
        None
    }
  }

  def get_by_local_handle(handle:Int) = {
    get(handle, local_handles)
  }

  def get_by_remote_handle(handle:Int) = {
    get(handle, remote_handles)
  }

  def remove_by_local_handle(handle:Int) = {
    remove(handle, local_handles)
  }

  def remove_by_remote_handle(handle:Int) = {
    remove(handle, remote_handles)
  }

}
