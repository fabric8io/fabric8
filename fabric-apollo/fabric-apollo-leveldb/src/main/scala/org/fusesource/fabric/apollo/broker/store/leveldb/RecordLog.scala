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

import java.{lang=>jl}
import java.{util=>ju}

import org.apache.activemq.apollo.util._
import org.fusesource.hawtbuf.{DataByteArrayOutputStream, AbstractVarIntSupport}
import java.io._
import java.util.zip.CRC32
import java.util.Map.Entry
import java.util.Arrays
import collection.mutable.{HashMap, HashSet}
import collection.immutable.TreeMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit
import org.fusesource.hawtdispatch.BaseRetained
import java.nio.ByteBuffer

object RecordLog {

  // The log files contain a sequence of variable length log records:
  // record :=
  //   '*L'     : int8*2     // 2 byte constant
  //   checksum : uint32     // crc32c of the data[]
  //   length   : uint32     // the length the the data
  //   data     : int8*length
  //
  // The log records are used to aggregate multiple data records
  // as a single write to the file system.

  //
  // The data is composed of multiple records too:
  // data :=
  //   kind     : int8
  //   length   : varInt
  //   body     : int8*length
  //
  // The kind field is an aid to the app layer.  It cannot be set to
  // '*'.

  val LOG_HEADER_PREFIX = Array('*', 'L').map(_.toByte)
  val LOG_HEADER_SIZE = 10 // BATCH_HEADER_PREFIX (2) + checksum (4) + length (4)

}

