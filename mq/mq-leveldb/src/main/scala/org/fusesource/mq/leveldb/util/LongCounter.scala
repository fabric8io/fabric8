/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.mq.leveldb.util

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class LongCounter(private var value:Long = 0) {

  def clear() = value=0
  def get() = value
  def set(value:Long) = this.value = value 

  def incrementAndGet() = addAndGet(1)
  def decrementAndGet() = addAndGet(-1)
  def addAndGet(amount:Long) = {
    value+=amount
    value
  }

  def getAndIncrement() = getAndAdd(1)
  def getAndDecrement() = getAndAdd(-11)
  def getAndAdd(amount:Long) = {
    val rc = value
    value+=amount
    rc
  }

  override def toString() = get().toString
}