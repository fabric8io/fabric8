/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.apollo.broker.store.leveldb

import dto.{LevelDBStoreDTO, LevelDBStoreStatusDTO}
import collection.Seq
import org.fusesource.hawtdispatch._
import java.util.concurrent._
import atomic.{AtomicReference, AtomicLong}
import org.apache.activemq.apollo.broker.store._
import org.apache.activemq.apollo.util._
import org.fusesource.hawtdispatch.ListEventAggregator
import org.apache.activemq.apollo.dto.StoreStatusDTO
import org.apache.activemq.apollo.util.OptionSupport._
import scala.util.continuations._
import java.io._
import org.apache.activemq.apollo.web.resources.ViewHelper
import collection.mutable.ListBuffer

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object LevelDBStore extends Log {
  val DATABASE_LOCKED_WAIT_DELAY = 10 * 1000;
}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class LevelDBStore(val config:LevelDBStoreDTO) extends DelayingStoreSupport {

  var next_queue_key = new AtomicLong(1)
  var next_msg_key = new AtomicLong(1)

  var write_executor:ExecutorService = _
  var gc_executor:ExecutorService = _
  var read_executor:ExecutorService = _

  var client:LevelDBClient = _
  def create_client = new LevelDBClient(this)


  def store_kind = "leveldb"

  override def toString = store_kind+" store at "+config.directory

  def flush_delay = config.flush_delay.getOrElse(100)
  
  protected def get_next_msg_key = next_msg_key.getAndIncrement

  override def zero_copy_buffer_allocator():ZeroCopyBufferAllocator = null

  protected def store(uows: Seq[DelayableUOW])(callback: =>Unit) = {
    write_executor {
      client.store(uows, ^{
        dispatch_queue {
          callback
        }
      })
    }
  }

  protected def _start(on_completed: Runnable) = {
    try {
      client = create_client
      write_executor = Executors.newFixedThreadPool(1, new ThreadFactory() {
        def newThread(r: Runnable) = {
          val rc = new Thread(r, store_kind + " store io write")
          rc.setDaemon(true)
          rc
        }
      })
      gc_executor = Executors.newFixedThreadPool(1, new ThreadFactory() {
        def newThread(r: Runnable) = {
          val rc = new Thread(r, store_kind + " store gc")
          rc.setDaemon(true)
          rc
        }
      })
      read_executor = Executors.newFixedThreadPool(config.read_threads.getOrElse(10), new ThreadFactory() {
        def newThread(r: Runnable) = {
          val rc = new Thread(r, store_kind + " store io read")
          rc.setDaemon(true)
          rc
        }
      })
      poll_stats
      write_executor {
        try {
          client.start()
          next_msg_key.set(client.getLastMessageKey + 1)
          next_queue_key.set(client.getLastQueueKey + 1)
          poll_gc
          on_completed.run
        } catch {
          case e:Throwable =>
            e.printStackTrace()
            LevelDBStore.error(e, "Store client startup failure: "+e)
        }
      }
    }
    catch {
      case e:Throwable =>
        e.printStackTrace()
        LevelDBStore.error(e, "Store startup failure: "+e)
    }
  }

  protected def _stop(on_completed: Runnable) = {
    new Thread() {
      override def run = {
        write_executor.shutdown
        write_executor.awaitTermination(60, TimeUnit.SECONDS)
        write_executor = null
        read_executor.shutdown
        read_executor.awaitTermination(60, TimeUnit.SECONDS)
        read_executor = null
        gc_executor.shutdown
        client.stop
        on_completed.run
      }
    }.start
  }

  private def keep_polling = {
    val ss = service_state
    ss.is_starting || ss.is_started
  }

  def poll_gc:Unit = {
    val interval = config.gc_interval.getOrElse(60*30)
    if( interval>0 ) {
      dispatch_queue.after(interval, TimeUnit.SECONDS) {
        if( keep_polling ) {
          gc {
            poll_gc
          }
        }
      }
    }
  }

  def gc(onComplete: =>Unit) = gc_executor {
    client.gc
    onComplete
  }

  /////////////////////////////////////////////////////////////////////
  //
  // Implementation of the Store interface
  //
  /////////////////////////////////////////////////////////////////////

  /**
   * Deletes all stored data from the store.
   */
  def purge(callback: =>Unit) = {
    write_executor {
      client.purge()
      next_queue_key.set(1)
      next_msg_key.set(1)
      callback
    }
  }


  /**
   * Ges the last queue key identifier stored.
   */
  def get_last_queue_key(callback:(Option[Long])=>Unit):Unit = {
    write_executor {
      callback(Some(client.getLastQueueKey))
    }
  }

  def add_queue(record: QueueRecord)(callback: (Boolean) => Unit) = {
    write_executor {
     client.addQueue(record, ^{ callback(true) })
    }
  }

  def remove_queue(queueKey: Long)(callback: (Boolean) => Unit) = {
    write_executor {
      client.removeQueue(queueKey,^{ callback(true) })
    }
  }

  def get_queue(queueKey: Long)(callback: (Option[QueueRecord]) => Unit) = {
    write_executor {
      callback( client.getQueue(queueKey) )
    }
  }

  def list_queues(callback: (Seq[Long]) => Unit) = {
    write_executor {
      callback( client.listQueues )
    }
  }

  val load_source = createSource(new ListEventAggregator[(Long, AtomicLong, (Option[MessageRecord])=>Unit)](), dispatch_queue)
  load_source.setEventHandler(^{drain_loads});
  load_source.resume


  def load_message(messageKey: Long, locator:AtomicLong)(callback: (Option[MessageRecord]) => Unit) = {
    message_load_latency_counter.start { end=>
      load_source.merge((messageKey, locator, { (result)=>
        end()
        callback(result)
      }))
    }
  }

  def drain_loads = {
    var data = load_source.getData
    message_load_batch_size_counter += data.size
    read_executor ^{
      client.loadMessages(data)
    }
  }

  def list_queue_entry_ranges(queueKey: Long, limit: Int)(callback: (Seq[QueueEntryRange]) => Unit) = {
    write_executor ^{
      callback( client.listQueueEntryGroups(queueKey, limit) )
    }
  }

  def list_queue_entries(queueKey: Long, firstSeq: Long, lastSeq: Long)(callback: (Seq[QueueEntryRecord]) => Unit) = {
    write_executor ^{
      callback( client.getQueueEntries(queueKey, firstSeq, lastSeq) )
    }
  }

  def poll_stats:Unit = {
    def displayStats = {
      if( service_state.is_started ) {

        flush_latency = flush_latency_counter(true)
        message_load_latency = message_load_latency_counter(true)
//        client.metric_journal_append = client.metric_journal_append_counter(true)
//        client.metric_index_update = client.metric_index_update_counter(true)
        commit_latency = commit_latency_counter(true)
        message_load_batch_size =  message_load_batch_size_counter(true)

        poll_stats
      }
    }

    dispatch_queue.executeAfter(1, TimeUnit.SECONDS, ^{ displayStats })
  }

  def get_store_status(callback:(StoreStatusDTO)=>Unit) = dispatch_queue {
    val rc = new LevelDBStoreStatusDTO
    fill_store_status(rc)
    rc.message_load_batch_size = message_load_batch_size
    write_executor {
      client.using_index {
        rc.index_stats = client.index.getProperty("leveldb.stats")
        rc.log_append_pos = client.log.appender_limit
        rc.index_snapshot_pos = client.last_index_snapshot_pos
        rc.last_gc_duration = client.last_gc_duration
        rc.last_gc_ts = client.last_gc_ts
        rc.in_gc = client.in_gc
        rc.log_stats = {
          var row_layout = "%-20s | %-10s | %10s/%-10s\n"
          row_layout.format("File", "Messages", "Used Size", "Total Size")+
          client.log.log_infos.map(x=> x._1 -> client.gc_detected_log_usage.get(x._1)).toSeq.flatMap { x=>
            try {
              val file = LevelDBClient.create_sequence_file(client.directory, x._1, LevelDBClient.LOG_SUFFIX)
              val size = file.length()
              val usage = x._2 match {
                case Some(usage)=>
                  (usage.count.toString, ViewHelper.memory(usage.size))
                case None=>
                  ("unknown", "unknown")
              }
              Some(row_layout.format(file.getName, usage._1, usage._2, ViewHelper.memory(size)))
            } catch {
              case e:Throwable =>
                None
            }
          }.mkString("")
        }
      }
      callback(rc)
    }
  }

  /**
   * Exports the contents of the store to the provided streams.  Each stream should contain
   * a list of framed protobuf objects with the corresponding object types.
   */
  def export_pb(streams:StreamManager[OutputStream]):Result[Zilch,String] @suspendable = write_executor ! {
    client.export_pb(streams)
  }

  /**
   * Imports a previously exported set of streams.  This deletes any previous data
   * in the store.
   */
  def import_pb(streams:StreamManager[InputStream]):Result[Zilch,String] @suspendable = write_executor ! {
    client.import_pb(streams)
  }

}
