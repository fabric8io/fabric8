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

import dto.LevelDBStoreDTO
import java.{lang=>jl}
import java.{util=>ju}

import collection.mutable.ListBuffer
import org.apache.activemq.apollo.util._
import org.fusesource.hawtbuf.proto.PBMessageFactory

import scala.Some
import org.apache.activemq.apollo.broker.store._
import org.apache.activemq.apollo.broker.store.PBSupport._
import java.io._
import java.util.concurrent.TimeUnit
import scala.Predef._
import org.fusesource.hawtbuf.{Buffer, AbstractVarIntSupport}
import org.fusesource.leveldbjni._

object LevelDBClient extends Log
/**
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class LevelDBClient(store: LevelDBStore) {

  import HelperTrait._

  import LevelDBClient._

  def dispatchQueue = store.dispatch_queue

  /////////////////////////////////////////////////////////////////////
  //
  // Helpers
  //
  /////////////////////////////////////////////////////////////////////

  private def directory = config.directory

  /////////////////////////////////////////////////////////////////////
  //
  // Public interface used by the LevelDBStore
  //
  /////////////////////////////////////////////////////////////////////

  var config: LevelDBStoreDTO = null

  var db:RichDB = _

  var cache:Cache = _
  var logger:Logger = _
  var db_options:Options = _

  var sync = false;
  var verify_checksums = false;

  def start() = {
    import OptionSupport._

    sync = config.sync.getOrElse(false);
    verify_checksums = config.verify_checksums.getOrElse(false);

    db_options = new Options();
    db_options.createIfMissing(true);

    config.max_open_files.foreach( db_options.maxOpenFiles(_) )
    config.block_restart_interval.foreach( db_options.blockRestartInterval(_) )
    config.paranoid_checks.foreach( db_options.paranoidChecks(_) )
    config.write_buffer_size.foreach( db_options.writeBufferSize(_) )
    config.block_size.foreach( db_options.blockSize(_) )
    Option(config.compression).foreach(x => db_options.compression( x match {
      case "snappy" => CompressionType.kSnappyCompression
      case "none" => CompressionType.kNoCompression
      case _ => CompressionType.kSnappyCompression
    }) )

    cache = new Cache(config.cache_size.getOrElse(1024*1024*256L))
    db_options.cache(cache)

//    logger = new Logger() {
//      def log(msg: String) = debug(msg)
//    }
//    db_options.infoLog(logger)

    retry {
      db = new RichDB(DB.open(db_options, config.directory));
    }
  }

  def stop() = {
    if( db!=null ) {
      db.delete
      db = null
    }
    if( logger!=null ) {
      logger.delete
      logger = null
    }
    if( cache!=null ) {
      cache.delete
      cache = null
    }
  }

  def retry[T](func: => T): T = {
    var error:Throwable = null
    var rc:Option[T] = None

    // We will loop until the tx succeeds.  Perhaps it's
    // failing due to a temporary condition like low disk space.
    while(!rc.isDefined) {

      try {
        rc = Some(func)
      } catch {
        case e:Throwable =>
          if( error==null ) {
            warn(e, "DB operation failed. (entering recovery mode)")
          }
          error = e
      }

      if (!rc.isDefined) {
        // We may need to give up if the store is being stopped.
        if ( !store.service_state.is_started ) {
          throw error
        }
        Thread.sleep(1000)
      }
    }

    if( error!=null ) {
      info("DB recovered from failure.")
    }
    rc.get
  }

  def purge() = {
    retry {
      val ro = new ReadOptions()
      ro.fillCache(false)
      ro.verifyChecksums(false)

      val wo = new WriteOptions
      wo.sync(sync)

      db.snapshot { snapshot =>
        ro.snapshot(snapshot)
        db.cursor_keys(ro) { key =>
          db.delete(key)
          true
        }
      }
    }
  }

  def addQueue(record: QueueRecord, callback:Runnable) = {
    val wo = new WriteOptions
    wo.sync(sync)
    retry {
      db.put(encode(queue_prefix, record.key), record, wo)
    }
    callback.run
  }

  def removeQueue(queue_key: Long, callback:Runnable) = {
    retry {
      val ro = new ReadOptions
      ro.fillCache(false)
      ro.verifyChecksums(verify_checksums)
      val wo = new WriteOptions
      wo.sync(false)
      db.delete(encode(queue_prefix, queue_key), wo)
      db.cursor_keys_prefixed(encode(queue_entry_prefix, queue_key), ro) { key=>
        db.delete(key, wo)
        true
      }
    }
    callback.run
  }

  def store(uows: Seq[LevelDBStore#DelayableUOW], callback:Runnable) {
    val now = encode(System.currentTimeMillis())
    retry {
      val wo = new WriteOptions
      wo.sync(sync)
      db.write(wo) { batch =>
        uows.foreach { uow =>
            uow.actions.foreach {
              case (msg, action) =>

                val message_record = action.message_record
                if (message_record != null) {
                  batch.put(encode(message_prefix, action.message_record.key), message_record)
                  batch.put(encode(message_ts_prefix, action.message_record.key), now)
                }

                action.enqueues.foreach { entry =>
                  batch.put(encode(queue_entry_prefix, entry.queue_key, entry.entry_seq), entry)
                }

                action.dequeues.foreach { entry =>
                  batch.delete(encode(queue_entry_prefix, entry.queue_key, entry.entry_seq))
                }
            }
        }

      }
    }
    callback.run
  }

  def listQueues: Seq[Long] = {
    val rc = ListBuffer[Long]()
    retry {
      val ro = new ReadOptions
      ro.verifyChecksums(verify_checksums)
      ro.fillCache(false)
      db.cursor_keys_prefixed(queue_prefix_array, ro) { key =>
        rc += decode_long_key(key)._2
        true // to continue cursoring.
      }
    }
    rc
  }

  def getQueue(queue_key: Long): Option[QueueRecord] = {
    retry {
      val ro = new ReadOptions
      ro.fillCache(false)
      ro.verifyChecksums(verify_checksums)
      db.get(encode(queue_prefix, queue_key), ro).map( x=> decode_queue_record(x)  )
    }
  }

  def listQueueEntryGroups(queue_key: Long, limit: Int) : Seq[QueueEntryRange] = {
    var rc = ListBuffer[QueueEntryRange]()
    val ro = new ReadOptions
    ro.verifyChecksums(verify_checksums)
    ro.fillCache(false)
    retry {
      db.snapshot { snapshot =>
        ro.snapshot(snapshot)

        var group:QueueEntryRange = null
        db.cursor_prefixed( encode(queue_entry_prefix, queue_key), ro) { (key, value) =>

          val (_,_,current_key) = decode_long_long_key(key)
          if( group == null ) {
            group = new QueueEntryRange
            group.first_entry_seq = current_key
          }

          val entry:QueueEntryRecord = value

          group.last_entry_seq = current_key
          group.count += 1
          group.size += entry.size

          if(group.expiration == 0){
            group.expiration = entry.expiration
          } else {
            if( entry.expiration != 0 ) {
              group.expiration = entry.expiration.min(group.expiration)
            }
          }

          if( group.count == limit) {
            rc += group
            group = null
          }

          true // to continue cursoring.
        }
        if( group!=null ) {
          rc += group
        }
      }
    }
    rc
  }

  def getQueueEntries(queue_key: Long, firstSeq:Long, lastSeq:Long): Seq[QueueEntryRecord] = {
    var rc = ListBuffer[QueueEntryRecord]()
    val ro = new ReadOptions
    ro.verifyChecksums(verify_checksums)
    ro.fillCache(true)
    retry {
      db.snapshot { snapshot =>
        ro.snapshot(snapshot)
        val start = encode(queue_entry_prefix, queue_key, firstSeq)
        val end = encode(queue_entry_prefix, queue_key, lastSeq+1)
        db.cursor_range( start, end, ro ) { (key, value) =>
          rc += value
          true
        }
      }
    }
    rc
  }

  val metric_load_from_index_counter = new TimeCounter
  var metric_load_from_index = metric_load_from_index_counter(false)

  def loadMessages(requests: ListBuffer[(Long, (Option[MessageRecord])=>Unit)]):Unit = {

    val ro = new ReadOptions
    ro.verifyChecksums(verify_checksums)
    ro.fillCache(true)

    val missing = retry {
      db.snapshot { snapshot =>
        ro.snapshot(snapshot)
        requests.flatMap { x =>
          val (message_key, callback) = x
          val record = metric_load_from_index_counter.time {
            db.get(encode(message_prefix, message_key), ro).map{ data=>
              val rc:MessageRecord = data
              rc
            }
          }
          if( record.isDefined ) {
            callback(record)
            None
          } else {
            Some(x)
          }
        }
      }
    }

    if (missing.isEmpty)
      return

    // There's a small chance that a message was missing, perhaps we started a read tx, before the
    // write tx completed.  Lets try again..
    retry {
      db.snapshot { snapshot =>
        ro.snapshot(snapshot)
        missing.foreach { x =>
          val (message_key, callback) = x
          val record = metric_load_from_index_counter.time {
            db.get(encode(message_prefix, message_key), ro).map{ data=>
              val rc:MessageRecord = data
              rc
            }
          }
          callback(record)
        }
      }
    }
  }


  def getLastMessageKey:Long = {
    retry {
      db.last_key(message_prefix_array).map(decode_long_key(_)._2).getOrElse(0)
    }
  }

  def getLastQueueKey:Long = {
    retry {
      db.last_key(queue_prefix_array).map(decode_long_key(_)._2).getOrElse(0)
    }
  }

  def mark_and_sweep:Unit = {
    var active_counter = 0
    var delete_counter = 0
    val latency_counter = new TimeCounter

    val now = System.currentTimeMillis()
    val marker = encode(now)

    debug("leveldb gc stating")
    latency_counter.time {

      retry {
        val ro = new ReadOptions()
        ro.fillCache(false)
        ro.verifyChecksums(verify_checksums)

        val wo = new WriteOptions
        wo.sync(false)

        // update all the message refs with the current time.
        db.snapshot { snapshot =>
          ro.snapshot(snapshot)
          db.cursor_prefixed(queue_entry_prefix_array, ro) { (_,value) =>
            val entry:QueueEntryRecord = value
            db.put(encode(message_ts_prefix, entry.message_key), marker)
            true
          }
        }

        val expiration_point = now - 1000;
        // now scan and sweep all messages that have an older value for the timestamp.
        db.snapshot { snapshot =>
          ro.snapshot(snapshot)
          db.cursor_prefixed(message_ts_prefix_array) { (key,value) =>
            val (_, message_id) = decode_long_key(key)
            val ts = decode_long(value)

            // Looks like it's no longer referenced.. it had an old ts.
            if ( ts < expiration_point ) {
              // Delete it.
              db.delete(encode(message_prefix, message_id))
              db.delete(encode(message_ts_prefix, message_id))
              delete_counter += 1
            } else {
              active_counter += 1
            }
            true
          }
        }


      }
    }

    info("leveldb gc deleted %d messages, it took: %f seconds, %d messages are left", delete_counter, latency_counter.apply(true).totalTime(TimeUnit.SECONDS), active_counter)
    info("leveldb stats:\n"+db.getProperty("leveldb.stats"))
  }


  def export_pb(streams:StreamManager[OutputStream]):Result[Zilch,String] = {
    try {
      retry {
        db.snapshot { snapshot=>
          val ro = new ReadOptions
          ro.snapshot(snapshot)
          ro.verifyChecksums(verify_checksums)
          ro.fillCache(false)

          def write_framed(stream:OutputStream, value:Array[Byte]) = {
            val helper = new AbstractVarIntSupport {
              def readByte: Byte = throw new UnsupportedOperationException
              def writeByte(value: Int) = stream.write(value)
            }
            helper.writeVarInt(value.length)
            stream.write(value);
            true
          }

          streams.using_queue_stream { stream =>
            db.cursor_prefixed(queue_prefix_array, ro) { (_, value) =>
              write_framed(stream, value)
            }
          }

          streams.using_message_stream { stream=>
            db.cursor_prefixed(message_prefix_array, ro) { (_, value) =>
              write_framed(stream, value)
            }
          }

          streams.using_queue_entry_stream { stream=>
            db.cursor_prefixed(queue_entry_prefix_array, ro) { (_, value) =>
              write_framed(stream, value)
            }
          }

        }
      }
      Success(Zilch)
    } catch {
      case x:Exception=>
        Failure(x.getMessage)
    }
  }

  def import_pb(streams:StreamManager[InputStream]):Result[Zilch,String] = {
    try {
      purge

      val now = encode(System.currentTimeMillis())
      retry {
        val wo = new WriteOptions
        wo.sync(sync)

        def foreach[Buffer] (stream:InputStream, fact:PBMessageFactory[_,_])(func: (Buffer)=>Unit):Unit = {
          var done = false
          do {
            try {
              func(fact.parseFramed(stream).asInstanceOf[Buffer])
            } catch {
              case x:EOFException =>
                done = true
            }
          } while( !done )
        }


        streams.using_queue_stream { stream=>
          foreach[QueuePB.Buffer](stream, QueuePB.FACTORY) { record=>
            db.put(encode(queue_prefix, record.key), record.toUnframedByteArray, wo)
          }
        }

        streams.using_message_stream { stream=>
          foreach[MessagePB.Buffer](stream, MessagePB.FACTORY) { record=>
            db.put(encode(message_prefix, record.getMessageKey), record.toUnframedByteArray, wo)
            db.put(encode(message_ts_prefix, record.getMessageKey), now, wo)
          }
        }

        streams.using_queue_entry_stream { stream=>
          foreach[QueueEntryPB.Buffer](stream, QueueEntryPB.FACTORY) { record=>
            db.put(encode(queue_entry_prefix, record.queue_key, record.entry_seq), record.toUnframedByteArray, wo)
          }
        }
      }

      Success(Zilch)

    } catch {
      case x:Exception=>
        Failure(x.getMessage)
    }
  }
}
