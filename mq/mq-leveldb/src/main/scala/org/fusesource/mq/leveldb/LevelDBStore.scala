/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.mq.leveldb

import org.apache.activemq.broker.BrokerService
import org.apache.activemq.broker.BrokerServiceAware
import org.apache.activemq.broker.ConnectionContext
import org.apache.activemq.command._
import org.apache.activemq.openwire.OpenWireFormat
import org.apache.activemq.usage.SystemUsage
import org.apache.activemq.util.IOExceptionSupport
import org.apache.activemq.util.ServiceStopper
import org.apache.activemq.util.ServiceSupport
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong
import util.Log
import reflect.BeanProperty
import org.apache.activemq.store.memory.MemoryTransactionStore
import org.apache.activemq.store._
import java.util._

object LevelDBStore extends Log {
  def toIOException(e: Throwable): IOException = {
    if (e.isInstanceOf[ExecutionException]) {
      var cause: Throwable = (e.asInstanceOf[ExecutionException]).getCause
      if (cause.isInstanceOf[IOException]) {
        return cause.asInstanceOf[IOException]
      }
    }
    if (e.isInstanceOf[IOException]) {
      return e.asInstanceOf[IOException]
    }
    return IOExceptionSupport.create(e)
  }

  def waitOn(future: Future[AnyRef]): Unit = {
    try {
      future.get
    }
    catch {
      case e: Throwable => {
        throw toIOException(e)
      }
    }
  }
}

case class DurableSubscription(subKey:Long, topicKey:Long, info: SubscriptionInfo) {
  var lastAckPosition = 0L
  var cursorPosition = 0L
}

class LevelDBStore extends ServiceSupport with BrokerServiceAware with PersistenceAdapter {
  import LevelDBStore._

  final val wireFormat = new OpenWireFormat
  final val db = new DBManager(this)

  @BeanProperty
  var directory: File = null
  @BeanProperty
  var logSize: Long = 1024 * 1024 * 100
  @BeanProperty
  var indexFactory: String = "org.fusesource.leveldbjni.JniDBFactory, org.iq80.leveldb.impl.Iq80DBFactory"
  @BeanProperty
  var sync: Boolean = true
  @BeanProperty
  var verifyChecksums: Boolean = false
  @BeanProperty
  var indexMaxOpenFiles: Int = 1000
  @BeanProperty
  var indexBlockRestartInterval: Int = 16
  @BeanProperty
  var paranoidChecks: Boolean = false
  @BeanProperty
  var indexWriteBufferSize: Int = 4 << 20
  @BeanProperty
  var indexBlockSize: Int = 4 * 1024
  @BeanProperty
  var indexCompression: String = "snappy"
  @BeanProperty
  var indexCacheSize: Long = 1024 * 1024 * 256L
  @BeanProperty
  var flushDelay = 1000*5
  @BeanProperty
  var asyncBufferSize = 1024*1024*4
  @BeanProperty
  var monitorStats = false

  var purgeOnStatup: Boolean = false
  var brokerService: BrokerService = null

