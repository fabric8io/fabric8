/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.utilities.link

import org.fusesource.fabric.apollo.amqp.protocol.interfaces.SenderMessageCache
import org.fusesource.hawtbuf.Buffer
import collection.mutable.Queue

class InMemoryMessageCache extends SenderMessageCache {

  val queue = Queue[Buffer]()

  def cache(message: Buffer) = queue.enqueue(message)

  def uncache = queue.dequeue()
}