case class RecordLog(directory: File, log_suffix:String) {
  import FileSupport._
  import RecordLog._

  directory.mkdirs()

  var write_buffer_size = 1024 * 1024 * 4
  var log_size = 1024 * 1024 * 100
  private var current_appender:LogAppender = _

  case class LogInfo(file:File, position:Long, length:AtomicLong) {
    def limit = position+length.get
  }

  var log_infos = TreeMap[Long, LogInfo]()
  object log_mutex

  def delete(id:Long) = {
    log_mutex.synchronized {
      // We can't delete the current appender.
      if( current_appender.start != id ) {
        log_infos.get(id).foreach { info =>
          on_delete(info.file)
          log_infos = log_infos.filterNot(_._1 == id)
        }
      }
    }
  }

  protected def on_delete(file:File) = {
    file.delete()
  }

  class LogAppender(val file:File, val start:Long) {

    val fos = new FileOutputStream(file)
    def channel = fos.getChannel
    def os:OutputStream = fos

    val outbound = new DataByteArrayOutputStream()

    var batch_length = 0
    val length = new AtomicLong(0)
    var limit = start

    // set the file size ahead of time so that we don't have to sync the file
    // meta-data on every log sync.
    channel.position(log_size)
    channel.write(ByteBuffer.wrap(Array(0.toByte)))
    channel.force(true)
    channel.position(0)

    def sync = {
      // only need to update the file metadata if the file size changes..
      channel.force(length.get() > log_size)
    }

    def flush {
      if( batch_length!= 0 ) {

        // Update the buffer with the log header info now that we
        // can calc the length and checksum info
        val buffer = outbound.toBuffer

        assert(buffer.length()==LOG_HEADER_SIZE+batch_length)

        outbound.reset()
        outbound.write(LOG_HEADER_PREFIX)

        val checksum = new CRC32
        checksum.update(buffer.data, buffer.offset + LOG_HEADER_SIZE, buffer.length - LOG_HEADER_SIZE)
        var actual_checksum = (checksum.getValue & 0xFFFFFFFF).toInt

        outbound.writeInt( actual_checksum )
        outbound.writeInt(batch_length)

        // Actually write the record to the file..
        buffer.writeTo(os);

        length.addAndGet( buffer.length() )

        batch_length = 0
        outbound.reset()
      }
    }

    /**
     * returns the offset position of the data record.
     */
    def append(id:Byte, data: Array[Byte]): Long = {
      assert(id != LOG_HEADER_PREFIX(0))
      if( batch_length!=0 && (batch_length + data.length > write_buffer_size) ) {
        flush
      }
      if( batch_length==0 ) {
        // first data pos record is offset by the log header.
        outbound.skip(LOG_HEADER_SIZE);
        limit += LOG_HEADER_SIZE
      }
      val rc = limit;

      val start = outbound.position
      outbound.writeByte(id);
      outbound.writeVarInt(data.length)
      outbound.write(data);
      val count = outbound.position - start

      limit += count
      batch_length += count
      rc
    }

    def close = {
      flush
      channel.truncate(length.get())
      os.close()
    }
  }

  case class LogReader(file:File, start:Long) {

    val is = new RandomAccessFile(file, "r")

    val var_support = new AbstractVarIntSupport {
      def writeByte(p1: Int) = sys.error("Not supported")
      def readByte(): Byte = is.readByte()
    };

    def read(pos:Long) = this.synchronized {
      is.seek(pos-start)
      val id = is.read()
      if( id == LOG_HEADER_PREFIX(0) ) {
        (id, null, pos+LOG_HEADER_SIZE)
      } else {
        val length = var_support.readVarInt()
        val data = new Array[Byte](length)
        is.readFully(data)
        (id, data, is.getFilePointer)
      }
    }

    def close = this.synchronized {
      is.close()
    }

    def next_position(verify_checksums:Boolean=true):Long = this.synchronized {
      var offset = 0;
      val prefix = new Array[Byte](LOG_HEADER_PREFIX.length)
      var done = false
      while(!done) {
        try {
          is.seek(offset)
          is.readFully(prefix)
          if( !Arrays.equals(prefix, LOG_HEADER_PREFIX) ) {
            throw new IOException("Missing header prefix");
          }
          val expected_checksum = is.readInt();

          val length = is.readInt();
          if (verify_checksums) {
            val data = new Array[Byte](length)
            is.readFully(data)

            val checksum = new CRC32
            checksum.update(data)
            val actual_checksum = (checksum.getValue & 0xFFFFFFFF).toInt

            if( expected_checksum != actual_checksum ) {
              throw new IOException("Data checksum missmatch");
            }
          }
          offset += LOG_HEADER_SIZE + length

        } catch {
          case e:IOException =>
            done = true
        }
      }
      start + offset
    }
  }

  def create_log_appender(position: Long) = {
    new LogAppender(next_log(position), position)
  }

  def create_appender(position: Long): Any = {
    current_appender = create_log_appender(position)
    log_mutex.synchronized {
      log_infos += position -> new LogInfo(current_appender.file, position, current_appender.length)
    }
  }

  def open = {
    log_mutex.synchronized {
      log_infos = LevelDBClient.find_sequence_files(directory, log_suffix).map { case (position,file) =>
        position -> LogInfo(file, position, new AtomicLong(file.length()))
      }

      val append_pos = if( log_infos.isEmpty ) {
        0L
      } else {
        val (_, file) = log_infos.last
        val r = LogReader(file.file, file.position)
        try {
          val rc = r.next_position()
          file.length.set(rc - file.position)
          if( file.file.length != file.length.get() ) {
            // we need to truncate.
            using(new RandomAccessFile(file.file, "rw")) ( _.setLength(file.length.get()) )
          }
          rc
        } finally {
          r.close
        }
      }

      create_appender(append_pos)
    }
  }
  def close = {
    log_mutex.synchronized {
      current_appender.close
    }
  }

  def appender_limit = current_appender.limit
  def appender_start = current_appender.start

  def next_log(position:Long) = LevelDBClient.create_sequence_file(directory, position, log_suffix)

  def appender[T](func: (LogAppender)=>T):T= {
    try {
      func(current_appender)
    } finally {
      current_appender.flush
      log_mutex.synchronized {
        if ( current_appender.length.get >= log_size ) {
          current_appender.close
          on_log_rotate()
          create_appender(current_appender.limit)
        }
      }
    }
  }

  var on_log_rotate: ()=>Unit = ()=>{}

  val next_reader_id = new LongCounter()
  val reader_cache_files = new HashMap[File, HashSet[Long]];
  val reader_cache_readers = new LRUCache[Long, LogReader](100) {
    protected override def onCacheEviction(entry: Entry[Long, LogReader]) = {
      var key = entry.getKey
      var value = entry.getValue
      value.close

      val set = reader_cache_files.get(value.file).get
      set.remove(key)
      if( set.isEmpty ) {
        reader_cache_files.remove(value.file)
      }
    }
  }


  private def get_reader[T](pos:Long)(func: (LogReader)=>T) = {
    val infos = log_mutex.synchronized(log_infos)
    val info = infos.range(0L, pos+1).lastOption.map(_._2)
    info.map { info =>
      // Checkout a reader from the cache...
      val (set, reader_id, reader) = reader_cache_files.synchronized {
        var set = reader_cache_files.getOrElseUpdate(info.file, new HashSet);
        if( set.isEmpty ) {
          val reader_id = next_reader_id.getAndIncrement()
          val reader = new LogReader(info.file, info.position)
          set.add(reader_id)
          reader_cache_readers.put(reader_id, reader)
          (set, reader_id, reader)
        } else {
          val reader_id = set.head
          set.remove(reader_id)
          (set, reader_id, reader_cache_readers.get(reader_id))
        }
      }

      try {
        func(reader)
      } finally {
        // check him back in..
        reader_cache_files.synchronized {
          set.add(reader_id)
        }
      }
    }
  }

  def read(pos:Long) = {
    get_reader(pos)(_.read(pos))
  }

}
