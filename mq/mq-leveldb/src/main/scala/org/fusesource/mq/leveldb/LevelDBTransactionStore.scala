/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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