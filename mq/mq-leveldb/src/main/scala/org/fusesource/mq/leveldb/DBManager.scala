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

import collection.mutable.ListBuffer
import java.util.HashMap
import org.fusesource.hawtdispatch._
import org.fusesource.hawtdispatch.{BaseRetained, ListEventAggregator}
import java.util.concurrent._
import atomic._
import org.fusesource.hawtbuf.Buffer
import org.apache.activemq.util.ByteSequence
import org.apache.activemq.store.MessageRecoveryListener
import java.lang.ref.WeakReference
import scala.Option._
import org.fusesource.hawtbuf.Buffer._
import org.apache.activemq.command._
import org.fusesource.mq.leveldb.record.{EntryRecord, SubscriptionRecord, CollectionRecord}
import util.TimeMetric

case class MessageRecord(id:MessageId, data:Buffer, syncNeeded:Boolean=false) {
  var locator:(Long, Int) = _
}

case class QueueEntryRecord(id:MessageId, queueKey:Long, queueSeq:Long)
case class QueueRecord(id:ActiveMQDestination, queue_key:Long)
case class QueueEntryRange()
case class SubAckRecord(subKey:Long, ackPosition:Long)

sealed trait UowState {
  def stage:Int
}
// UoW is initial open.
object UowOpen extends UowState {
  override def stage = 0
  override def toString = "UowOpen"
}
// UoW is Committed once the broker finished creating it.
object UowClosed extends UowState {
  override def stage = 1
  override def toString = "UowClosed"
}
// UOW is delayed until we send it to get flushed.
object UowDelayed extends UowState {
  override def stage = 2
  override def toString = "UowDelayed"
}
object UowFlushQueued extends UowState {
  override def stage = 3
  override def toString = "UowFlushQueued"
}

object UowFlushing extends UowState {
  override def stage = 4
  override def toString = "UowFlushing"
}
// Then it moves on to be flushed. Flushed just
// means the message has been written to disk
// and out of memory
object UowFlushed extends UowState {
  override def stage = 5
  override def toString = "UowFlushed"
}

