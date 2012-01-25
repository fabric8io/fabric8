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

import java.{lang=>jl}
import java.{util=>ju}

import java.util.zip.CRC32
import java.util.Map.Entry
import collection.immutable.TreeMap
import java.util.concurrent.atomic.AtomicLong
import java.io._
import org.fusesource.hawtbuf.{DataByteArrayInputStream, DataByteArrayOutputStream, Buffer}
import org.fusesource.hawtdispatch.BaseRetained
import org.fusesource.mq.leveldb.util.Log
import org.fusesource.mq.leveldb.util.FileSupport._
import org.apache.activemq.util.LRUCache

object RecordLog extends Log {

  // The log files contain a sequence of variable length log records:
  // record := header + data
  //
  // header :=
  //   '*'      : int8       // Start of Record Magic
  //   kind     : int8       // Help identify content type of the data.
  //   checksum : uint32     // crc32c of the data[]
  //   length   : uint32     // the length the the data

  val LOG_HEADER_PREFIX = '*'.toByte
  val LOG_HEADER_SIZE = 10

  val BUFFER_SIZE = 1024*512
  val BYPASS_BUFFER_SIZE = 1024*16

  case class LogInfo(file:File, position:Long, length:Long) {
    def limit = position+length
  }
}

case class RecordLog(directory: File, logSuffix:String) {
  import RecordLog._

  directory.mkdirs()

  var logSize = 1024 * 1024 * 100
  var currentAppender:LogAppender = _
  var paranoidChecks = false
  var sync = false


  var logInfos = TreeMap[Long, LogInfo]()
  object log_mutex

  def delete(id:Long) = {
    log_mutex.synchronized {
      // We can't delete the current appender.
      if( currentAppender.position != id ) {
        logInfos.get(id).foreach { info =>
          onDelete(info.file)
          logInfos = logInfos.filterNot(_._1 == id)
        }
      }
    }
  }

  protected def onDelete(file:File) = {
    file.delete()
  }

  def checksum(data: Buffer): Int = {
    val checksum = new CRC32
    checksum.update(data.data, data.offset, data.length)
    (checksum.getValue & 0xFFFFFFFF).toInt
  }

  class LogAppender(file:File, position:Long) extends LogReader(file, position) {

    override def open = new RandomAccessFile(file, "rw")

    override def dispose() = {
      force
      super.dispose()
    }

    var append_offset = 0L
    val flushed_offset = new AtomicLong(0)

    def append_position = {
      position+append_offset
    }

    // set the file size ahead of time so that we don't have to sync the file
    // meta-data on every log sync.
    channel.position(logSize-1)
    channel.write(new Buffer(1).toByteBuffer)
    channel.force(true)
    channel.position(0)

    val write_buffer = new DataByteArrayOutputStream((BUFFER_SIZE)+BUFFER_SIZE)

    def force = {
      flush
      if(sync) {
        // only need to update the file metadata if the file size changes..
        channel.force(append_offset > logSize)
      }
    }

    /**
     * returns the offset position of the data record.
     */
    def append(id:Byte, data: Buffer): Long = this.synchronized {
      val record_position = append_position
      val data_length = data.length
      val total_length = LOG_HEADER_SIZE + data_length

      if( write_buffer.position() + total_length > BUFFER_SIZE ) {
        flush
      }

      val cs: Int = checksum(data)
//      trace("Writing at: "+record_position+" len: "+data_length+" with checksum: "+cs)

      if( total_length > BYPASS_BUFFER_SIZE ) {

        // Write the header and flush..
        write_buffer.writeByte(LOG_HEADER_PREFIX)
        write_buffer.writeByte(id)
        write_buffer.writeInt(cs)
        write_buffer.writeInt(data_length)

        append_offset += LOG_HEADER_SIZE
        flush

        // Directly write the data to the channel since it's large.
        val buffer = data.toByteBuffer
        val pos = append_offset+LOG_HEADER_SIZE
        flushed_offset.addAndGet(buffer.remaining)
        channel.write(buffer, pos)
        if( buffer.hasRemaining ) {
          throw new IOException("Short write")
        }
        append_offset += data_length

      } else {
        write_buffer.writeByte(LOG_HEADER_PREFIX)
        write_buffer.writeByte(id)
        write_buffer.writeInt(cs)
        write_buffer.writeInt(data_length)
        write_buffer.write(data.data, data.offset, data_length)
        append_offset += total_length
      }
      record_position
    }

    def flush = this.synchronized {
      if( write_buffer.position() > 0 ) {
        val buffer = write_buffer.toBuffer.toByteBuffer
        val pos = append_offset-buffer.remaining
        flushed_offset.addAndGet(buffer.remaining)
        channel.write(buffer, pos)
        if( buffer.hasRemaining ) {
          throw new IOException("Short write")
        }
        write_buffer.reset()
      }
    }

    override def check_read_flush(end_offset:Int) = {
      if( flushed_offset.get() < end_offset )  {
        this.synchronized {
          println("read flush")
          flush
        }
      }
    }

  }

  case class LogReader(file:File, position:Long) extends BaseRetained {

    def open = new RandomAccessFile(file, "r")

    val fd = open
    val channel = fd.getChannel

    override def dispose() {
      fd.close()
    }

    def check_read_flush(end_offset:Int) = {}

    def read(record_position:Long, length:Int) = {
      val offset = (record_position-position).toInt
      check_read_flush(offset+LOG_HEADER_SIZE+length)

      if(paranoidChecks) {

        val record = new Buffer(LOG_HEADER_SIZE+length)

        if( channel.read(record.toByteBuffer, offset) != record.length ) {
          throw new IOException("short record at position: "+record_position+" in file: "+file+", offset: "+offset)
        }

        val is = new DataByteArrayInputStream(record)
        val prefix = is.readByte()
        if( prefix != LOG_HEADER_PREFIX ) {
          throw new IOException("invalid record at position: "+record_position+" in file: "+file+", offset: "+offset)
        }

        val id = is.readByte()
        val expectedChecksum = is.readInt()
        val expectedLength = is.readInt()
        val data = is.readBuffer(length)

        // If your reading the whole record we can verify the data checksum
        if( expectedLength == length ) {
          if( expectedChecksum != checksum(data) ) {
            throw new IOException("checksum does not match at position: "+record_position+" in file: "+file+", offset: "+offset)
          }
        }

        data
      } else {
        val data = new Buffer(length)
        if( channel.read(data.toByteBuffer, offset+LOG_HEADER_SIZE) != data.length ) {
          throw new IOException("short record at position: "+record_position+" in file: "+file+", offset: "+offset)
        }
        data
      }
    }

    def read(record_position:Long) = {
      val offset = (record_position-position).toInt
      val header = new Buffer(LOG_HEADER_SIZE)
      channel.read(header.toByteBuffer, offset)
      val is = header.bigEndianEditor();
      val prefix = is.readByte()
      if( prefix != LOG_HEADER_PREFIX ) {
        // Does not look like a record.
        throw new IOException("invalid record position")
      }
      val id = is.readByte()
      val expectedChecksum = is.readInt()
      val length = is.readInt()
      val data = new Buffer(length)

      if( channel.read(data.toByteBuffer, offset+LOG_HEADER_SIZE) != length ) {
        throw new IOException("short record")
      }

      if(paranoidChecks) {
        if( expectedChecksum != checksum(data) ) {
          throw new IOException("checksum does not match")
        }
      }
      (id, data, record_position+LOG_HEADER_SIZE+length)
    }

    def check(record_position:Long):Option[Long] = {
      var offset = (record_position-position).toInt
      val header = new Buffer(LOG_HEADER_SIZE)
      channel.read(header.toByteBuffer, offset)
      val is = header.bigEndianEditor();
      val prefix = is.readByte()
      if( prefix != LOG_HEADER_PREFIX ) {
        return None // Does not look like a record.
      }
      val id = is.readByte()
      val expectedChecksum = is.readInt()
      val length = is.readInt()

      val chunk = new Buffer(1024*4)
      val chunkbb = chunk.toByteBuffer
      offset += LOG_HEADER_SIZE

      // Read the data in in chunks to avoid
      // OOME if we are checking an invalid record
      // with a bad record length
      val checksumer = new CRC32
      var remaining = length
      while( remaining > 0 ) {
        val chunkSize = remaining.min(1024*4);
        chunkbb.position(0)
        chunkbb.limit(chunkSize)
        channel.read(chunkbb, offset)
        if( chunkbb.hasRemaining ) {
          return None
        }
        checksumer.update(chunk.data, 0, chunkSize)
        offset += chunkSize
        remaining -= chunkSize
      }

      val checksum = ( checksumer.getValue & 0xFFFFFFFF).toInt
      if( expectedChecksum !=  checksum ) {
        return None
      }
      return Some(record_position+LOG_HEADER_SIZE+length)
    }

    def verifyAndGetEndPosition:Long = {
      var pos = position;
      val limit = position+channel.size()
      while(pos < limit) {
        check(pos) match {
          case Some(next) => pos = next
          case None => return pos
        }
      }
      pos
    }
  }

  def create_log_appender(position: Long) = {
    new LogAppender(next_log(position), position)
  }

  def create_appender(position: Long): Any = {
    log_mutex.synchronized {
      if(currentAppender!=null) {
        logInfos += position -> new LogInfo(currentAppender.file, currentAppender.position, currentAppender.append_offset)
      }
      currentAppender = create_log_appender(position)
      logInfos += position -> new LogInfo(currentAppender.file, position, 0)
    }
  }

  def open = {
    log_mutex.synchronized {
      logInfos = LevelDBClient.findSequenceFiles(directory, logSuffix).map { case (position,file) =>
        position -> LogInfo(file, position, file.length())
      }

      val appendPos = if( logInfos.isEmpty ) {
        0L
      } else {
        val (_, file) = logInfos.last
        val r = LogReader(file.file, file.position)
        try {
          val actualLength = r.verifyAndGetEndPosition
          val updated = file.copy(length = actualLength - file.position)
          logInfos = logInfos + (updated.position->updated)
          if( updated.file.length != file.length ) {
            // we need to truncate.
            using(new RandomAccessFile(file.file, "rw")) ( _.setLength(updated.length))
          }
          actualLength
        } finally {
          r.release()
        }
      }

      create_appender(appendPos)
    }
  }

  def close = {
    log_mutex.synchronized {
      currentAppender.release
    }
  }

  def appenderLimit = currentAppender.append_position
  def appenderStart = currentAppender.position

  def next_log(position:Long) = LevelDBClient.createSequenceFile(directory, position, logSuffix)

  def appender[T](func: (LogAppender)=>T):T= {
    try {
      func(currentAppender)
    } finally {
      currentAppender.flush
      log_mutex.synchronized {
        if ( currentAppender.append_offset >= logSize ) {
          currentAppender.release()
          onLogRotate()
          create_appender(currentAppender.append_position)
        }
      }
    }
  }

  var onLogRotate: ()=>Unit = ()=>{}

  private val reader_cache = new LRUCache[File, LogReader](100) {
    protected override def onCacheEviction(entry: Entry[File, LogReader]) = {
      entry.getValue.release()
    }
  }

  def logInfo(pos:Long) = log_mutex.synchronized(logInfos.range(0L, pos+1).lastOption.map(_._2))

  private def get_reader[T](record_position:Long)(func: (LogReader)=>T) = {

    val lookup = log_mutex.synchronized {
      val info = logInfo(record_position)
      info.map { info=>
        if(info.position == currentAppender.position) {
          currentAppender.retain()
          (info, currentAppender)
        } else {
          (info, null)
        }
      }
    }

    lookup.map { case (info, appender) =>
      val reader = if( appender!=null ) {
        // read from the current appender.
        appender
      } else {
        // Checkout a reader from the cache...
        reader_cache.synchronized {
          var reader = reader_cache.get(info.file)
          if(reader==null) {
            reader = LogReader(info.file, info.position)
            reader_cache.put(info.file, reader)
          }
          reader.retain()
          reader
        }
      }

      try {
        func(reader)
      } finally {
        reader.release
      }
    }
  }

  def read(pos:Long) = {
    get_reader(pos)(_.read(pos))
  }
  def read(pos:Long, length:Int) = {
    get_reader(pos)(_.read(pos, length))
  }

}