  var queues: HashMap[ActiveMQQueue, LevelDBStore#LevelDBTxMessageStore] = new HashMap[ActiveMQQueue, LevelDBStore#LevelDBTxMessageStore]
  var topics: HashMap[ActiveMQTopic, LevelDBStore#LevelDBTxTopicMessageStore] = new HashMap[ActiveMQTopic, LevelDBStore#LevelDBTxTopicMessageStore]
  var topicsById: HashMap[Long, LevelDBStore#LevelDBTxTopicMessageStore] = new HashMap[Long, LevelDBStore#LevelDBTxTopicMessageStore]

  override def toString: String = {
    return "LevelDB:[" + directory.getAbsolutePath + "]"
  }

  def doStart: Unit = {
    debug("starting")
    db.start
    if (purgeOnStatup) {
      purgeOnStatup = false
      db.purge
      info("Purged: "+this)
    }
    db.loadCollections
    debug("started")
  }

  def doStop(stopper: ServiceStopper): Unit = {
    db.stop
    info("Stopped "+this)
  }

  def setBrokerService(brokerService: BrokerService): Unit = {
    this.brokerService = brokerService
  }

  def setBrokerName(brokerName: String): Unit = {
  }

  def setUsageManager(usageManager: SystemUsage): Unit = {
  }

  def deleteAllMessages: Unit = {
    purgeOnStatup = true
  }

  def getLastMessageBrokerSequenceId: Long = {
    return 0
  }

  def beginTransaction(context: ConnectionContext): Unit = {
  }

  def commitTransaction(context: ConnectionContext): Unit = {
  }

  def rollbackTransaction(context: ConnectionContext): Unit = {
  }


  def createTransactionStore: TransactionStore = {
    return this.transactionStore
  }

  def createQueueMessageStore(destination: ActiveMQQueue): MessageStore = {
    var rc: MessageStore = queues.get(destination)
    if (rc == null) {
      rc = db.createQueueStore(destination)
    }
    return rc
  }

  def createQueueMessageStore(destination: ActiveMQQueue, key: Long): MessageStore = {
    var rc = this.transactionStore.proxy(new LevelDBMessageStore(destination, key))
    this synchronized {
      queues.put(destination, rc)
    }
    return rc
  }

  def removeQueueMessageStore(destination: ActiveMQQueue): Unit = this synchronized {
    var store = queues.remove(destination)
    if (store != null) {
      store match {
        case store:LevelDBTxMessageStore => db.destroyQueueStore(store.store.key)
        case store:LevelDBTxTopicMessageStore => db.destroyQueueStore(store.store.key)
      }
    }
  }

  def createTopicMessageStore(destination: ActiveMQTopic): TopicMessageStore = {
    var rc = topics.get(destination)
    if (rc == null) {
      rc = db.createTopicStore(destination)
    }
    return rc
  }

  def createTopicMessageStore(destination: ActiveMQTopic, key: Long) = {
    var rc = this.transactionStore.proxy(new LevelDBTopicMessageStore(destination, key))
    this synchronized {
      topics.put(destination, rc)
      topicsById.put(key, rc)
    }
    rc
  }

  def removeTopicMessageStore(destination: ActiveMQTopic): Unit = {
  }

  def getLogAppendPosition = db.getLogAppendPosition

  def getDestinations: Set[ActiveMQDestination] = {
    var rc: HashSet[ActiveMQDestination] = new HashSet[ActiveMQDestination]
    rc.addAll(topics.keySet)
    rc.addAll(queues.keySet)
    return rc
  }

  def getLastProducerSequenceId(id: ProducerId): Long = {
    throw new RuntimeException("Implement me.")
  }

  def size: Long = {
    return 0
  }

  def checkpoint(sync: Boolean): Unit = db.checkpoint(sync)

  def withUow[T](func:(DBManager#DelayableUOW)=>T):T = {
    val uow = db.createUow
    try {
      func(uow)
    } finally {
      uow.release()
    }
  }

  private def subscriptionKey(clientId: String, subscriptionName: String): String = {
    return clientId + ":" + subscriptionName
  }

  case class LevelDBTxMessageStore(store: LevelDBMessageStore) extends ProxyMessageStore(store) {
  }

  case class LevelDBTxTopicMessageStore(store: LevelDBTopicMessageStore) extends ProxyTopicMessageStore(store) {
  }

  final val transactionStore = new MemoryTransactionStore(this) {
    override def proxy(store: MessageStore) = LevelDBTxMessageStore(store.asInstanceOf[LevelDBMessageStore])
    override def proxy(store: TopicMessageStore) = LevelDBTxTopicMessageStore(store.asInstanceOf[LevelDBTopicMessageStore])
  }

  case class LevelDBMessageStore(dest: ActiveMQDestination, val key: Long) extends AbstractMessageStore(dest) {

    protected val lastSeq: AtomicLong = new AtomicLong(0)
    protected var cursorPosition: Long = 0

    lastSeq.set(db.getLastQueueEntrySeq(key))

    override def asyncAddQueueMessage(context: ConnectionContext, message: Message): Future[AnyRef] = {
      withUow{uow=>
        uow.enqueue(key, lastSeq.incrementAndGet, message)
      }
    }

    def addMessage(context: ConnectionContext, message: Message): Unit = {
      waitOn(asyncAddQueueMessage(context, message))
    }

    override def removeAsyncMessage(context: ConnectionContext, ack: MessageAck): Unit = {
      waitOn(withUow{uow=>
        uow.dequeue(key, ack.getLastMessageId)
      })
    }

    def removeMessage(context: ConnectionContext, ack: MessageAck): Unit = {
      removeAsyncMessage(context, ack)
    }

    def getMessage(id: MessageId): Message = {
      var message: Message = db.getMessage(id)
      if (message == null) {
        throw new IOException("Message id not found: " + id)
      }
      return message
    }

    def removeAllMessages(context: ConnectionContext): Unit = {
      db.collectionEmpty(key)
      cursorPosition = 0
    }

    def getMessageCount: Int = {
      return db.collectionSize(key)
    }

    override def isEmpty: Boolean = {
      return db.collectionIsEmpty(key)
    }

    def recover(listener: MessageRecoveryListener): Unit = {
      cursorPosition = db.cursorMessages(key, listener, 0)
    }

    def resetBatching: Unit = {
      cursorPosition = 0
    }

    def recoverNextMessages(maxReturned: Int, listener: MessageRecoveryListener): Unit = {
      cursorPosition = db.cursorMessages(key, LimitingRecoveryListener(maxReturned, listener), cursorPosition)
    }

    override def setBatch(id: MessageId): Unit = {
      cursorPosition = db.queuePosition(id)
    }

  }

  case class LimitingRecoveryListener(max: Int, listener: MessageRecoveryListener) extends MessageRecoveryListener {
    private var recovered: Int = 0
    def hasSpace = recovered < max && listener.hasSpace
    def recoverMessage(message: Message) = {
      recovered += 1;
      listener.recoverMessage(message)
    }
    def recoverMessageReference(ref: MessageId) = {
      recovered += 1;
      listener.recoverMessageReference(ref)
    }
    def isDuplicate(ref: MessageId) = listener.isDuplicate(ref)
  }
  

  //
  // This gts called when the store is first loading up, it restores
  // the existing durable subs..
  def createSubscription(sub:DurableSubscription) = {
    val topic = this.synchronized {
      topicsById.get(sub.topicKey)
    }
    if( topic!=null ) {
      topic.store.synchronized {
        topic.store.subscriptions.put((sub.info.getClientId, sub.info.getSubcriptionName), sub)
      }
    } else {
      // Topic does not exist.. so kill the durable sub..
      db.removeSubscription(sub)
    }
  }
  
  
  def getTopicGCPositions = {
    import collection.JavaConversions._
    val topics = this.synchronized {
      new ArrayList(topicsById.values())
    }
    topics.flatMap(_.store.gcPosition).toSeq
  }

  class LevelDBTopicMessageStore(dest: ActiveMQDestination, key: Long) extends LevelDBMessageStore(dest, key) with TopicMessageStore {
    var subscriptions = new  HashMap[(String, String), DurableSubscription]()
    var firstSeq = 0L

    def gcPosition:Option[(Long, Long)] = {
      var pos = lastSeq.get()
      subscriptions.synchronized {
        import collection.JavaConversions._
        subscriptions.values().foreach { sub =>
          if( sub.lastAckPosition < pos ) {
            pos = sub.lastAckPosition
          }
        }
        if( firstSeq != pos+1) {
          firstSeq = pos+1
          Some(key, firstSeq)
        } else {
          None
        }
      }
    }
    
    def addSubsciption(info: SubscriptionInfo, retroactive: Boolean) = {
      var sub = db.addSubscription(key, info)
      subscriptions.synchronized {
        subscriptions.put((info.getClientId, info.getSubcriptionName), sub)
      }
      sub.lastAckPosition = if (retroactive) 0 else lastSeq.get()
      withUow{ uow=>
        uow.updateAckPosition(sub)
      }
    }
    
    def getAllSubscriptions: Array[SubscriptionInfo] = subscriptions.synchronized {
      import collection.JavaConversions._
      subscriptions.values().map(_.info).toArray
    }

    def lookupSubscription(clientId: String, subscriptionName: String): SubscriptionInfo = subscriptions.synchronized {
      Option(subscriptions.get((clientId, subscriptionName))).map(_.info).getOrElse(null)
    }

    def deleteSubscription(clientId: String, subscriptionName: String): Unit = {
      val sub = subscriptions.synchronized {
        subscriptions.remove((clientId, subscriptionName))
      }
      if(sub!=null) {
        db.removeSubscription(sub)
      }
    }

    private def lookup(clientId: String, subscriptionName: String): Option[DurableSubscription] = subscriptions.synchronized {
      Option(subscriptions.get((clientId, subscriptionName)))
    }

    def acknowledge(context: ConnectionContext, clientId: String, subscriptionName: String, messageId: MessageId, ack: MessageAck): Unit = {
      lookup(clientId, subscriptionName).foreach { sub =>
        sub.lastAckPosition = db.queuePosition(messageId)
        withUow{ uow=>
          uow.updateAckPosition(sub)
        }
      }
    }
    
    def resetBatching(clientId: String, subscriptionName: String): Unit = {
      lookup(clientId, subscriptionName).foreach { sub =>
        sub.cursorPosition = 0
      }
    }
    def recoverSubscription(clientId: String, subscriptionName: String, listener: MessageRecoveryListener): Unit = {
      lookup(clientId, subscriptionName).foreach { sub =>
        sub.cursorPosition = db.cursorMessages(key, listener, sub.cursorPosition.max(sub.lastAckPosition+1))
      }
    }
    
    def recoverNextMessages(clientId: String, subscriptionName: String, maxReturned: Int, listener: MessageRecoveryListener): Unit = {
      lookup(clientId, subscriptionName).foreach { sub =>
        sub.cursorPosition = db.cursorMessages(key,  LimitingRecoveryListener(maxReturned, listener), sub.cursorPosition.max(sub.lastAckPosition+1))
      }
    }
    
    def getMessageCount(clientId: String, subscriptionName: String): Int = {
      lookup(clientId, subscriptionName) match {
        case Some(sub) => db.queueSizeFrom(key, sub.lastAckPosition+1)
        case None => 0
      }
    }

  }
}