// Once completed then you know it has been synced to disk.
object UowCompleted extends UowState {
  override def stage = 6
  override def toString = "UowCompleted"
}

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
  val SUBSCRIPTION_COLLECTION_TYPE = 4
}

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class DBManager(val parent:LevelDBStore) {
  import UowManagerConstants._

  var lastCollectionKey = new AtomicLong(0)
  val client = new LevelDBClient(parent);

  def writeExecutor = client.writeExecutor
  def flushDelay = parent.flushDelay

  val dispatchQueue = createQueue(toString)
  val aggregator = new AggregatingExecutor(dispatchQueue)

  val asyncCapacityRemaining = new AtomicLong(0L)

  def createUow() = new DelayableUOW

  var uowClosedCounter = 0L
  var uowCanceledCounter = 0L
  var uowStoringCounter = 0L
  var uowStoredCounter = 0L

  val uow_complete_latency = TimeMetric() 
  
  class DelayableUOW extends BaseRetained {
    val countDownFuture = CountDownFuture()
    var canceled = false;

    val uowId:Int = lastUowId.incrementAndGet()
    var actions = Map[MessageId, MessageAction]()
    var subAcks = ListBuffer[SubAckRecord]()
    var completed = false
    var disableDelay = false
    var delayableActions = 0

    private var _state:UowState = UowOpen

    def state = this._state
    def state_=(next:UowState) {
      assert(this._state.stage < next.stage)
      this._state = next
    }

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
      if( actions.isEmpty && state.stage < UowFlushing.stage ) {
        cancel
      }
    }

    def cancel = {
      dispatchQueue.assertExecuting()
      uowCanceledCounter += 1
      canceled = true
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

    def updateAckPosition(sub:DurableSubscription) = {
      subAcks += SubAckRecord(sub.subKey, sub.lastAckPosition)
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
        cancelable_enqueue_actions.put(key(entry), a)
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

    def complete_asap = this.synchronized {
      disableDelay=true
      if( state eq UowDelayed ) {
        enqueueFlush(this)
      }
    }

    var asyncCapacityUsed = 0L
    var disposed_at = 0L
    
    override def dispose = this.synchronized {
      state = UowClosed
      disposed_at = System.nanoTime()
      if( !syncNeeded ) {
        val s = size
        if( asyncCapacityRemaining.addAndGet(-s) > 0 ) {
          asyncCapacityUsed = s
          countDownFuture.countDown
        } else {
          asyncCapacityRemaining.addAndGet(s)
        }
      }
      closeSource.merge(this)
    }

    def onCompleted() = this.synchronized {
      if ( state.stage < UowCompleted.stage ) {
        if( asyncCapacityUsed != 0 ) {
          asyncCapacityRemaining.addAndGet(asyncCapacityUsed)
          asyncCapacityUsed = 0
        } else {
          uow_complete_latency.add(System.nanoTime() - disposed_at)
          countDownFuture.countDown
        }
        super.dispose
      }
    }
  }

  def key(x:QueueEntryRecord) = (x.queueKey, x.queueSeq)

  val closeSource = createSource(new ListEventAggregator[DelayableUOW](), dispatchQueue)
  closeSource.setEventHandler(^{processClosed});
  closeSource.resume

  var pendingStores = new ConcurrentHashMap[MessageId, DelayableUOW#MessageAction]()
  var cancelable_enqueue_actions = new HashMap[(Long,Long), DelayableUOW#MessageAction]()

  val lastUowId = new AtomicInteger(1)

  def processClosed = {
    dispatchQueue.assertExecuting()
    val uows = closeSource.getData
    uowClosedCounter += uows.size
    
    val now = System.nanoTime()
    uows.foreach { uow =>

      // Broker could issue a flush_message call before
      // this stage runs.. which make the stage jump over UowDelayed
      if( uow.state.stage < UowDelayed.stage ) {
        uow.state = UowDelayed
      }
      if( uow.state.stage < UowFlushing.stage ) {
        uow.actions.foreach { case (id, action) =>

          // The UoW may have been canceled.
          if( action.messageRecord!=null && action.enqueues.isEmpty ) {
            pendingStores.remove(id)
            action.messageRecord = null
            uow.delayableActions -= 1
          }
          if( action.isEmpty ) {
            action.cancel()
          }

          // dequeues can cancel out previous enqueues
          action.dequeues.foreach { entry=>
            val entry_key = key(entry)
            val prev_action:DelayableUOW#MessageAction = cancelable_enqueue_actions.remove(entry_key)

            if( prev_action!=null ) {
              val prev_uow = prev_action.uow
              prev_uow.synchronized {
                if( !prev_uow.canceled ) {

                  prev_uow.delayableActions -= 1

                  // yay we can cancel out a previous enqueue
                  prev_action.enqueues = prev_action.enqueues.filterNot( x=> key(x) == entry_key )
                  if( prev_uow.state.stage >= UowDelayed.stage ) {

                    // if the message is not in any queues.. we can gc it..
                    if( prev_action.enqueues == Nil && prev_action.messageRecord !=null ) {
                      pendingStores.remove(id)
                      prev_action.messageRecord = null
                      prev_uow.delayableActions -= 1
                    }

                    // Cancel the action if it's now empty
                    if( prev_action.isEmpty ) {
                      prev_action.cancel()
                    } else if( !prev_uow.delayable ) {
                      // flush it if there is no point in delaying anymore
                      prev_uow.complete_asap
                    }
                  }
                }
              }
              // since we canceled out the previous enqueue.. now cancel out the action
              action.dequeues = action.dequeues.filterNot( _ == entry)
              if( action.isEmpty ) {
                action.cancel()
              }
            }
          }
        }
      }

      if( !uow.canceled && uow.state.stage < UowFlushQueued.stage ) {
        if( uow.delayable ) {
          // Let the uow get GCed if its' canceled during the delay window..
          val ref = new WeakReference[DelayableUOW](uow)
          scheduleFlush(ref)
        } else {
          enqueueFlush(uow)
        }
      }

    }
  }

  private def scheduleFlush(ref: WeakReference[DelayableUOW]) {
    dispatchQueue.executeAfter(flushDelay, TimeUnit.MILLISECONDS, ^ {
      val uow = ref.get();
      if (uow != null) {
        enqueueFlush(uow)
      }
    })
  }

  private def enqueueFlush(uow:DelayableUOW) = {
    if( uow!=null && !uow.canceled && uow.state.stage < UowFlushQueued.stage ) {
      uow.state = UowFlushQueued
      flushSource.merge(uow)
    }
  }

  val flushSource = createSource(new ListEventAggregator[DelayableUOW](), dispatchQueue)
  flushSource.setEventHandler(^{drainFlushes});
  flushSource.resume

  def drainFlushes:Unit = {
    dispatchQueue.assertExecuting()
    if( !started ) {
      return
    }

    // Some UOWs may have been canceled.
    val uows = flushSource.getData.flatMap { uow=>
      if( uow.canceled ) {
        None
      } else {
        // It will not be possible to cancel the UOW anymore..
        uow.state = UowFlushing
        uow.actions.foreach { case (_, action) =>
          action.enqueues.foreach { queue_entry=>
            val action = cancelable_enqueue_actions.remove(key(queue_entry))
            assert(action!=null)
          }
        }
        Some(uow)
      }
    }

    if( !uows.isEmpty ) {
      uowStoringCounter += uows.size
      flushSource.suspend
      writeExecutor {
        client.store(uows)
        flushSource.resume
        dispatchQueue {
          uowStoredCounter += uows.size
          uows.foreach { uow=>
            uow.onCompleted
            uow.actions.foreach { case (id, action) =>
              if( action.messageRecord !=null ) {
                pendingStores.remove(id)
              }
              action.enqueues.foreach { queueEntry=>
                cancelable_enqueue_actions.remove(key(queueEntry))
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
      val positions = parent.getTopicGCPositions
      writeExecutor {
        if( started ) {
          client.gc(positions)
          pollGc
        }
      }
    }
  }

  def monitorStats:Unit = dispatchQueue.after(1, TimeUnit.SECONDS) {
    if( started ) {
      println(("committed: %d, canceled: %d, storing: %d, stored: %d, " +
        "uow complete: %,.3f ms, " +
        "index write: %,.3f ms, " +
        "log write: %,.3f ms, log flush: %,.3f ms, log rotate: %,.3f ms").format(
          uowClosedCounter, uowCanceledCounter, uowStoringCounter, uowStoredCounter,
          uow_complete_latency.reset,
        client.max_index_write_latency.reset,
          client.log.max_log_write_latency.reset, client.log.max_log_flush_latency.reset, client.log.max_log_rotate_latency.reset
      ))
      uowClosedCounter = 0
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
  
  def cursorMessages(key:Long, listener:MessageRecoveryListener, startPos:Long) = {
    var nextPos = startPos;
    client.queueCursor(key, nextPos) { msg =>
      if( listener.hasSpace ) {
        listener.recoverMessage(msg)
        nextPos += 1
        true
      } else {
        false
      }
    }
    nextPos
  }

  def queueSizeFrom(key:Long, pos:Long) = client.queueSizeFrom(key, pos)

  def queuePosition(id: MessageId):Long = {
    id.getEntryLocator.asInstanceOf[(Long, Long)]._2
  }

  def createQueueStore(dest:ActiveMQQueue):parent.LevelDBMessageStore = {
    parent.createQueueMessageStore(dest, createStore(dest, QUEUE_COLLECTION_TYPE))
  }
  def destroyQueueStore(key:Long) = writeExecutor.sync {
      client.removeCollection(key)
  }

  def getLogAppendPosition = writeExecutor.sync {
    client.getLogAppendPosition
  }

  def addSubscription(topic_key:Long, info:SubscriptionInfo):DurableSubscription = {
    val record = new SubscriptionRecord.Bean
    record.setTopicKey(topic_key)
    record.setClientId(info.getClientId)
    record.setSubscriptionName(info.getSubcriptionName)
    if( info.getSelector!=null ) {
      record.setSelector(info.getSelector)
    }
    if( info.getDestination!=null ) {
      record.setDestinationName(info.getDestination.getQualifiedName)
    }
    val collection = new CollectionRecord.Bean()
    collection.setType(SUBSCRIPTION_COLLECTION_TYPE)
    collection.setKey(lastCollectionKey.incrementAndGet())
    collection.setMeta(record.freeze().toUnframedBuffer)

    val buffer = collection.freeze()
    buffer.toFramedBuffer // eager encode the record.
    writeExecutor.sync {
      client.addCollection(buffer)
    }
    DurableSubscription(collection.getKey, topic_key, info)
  }

  def removeSubscription(sub:DurableSubscription) = {
    client.removeCollection(sub.subKey)
  }

  def createTopicStore(dest:ActiveMQTopic) = {
    var key = createStore(dest, TOPIC_COLLECTION_TYPE)
    parent.createTopicMessageStore(dest, key)
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
        case SUBSCRIPTION_COLLECTION_TYPE =>
          val sr = SubscriptionRecord.FACTORY.parseUnframed(record.getMeta)
          val info = new SubscriptionInfo
          info.setClientId(sr.getClientId)
          info.setSubcriptionName(sr.getSubscriptionName)
          if( sr.hasSelector ) {
            info.setSelector(sr.getSelector)
          }
          if(sr.hasDestinationName) {
            info.setSubscribedDestination(ActiveMQDestination.createDestination(sr.getDestinationName, ActiveMQDestination.TOPIC_TYPE))
          }

          var sub = DurableSubscription(key, sr.getTopicKey, info)
          sub.lastAckPosition = client.getAckPosition(key);
          parent.createSubscription(sub)
        case _ =>
      }
    }
    lastCollectionKey.set(last)
  }


  def getMessage(x: MessageId):Message = {
    val id = Option(pendingStores.get(x)).map(_.id).getOrElse(x)
    val locator = id.getDataLocator()
    val msg = client.getMessage(locator)
    msg.setMessageId(id)
    msg
  }

}
