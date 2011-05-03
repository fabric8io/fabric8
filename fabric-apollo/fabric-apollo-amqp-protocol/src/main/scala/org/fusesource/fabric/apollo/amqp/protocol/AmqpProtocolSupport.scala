/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fusemq.amqp.protocol

import org.fusesource.fusemq.amqp.codec.types._
import org.fusesource.fusemq.amqp.codec.types.TypeFactory._
import collection.JavaConversions._
import AmqpConversions._
import org.fusesource.fusemq.amqp.api.{DistributionMode, Lifetime, Outcome}
import java.util.concurrent.atomic.AtomicLong

/**
 *
 */
object AmqpProtocolSupport {


  def maybe_set[T <: AmqpType[_,_]](arg:String, func: T => Unit) = Option[String](arg).foreach((arg) => func(createAmqpString(arg).asInstanceOf[T]))

  def maybe_set[T <: AmqpType[_,_]](arg:T, func: T => Unit) = Option[T](arg).foreach((arg) => func(arg))
  def maybe_set[T <: AmqpType[_, _]](arg:Array[String], func: Multiple => Unit) = Option(arg).foreach((arg) => func(arg))
  def maybe_set(arg:DistributionMode, func:AmqpSymbol => Unit) = Option(arg).foreach((arg) => func(arg))
  def maybe_set(arg:Lifetime, func: AmqpType[_, _] => Unit) = Option(arg).foreach((arg) => func(arg))
  def maybe_set(arg:Array[Outcome], func:Multiple => Unit):Unit = Option(arg).foreach((arg) => func(arg))
  def maybe_set(arg:Outcome, func:AmqpType[_, _] => Unit):Unit = Option(arg).foreach((arg) => func(arg))
  def maybe_set(arg:Boolean, func:Boolean => Unit):Unit = Option(arg).foreach((arg) => func(arg))
  def maybe_set(arg:AmqpTerminusExpiryPolicy, func:AmqpTerminusExpiryPolicy => Unit):Unit = Option(arg).foreach((arg => func(arg)))
  def maybe_set(arg:AmqpSeconds, func:AmqpSeconds => Unit):Unit = Option(arg).foreach((arg => func(arg)))



  def multiple(list:List[AmqpType[_,_]]):Multiple = {
    list.size match {
      case 0 =>
        null
      case 1 =>
        val rc = createMultiple
        rc.setValue(list.head)
        rc
      case _ =>
        val rc = createMultiple
        rc.setValue(createAmqpList(new IAmqpList.AmqpWrapperList[AmqpType[_,_]](asJavaList(list))))
        rc
    }
  }
}