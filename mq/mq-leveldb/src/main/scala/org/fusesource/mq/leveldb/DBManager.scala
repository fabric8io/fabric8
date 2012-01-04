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

case class MessageRecord(id:MessageId, data:Buffer, syncNeeded:Boolean=false)
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

  class DelayableUOW extends BaseRetained {
    val countDownFuture = CountDownFuture()
    var flushing = false;

    val uowId:Int = lastUowId.incrementAndGet()
    var actions = Map[MessageId, MessageAction]()
    var completed = false
    var disableDelay = false
    var delayableActions = 0

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
      flushing = true
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
        // For now, don't delay..
        disableDelay = true

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
      uowSource.merge(this)
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

  val uowSource = createSource(new ListEventAggregator[DelayableUOW](), dispatchQueue)
  uowSource.setEventHandler(^{drainUows});
  uowSource.resume

  var pendingStores = new HashMap[MessageId, DelayableUOW#MessageAction]()
  var pendingEnqueues = new HashMap[(Long,Long), DelayableUOW#MessageAction]()
  var delayedUows = new HashMap[Int, DelayableUOW]()

  val lastUowId = new AtomicInteger(1)

  def drainUows = {
    dispatchQueue.assertExecuting()
    val data = uowSource.getData
    data.foreach { uow =>

      delayedUows.put(uow.uowId, uow)

      uow.actions.foreach { case (msg, action) =>

        // dequeues can cancel out previous enqueues
        action.dequeues.foreach { currentDequeue=>
          val currentKey = key(currentDequeue)
          val prevAction:DelayableUOW#MessageAction = pendingEnqueues.remove(currentKey)

          def prevUow = prevAction.uow

          if( prevAction!=null && !prevUow.flushing ) {


            prevUow.delayableActions -= 1

            // yay we can cancel out a previous enqueue
            prevAction.enqueues = prevAction.enqueues.filterNot( x=> key(x) == currentKey )

            // if the message is not in any queues.. we can gc it..
            if( prevAction.enqueues == Nil && prevAction.messageRecord !=null ) {
              pendingStores.remove(msg)
              prevAction.messageRecord = null
              prevUow.delayableActions -= 1
            }

            // Cancel the action if it's now empty
            if( prevAction.isEmpty ) {
              prevAction.cancel()
            } else if( !prevUow.delayable ) {
              // flush it if there is no point in delyaing anymore
              flush(prevUow)
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
          flush(delayedUows.get(uowId))
        })
      } else {
        flush(uow)
      }

    }
  }

  private def flush(uow:DelayableUOW) = {
    if( uow!=null && !uow.flushing ) {
      uow.flushing = true
      delayedUows.remove(uow.uowId)
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
    val uows = flushSource.getData
    if( !uows.isEmpty ) {
      flushSource.suspend
      writeExecutor {
        client.store(uows)
        flushSource.resume
        dispatchQueue {
          uows.foreach { uow=>
            uow.onCompleted
            uow.actions.foreach { case (msg, action) =>
              if( action.messageRecord !=null ) {
                pendingStores.remove(msg)
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
      gc {
        pollGc
      }
    }
  }

  def gc(onComplete: =>Unit) = writeExecutor {
    if( started ) {
      client.gc
    }
    onComplete
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


  def getMessage(id: MessageId):Message = {
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
