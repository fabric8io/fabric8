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

import org.fusesource.leveldbjni._
import org.fusesource.leveldbjni.DB._


import scala.Some
import java.security.Key
import org.apache.activemq.apollo.broker.store._
import scala.Predef._
import org.apache.activemq.apollo.broker.store.PBSupport._
import java.io._
import org.fusesource.hawtbuf.AbstractVarIntSupport

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
  var db_options:Options = _

  def zero_copy_dir = {
    import FileSupport._
    config.directory / "zerocp"
  }

  def start() = {
    db_options = new Options();
    db_options.setCreateIfMissing(true);
    db = new RichDB(DB.open(db_options, config.directory));
  }

  def stop() = {
    db.delete
    db_options.delete()
  }

  def with_ctx[T](func: => T): T = {
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
            warn(e, "Message store transaction failed. Will keep retrying after every second.")
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
      info("Store recovered from inital failure.")
    }
    rc.get
  }

  def purge() = {
    with_ctx {
      val ro = new ReadOptions()
      ro.setFillCache(false)
      ro.setVerifyChecksums(false)
      val iterator = db.iterator(ro)
      db.cursor_keys() { key =>
        db.delete(key)
        true
      }
    }
  }

  def addQueue(record: QueueRecord, callback:Runnable) = {
    with_ctx {
      db.put(encode(queues_db_byte, record.key), record)
    }
    callback.run
  }

  def removeQueue(queue_key: Long, callback:Runnable) = {
    with_ctx {
      db.write() { batch =>
        batch.delete(encode(queues_db_byte, queue_key))
        db.cursor_keys_prefixed(encode(entries_db_byte, queue_key)) { key=>
          batch.delete(key)
          true
        }
      }
    }
    callback.run
  }

  def store(uows: Seq[LevelDBStore#DelayableUOW], callback:Runnable) {
    val now = encode(System.currentTimeMillis())
    with_ctx {
      db.write() { batch =>
        uows.foreach { uow =>
            uow.actions.foreach {
              case (msg, action) =>

                val message_record = action.message_record
                if (message_record != null) {
                  import PBSupport._
                  batch.put(encode(messages_db_byte, action.message_record.key), message_record)
                  batch.put(encode(message_refs_db_byte, action.message_record.key), now)
                }

                action.enqueues.foreach { entry =>
                  batch.put(encode(entries_db_byte, entry.queue_key, entry.entry_seq), entry)
                }

                action.dequeues.foreach { entry =>
                  batch.delete(encode(entries_db_byte, entry.queue_key, entry.entry_seq))
                }
            }
        }

      }
    }
    callback.run
  }

  def listQueues: Seq[Long] = {
    val rc = ListBuffer[Long]()
    with_ctx {
      db.cursor_keys_prefixed(queues_db) { key =>
        rc += decode_long_key(key)._2
        true // to continue cursoring.
      }
    }
    rc
  }

  def getQueue(queue_key: Long): Option[QueueRecord] = {
    with_ctx {
      db.get(encode(queues_db_byte, queue_key)).map( x=> decode_queue_record(x)  )
    }
  }

  def listQueueEntryGroups(queue_key: Long, limit: Int) : Seq[QueueEntryRange] = {
    var rc = ListBuffer[QueueEntryRange]()
    with_ctx {
      var group:QueueEntryRange = null
      db.cursor_prefixed( encode(entries_db_byte, queue_key)  ) { (key, value) =>

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
    rc
  }

  def getQueueEntries(queue_key: Long, firstSeq:Long, lastSeq:Long): Seq[QueueEntryRecord] = {
    var rc = ListBuffer[QueueEntryRecord]()
    with_ctx {
      val start = encode(entries_db_byte, queue_key, firstSeq)
      val end = encode(entries_db_byte, queue_key, lastSeq+1)
      db.cursor_range( start, end  ) { (key, value) =>
//        val entry:QueueEntryRecord = value
        rc += value
        true
      }
    }
    rc
  }

  val metric_load_from_index_counter = new TimeCounter
  var metric_load_from_index = metric_load_from_index_counter(false)

  def loadMessages(requests: ListBuffer[(Long, (Option[MessageRecord])=>Unit)]):Unit = {

    val missing = with_ctx {
      requests.flatMap { x =>
        val (message_key, callback) = x
        val record = metric_load_from_index_counter.time {
          db.get(encode(messages_db_byte, message_key)).map{ data=>
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

    if (missing.isEmpty)
      return

    // There's a small chance that a message was missing, perhaps we started a read tx, before the
    // write tx completed.  Lets try again..
    with_ctx {
      missing.foreach { x =>
        val (message_key, callback) = x
        val record = metric_load_from_index_counter.time {
          db.get(encode(messages_db_byte, message_key)).map{ data=>
            val rc:MessageRecord = data
            rc
          }
        }
        callback(record)
      }
    }
  }


  def getLastMessageKey:Long = {
    with_ctx {
      db.last_key(messages_db).map(decode_long_key(_)._2).getOrElse(0)
    }
  }

  def getLastQueueKey:Long = {
    with_ctx {
      db.last_key(queues_db).map(decode_long_key(_)._2).getOrElse(0)
    }
  }

  def export_pb(streams:StreamManager[OutputStream]):Result[Zilch,String] = {
    try {
      with_ctx {
        db.snapshot { snapshot=>
          val ro = new ReadOptions()
          ro.setSnapshot(snapshot)
          ro.setVerifyChecksums(true)
          ro.setFillCache(false)

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
            db.cursor_prefixed(queues_db, ro) { (_, value) =>
              write_framed(stream, value)
            }
          }

          streams.using_message_stream { stream=>
            db.cursor_prefixed(messages_db, ro) { (_, value) =>
              write_framed(stream, value)
            }
          }

          streams.using_queue_entry_stream { stream=>
            db.cursor_prefixed(entries_db, ro) { (_, value) =>
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

      val now = encode(System.currentTimeMillis())
      with_ctx {

        streams.using_queue_stream { stream=>
          foreach[QueuePB.Buffer](stream, QueuePB.FACTORY) { record=>
            db.put(encode(queues_db_byte, record.key), record.toUnframedByteArray)
          }
        }

        streams.using_message_stream { stream=>
          foreach[MessagePB.Buffer](stream, MessagePB.FACTORY) { record=>
            db.put(encode(messages_db_byte, record.getMessageKey), record.toUnframedByteArray)
            db.put(encode(message_refs_db_byte, record.getMessageKey), now)
          }
        }

        streams.using_queue_entry_stream { stream=>
          foreach[QueueEntryPB.Buffer](stream, QueueEntryPB.FACTORY) { record=>
            db.put(encode(entries_db_byte, record.queue_key, record.entry_seq), record.toUnframedByteArray)
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
