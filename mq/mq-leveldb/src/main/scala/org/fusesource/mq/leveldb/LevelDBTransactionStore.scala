/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.mq.leveldb

import org.apache.activemq.store.memory.MemoryTransactionStore
import util.Log
import org.apache.activemq.store._

object LevelDBTransactionStore extends Log

/**
 * Provides a TransactionStore implementation that can create transaction aware
 * MessageStore objects from non transaction aware MessageStore objects.
 */
class LevelDBTransactionStore(store: LevelDBStore) extends MemoryTransactionStore(store) {

  override def proxy(messageStore: MessageStore): MessageStore = {
    return new ProxyMessageStore((messageStore)) {
    }
  }

  override def proxy(messageStore: TopicMessageStore): TopicMessageStore = {
    return new ProxyTopicMessageStore((messageStore)) {
    }
  }
}