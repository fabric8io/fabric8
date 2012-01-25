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

import collection.mutable.ListBuffer
import java.util.HashMap
import org.fusesource.hawtdispatch._
import org.fusesource.hawtdispatch.{BaseRetained, ListEventAggregator}
import org.apache.activemq.command._
import record.CollectionRecord
import java.util.concurrent._
import atomic._
import org.fusesource.hawtbuf.Buffer
import org.fusesource.hawtbuf.Buffer._
import org.apache.activemq.util.ByteSequence
import org.apache.activemq.store.MessageRecoveryListener

case class MessageRecord(id:MessageId, data:Buffer, syncNeeded:Boolean=false) {
  var locator:(Long, Int) = _
}

case class QueueEntryRecord(id:MessageId, queueKey:Long, queueSeq:Long)
case class QueueRecord(id:ActiveMQDestination, queue_key:Long)
case class QueueEntryRange()

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
case class CountDownFuture(completed:CountDownLatch=new CountDownLatch(1)) extends java.util.concurrent.Future[Object] {
  def countDown = completed.countDown()
  def cancel(mayInterruptIfRunning: Boolean) = false
  def isCancelled = false

  def get() = {
    completed.await()
    null
  }

  def get(p1: Long, p2: TimeUnit) = {
    if(completed.await(p1, p2)) {
      null
    } else {
      throw new TimeoutException
    }
  }

  def isDone = completed.await(0, TimeUnit.SECONDS);
}

object UowManagerConstants {
  val QUEUE_COLLECTION_TYPE = 1
  val TOPIC_COLLECTION_TYPE = 2
  val TRANSACTION_COLLECTION_TYPE = 3
}

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class DBManager(parent:LevelDBStore) {
  import UowManagerConstants._

  var lastCollectionKey = new AtomicLong(0)
  val client = new LevelDBClient(parent);

  def writeExecutor = client.writeExecutor
  def flushDelay = parent.flushDelay

  val dispatchQueue = createQueue(toString)
  val aggregator = new AggregatingExecutor(dispatchQueue)

  val asyncCapacityRemaining = new AtomicLong(0L)

  def createUow() = new DelayableUOW

  var uowCommittedCounter = 0L
  var uowCanceledCounter = 0L
  var uowStoringCounter = 0L
  var uowStoredCounter = 0L
  
  class DelayableUOW extends BaseRetained {
    val countDownFuture = CountDownFuture()
    var storing = false;
    var canceled = false;

    val uowId:Int = lastUowId.incrementAndGet()
    var actions = Map[MessageId, MessageAction]()
    var completed = false
    var disableDelay = false
    var delayableActions = 0
    var commitNanoTime = 0L

    def syncNeeded = actions.find( _._2.syncNeeded ).isDefined
    def size = actions.foldLeft(0L){ case (sum, entry) => 
      sum + entry._2.size 
    }

    class MessageAction {
      var id:MessageId = _
      var messageRecord: MessageRecord = null
      var enqueues = ListBuffer[QueueEntryRecord]()
      var dequeues = ListBuffer[QueueEntryRecord]()

      def uow = DelayableUOW.this
      def isEmpty() = messageRecord==null && enqueues==Nil && dequeues==Nil

      def cancel() = {
        uow.rm(id)
      }

      def syncNeeded = messageRecord!=null && messageRecord.syncNeeded
      def size = (if(messageRecord!=null) messageRecord.data.length+20 else 0) + ((enqueues.size+dequeues.size)*50)  
    }

    def completeAsap() = this.synchronized { disableDelay=true }
    def delayable = !disableDelay && delayableActions>0 && flushDelay>=0

    def rm(msg:MessageId) = {
      actions -= msg
      if( actions.isEmpty ) {
        cancel
      }
    }

    def cancel = {
      dispatchQueue.assertExecuting()
      uowCanceledCounter += 1
      canceled = true
      delayedUows.remove(uowId)
      onCompleted
    }

    def action(id:MessageId) = {
      actions.get(id) match {
        case Some(x) => x
        case None =>
          val x = new MessageAction
          x.id = id
          actions += id->x
          x
      }
    }

    def store(record: MessageRecord) = {
      val action = new MessageAction
      action.id = record.id
      action.messageRecord = record
      this.synchronized {
        actions += record.id -> action
      }
      aggregator {
        pendingStores.put(record.id, action)
      }
      delayableActions += 1
    }

    def enqueue(queueKey:Long, queueSeq:Long, message:Message)  = {

      val id = message.getMessageId
      if( id.getDataLocator==null ) {
        var packet = parent.wireFormat.marshal(message)
        val record = MessageRecord(id, new Buffer(packet.data, packet.offset, packet.length), message.isResponseRequired)
        id.setDataLocator(record)
        store(record)
      }

      val entry = QueueEntryRecord(id, queueKey, queueSeq)
      assert(id.getEntryLocator == null)
      id.setEntryLocator((queueKey, queueSeq))

      val a = this.synchronized {
        // Need to figure out a better way for the the broker to hint when
        // a store should be delayed or not.
        disableDelay = true // message.isResponseRequired

        val a = action(entry.id)
        a.enqueues += entry
        delayableActions += 1
        a
      }

      aggregator {
        pendingEnqueues.put(key(entry), a)
        pendingStores.put(id, a)
      }
      countDownFuture
    }

    def dequeue(queueKey:Long, id:MessageId) = {
      val (queueKey, queueSeq) = id.getEntryLocator.asInstanceOf[(Long, Long)];
      val entry = QueueEntryRecord(id, queueKey, queueSeq)
      this.synchronized {
        action(id).dequeues += entry
      }
      countDownFuture
    }
    
    var asyncCapacityUsed = 0L
    
    override def dispose = {
      if( !syncNeeded ) {
        val s = size
        if( asyncCapacityRemaining.addAndGet(-s) > 0 ) {
          asyncCapacityUsed = s
          countDownFuture.countDown
        } else {
          asyncCapacityRemaining.addAndGet(s)
        }
      }
      commitSource.merge(this)
    }

    def onCompleted() = {
      if( asyncCapacityUsed != 0 ) {
        asyncCapacityRemaining.addAndGet(asyncCapacityUsed)
        asyncCapacityUsed = 0
      } else {
        countDownFuture.countDown
      }
      super.dispose
    }
  }

  def key(x:QueueEntryRecord) = (x.queueKey, x.queueSeq)

  val commitSource = createSource(new ListEventAggregator[DelayableUOW](), dispatchQueue)
  commitSource.setEventHandler(^{processCommitted});
  commitSource.resume

  var pendingStores = new ConcurrentHashMap[MessageId, DelayableUOW#MessageAction]()
  var pendingEnqueues = new HashMap[(Long,Long), DelayableUOW#MessageAction]()
  var delayedUows = new HashMap[Int, DelayableUOW]()

  val lastUowId = new AtomicInteger(1)

  def processCommitted = {
    dispatchQueue.assertExecuting()
    val uows = commitSource.getData
    uowCommittedCounter += uows.size
    
    val now = System.nanoTime()
    uows.foreach { uow =>
      uow.commitNanoTime = now 
      delayedUows.put(uow.uowId, uow)

      uow.actions.foreach { case (id, action) =>

        // dequeues can cancel out previous enqueues
        action.dequeues.foreach { currentDequeue=>
          val currentKey = key(currentDequeue)
          val prevAction:DelayableUOW#MessageAction = pendingEnqueues.remove(currentKey)

          def prevUow = prevAction.uow

          if( prevAction!=null && !prevUow.storing ) {


            prevUow.delayableActions -= 1

            // yay we can cancel out a previous enqueue
            prevAction.enqueues = prevAction.enqueues.filterNot( x=> key(x) == currentKey )

            // if the message is not in any queues.. we can gc it..
            if( prevAction.enqueues == Nil && prevAction.messageRecord !=null ) {
              pendingStores.remove(id)
              prevAction.messageRecord = null
              prevUow.delayableActions -= 1
            }

            // Cancel the action if it's now empty
            if( prevAction.isEmpty ) {
              prevAction.cancel()
            } else if( !prevUow.delayable ) {
              // flush it if there is no point in delaying anymore
              store(prevUow)
            }

            // since we canceled out the previous enqueue.. now cancel out the action
            action.dequeues = action.dequeues.filterNot( _ == currentDequeue)
            if( action.isEmpty ) {
              action.cancel()
            }
          }
        }
      }

      val uowId = uow.uowId
      if( uow.delayable ) {
        dispatchQueue.executeAfter(flushDelay, TimeUnit.MILLISECONDS, ^{
          store(delayedUows.get(uowId))
        })
      } else {
        store(uow)
      }

    }
  }

  private def store(uow:DelayableUOW) = {
    if( uow!=null && !uow.storing && !uow.canceled ) {
      uow.storing = true
      delayedUows.remove(uow.uowId)
      storeSource.merge(uow)
    }
  }

  val storeSource = createSource(new ListEventAggregator[DelayableUOW](), dispatchQueue)
  storeSource.setEventHandler(^{processStores});
  storeSource.resume


  def processStores:Unit = {
    dispatchQueue.assertExecuting()
    if( !started ) {
      return
    }
    val uows = storeSource.getData
    if( !uows.isEmpty ) {
      uowStoringCounter += uows.size
      storeSource.suspend
      writeExecutor {
        client.store(uows)
        storeSource.resume
        dispatchQueue {
          uowStoredCounter += uows.size
          uows.foreach { uow=>
            uow.onCompleted
            uow.actions.foreach { case (id, action) =>
              if( action.messageRecord !=null ) {
                pendingStores.remove(id)
              }
              action.enqueues.foreach { queueEntry=>
                pendingEnqueues.remove(key(queueEntry))
              }
            }
          }
        }
      }
    }
  }

  var started = false

  def start = {
    asyncCapacityRemaining.set(parent.asyncBufferSize)
    client.start()
    dispatchQueue.sync {
      started = true
      pollGc
      if(parent.monitorStats) {
        monitorStats
      }
    }
  }

  def stop() = {
    dispatchQueue.sync {
      started = false
    }
    client.stop()
  }

  def pollGc:Unit = dispatchQueue.after(10, TimeUnit.SECONDS) {
    if( started ) {
      writeExecutor {
        if( started ) {
          client.gc
          pollGc
        }
      }
    }
  }

  def monitorStats:Unit = dispatchQueue.after(1, TimeUnit.SECONDS) {
    if( started ) {
      println("committed: %d, canceled: %d, storing: %d, stored: %d, ".format(uowCommittedCounter, uowCanceledCounter, uowStoringCounter, uowStoredCounter))
      uowCommittedCounter = 0
      uowCanceledCounter = 0
      uowStoringCounter = 0
      uowStoredCounter = 0
      monitorStats
    }
  }

  /////////////////////////////////////////////////////////////////////
  //
  // Implementation of the Store interface
  //
  /////////////////////////////////////////////////////////////////////

  def checkpoint(sync:Boolean) = writeExecutor.sync {
    client.snapshotIndex(sync)
  }

  def purge = writeExecutor.sync {
    client.purge
    lastCollectionKey.set(1)
  }

  def getLastQueueEntrySeq(key:Long) = {
    client.getLastQueueEntrySeq(key)
  }

  def collectionEmpty(key:Long) = writeExecutor.sync {
    client.collectionEmpty(key)
  }

  def collectionSize(key:Long) = {
    client.collectionSize(key)
  }

  def collectionIsEmpty(key:Long) = {
    client.collectionIsEmpty(key)
  }
  
  def cursorMessages(key:Long, listener:MessageRecoveryListener, cursorPosition:Long) = {
    var nextPos = cursorPosition;
    client.queueCursor(key, cursorPosition) { record =>
      if( listener.hasSpace ) {
        
        //if ( !ackedAndPrepared.contains(record.id) ) {
          listener.recoverMessage(getMessage(record.id))
        //}
        nextPos = record.queueSeq+1
        true
      } else {
        false
      }
    }
    nextPos
  }

  def getCursorPosition(key:Long, id: MessageId, cursorPosition:Long):Long = {
    val keyLocation = id.getEntryLocator.asInstanceOf[(Long, Long)]
    assert(keyLocation!=null)
    keyLocation._2
  }

  def createQueueStore(dest:ActiveMQQueue) = {
    parent.createQueueMessageStore(dest, createStore(dest, QUEUE_COLLECTION_TYPE))
  }

  def getLogAppendPosition = writeExecutor.sync {
    client.getLogAppendPosition
  }

  def createTopicStore(dest:ActiveMQTopic) = {
    parent.createTopicMessageStore(dest, createStore(dest, TOPIC_COLLECTION_TYPE))
  }

  def createStore(destination:ActiveMQDestination, collectionType:Int) = {
    val collection = new CollectionRecord.Bean()
    collection.setType(collectionType)
    collection.setMeta(utf8(destination.getQualifiedName))
    collection.setKey(lastCollectionKey.incrementAndGet())
    val buffer = collection.freeze()
    buffer.toFramedBuffer // eager encode the record.
    writeExecutor.sync {
      client.addCollection(buffer)
    }
    collection.getKey
  }
  
  def loadCollections = {
    val collections = writeExecutor.sync {
      client.listCollections
    }
    var last = 0L
    collections.foreach { case (key, record) =>
      last = key
      record.getType match {
        case QUEUE_COLLECTION_TYPE =>
          val dest = ActiveMQDestination.createDestination(record.getMeta.utf8().toString, ActiveMQDestination.QUEUE_TYPE).asInstanceOf[ActiveMQQueue]
          parent.createQueueMessageStore(dest, key)
        case TOPIC_COLLECTION_TYPE =>
          val dest = ActiveMQDestination.createDestination(record.getMeta.utf8().toString, ActiveMQDestination.TOPIC_TYPE).asInstanceOf[ActiveMQTopic]
          parent.createTopicMessageStore(dest, key)
        case x =>
          println("Unknown collection type: "+x)
      }
    }
    lastCollectionKey.set(last)
  }


  def getMessage(x: MessageId):Message = {

    val id = Option(pendingStores.get(x)).map(_.id).getOrElse(x)

    val locator = id.getDataLocator()
    assert(locator!=null)
    val buffer = locator match {
      case x:MessageRecord =>
        // Encoded form is still in memory..
        Some(x.data)
      case (pos:Long, len:Int) =>
        // Load the encoded form from disk.
        client.log.read(pos, len).map(new Buffer(_))
    }

    // Lets decode
    val message = buffer.map{ x =>
      val msg = parent.wireFormat.unmarshal(new ByteSequence(x.data, x.offset, x.length)).asInstanceOf[Message]
      msg.setMessageId(id)
      msg
    }
    message.getOrElse(null)
  }


}
