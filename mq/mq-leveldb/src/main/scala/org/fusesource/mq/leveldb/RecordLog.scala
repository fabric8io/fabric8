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

import java.{lang=>jl}
import java.{util=>ju}

import java.io._
import java.util.zip.CRC32
import java.util.Map.Entry
import java.util.Arrays
import collection.mutable.{HashMap, HashSet}
import collection.immutable.TreeMap
import java.util.concurrent.atomic.AtomicLong
import java.nio.ByteBuffer
import org.apache.activemq.util.LRUCache
import util.FileSupport._
import org.fusesource.hawtbuf.{Buffer, DataByteArrayOutputStream}

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

case class RecordLog(directory: File, logSuffix:String) {
  import RecordLog._

  directory.mkdirs()

  var writeBufferSize = 1024 * 1024 * 4
  var logSize = 1024 * 1024 * 100
  var currentAppender:LogAppender = _

  case class LogInfo(file:File, position:Long, length:AtomicLong) {
    def limit = position+length.get
  }

  var logInfos = TreeMap[Long, LogInfo]()
  object logMutex

  def delete(id:Long) = {
    logMutex.synchronized {
      // We can't delete the current appender.
      if( currentAppender.start != id ) {
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

  class LogAppender(val file:File, val start:Long) {

    val fos = new FileOutputStream(file)
    def channel = fos.getChannel
    def os:OutputStream = fos

    val outbound = new DataByteArrayOutputStream()

    var batchLength = 0
    val length = new AtomicLong(0)
    var limit = start

    // set the file size ahead of time so that we don't have to sync the file
    // meta-data on every log sync.
    channel.position(logSize)
    channel.write(ByteBuffer.wrap(Array(0.toByte)))
    channel.force(true)
    channel.position(0)

    def sync = {
      // only need to update the file metadata if the file size changes..
      channel.force(length.get() > logSize)
    }

    def flush {
      if( batchLength!= 0 ) {

        // Update the buffer with the log header info now that we
        // can calc the length and checksum info
        val buffer = outbound.toBuffer

        assert(buffer.length()==LOG_HEADER_SIZE+batchLength)

        outbound.reset()
        outbound.write(LOG_HEADER_PREFIX)

        val checksum = new CRC32
        checksum.update(buffer.data, buffer.offset + LOG_HEADER_SIZE, buffer.length - LOG_HEADER_SIZE)
        var actualChecksum = (checksum.getValue & 0xFFFFFFFF).toInt

        outbound.writeInt( actualChecksum )
        outbound.writeInt(batchLength)

        // Actually write the record to the file..
        buffer.writeTo(os);

        length.addAndGet( buffer.length() )

        batchLength = 0
        outbound.reset()
      }
    }

    /**
     * returns the offset position of the data record.
     */
    def append(id:Byte, data: Buffer): Long = {
      assert(id != LOG_HEADER_PREFIX(0))
      if( batchLength!=0 && (batchLength + data.length > writeBufferSize) ) {
        flush
      }
      if( batchLength==0 ) {
        // first data pos record is offset by the log header.
        outbound.skip(LOG_HEADER_SIZE);
        limit += LOG_HEADER_SIZE
      }
      val rc = limit;

      val start = outbound.position
      outbound.writeByte(id);
      outbound.writeInt(data.length)
      outbound.write(data);
      val count = outbound.position - start

      limit += count
      batchLength += count
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

    def read(pos:Long) = this.synchronized {
      is.seek(pos-start)
      val id = is.read()
      if( id == LOG_HEADER_PREFIX(0) ) {
        (id, null, pos+LOG_HEADER_SIZE)
      } else {
        val length = is.readInt()
        val data = new Array[Byte](length)
        is.readFully(data)
        (id, data, start+is.getFilePointer)
      }
    }

    def read(pos:Long, length:Int) = this.synchronized {
      is.seek((pos-start)+5)
      val data = new Array[Byte](length)
      is.readFully(data)
      data
    }

    def close = this.synchronized {
      is.close()
    }

    def nextPosition(verifyChecksums:Boolean=true):Long = this.synchronized {
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
          val expectedChecksum = is.readInt();

          val length = is.readInt();
          if (verifyChecksums) {
            val data = new Array[Byte](length)
            is.readFully(data)

            val checksum = new CRC32
            checksum.update(data)
            val actualChecksum = (checksum.getValue & 0xFFFFFFFF).toInt

            if( expectedChecksum != actualChecksum ) {
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

  def createLogAppender(position: Long) = {
    new LogAppender(nextLog(position), position)
  }

  def createAppender(position: Long): Any = {
    currentAppender = createLogAppender(position)
    logMutex.synchronized {
      logInfos += position -> new LogInfo(currentAppender.file, position, currentAppender.length)
    }
  }

  def open = {
    logMutex.synchronized {
      logInfos = LevelDBClient.findSequenceFiles(directory, logSuffix).map { case (position,file) =>
        position -> LogInfo(file, position, new AtomicLong(file.length()))
      }

      val appendPos = if( logInfos.isEmpty ) {
        0L
      } else {
        val (_, file) = logInfos.last
        val r = LogReader(file.file, file.position)
        try {
          val rc = r.nextPosition()
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

      createAppender(appendPos)
    }
  }
  def close = {
    logMutex.synchronized {
      currentAppender.close
    }
  }

  def appenderLimit = currentAppender.limit
  def appenderStart = currentAppender.start

  def nextLog(position:Long) = LevelDBClient.createSequenceFile(directory, position, logSuffix)

  def appender[T](func: (LogAppender)=>T):T= {
    try {
      func(currentAppender)
    } finally {
      currentAppender.flush
      logMutex.synchronized {
        if ( currentAppender.length.get >= logSize ) {
          currentAppender.close
          onLogRotate()
          createAppender(currentAppender.limit)
        }
      }
    }
  }

  var onLogRotate: ()=>Unit = ()=>{}

  var nextReaderId = 0L

  def nextReaderIdIncrement = {
    val rc = nextReaderId
    nextReaderId += 1
    rc
  }

  val readerCacheFiles = new HashMap[File, HashSet[Long]];
  val readerCacheReaders = new LRUCache[Long, LogReader](100) {
    protected override def onCacheEviction(entry: Entry[Long, LogReader]) = {
      var key = entry.getKey
      var value = entry.getValue
      value.close

      val set = readerCacheFiles.get(value.file).get
      set.remove(key)
      if( set.isEmpty ) {
        readerCacheFiles.remove(value.file)
      }
    }
  }

  def logInfo(pos:Long) = logMutex.synchronized(logInfos.range(0L, pos+1).lastOption.map(_._2))

  private def getReader[T](pos:Long)(func: (LogReader)=>T) = {
    val info = logInfo(pos)
    info.map { info =>
      // Checkout a reader from the cache...
      val (set, readerId, reader) = readerCacheFiles.synchronized {
        var set = readerCacheFiles.getOrElseUpdate(info.file, new HashSet);
        if( set.isEmpty ) {
          val readerId = nextReaderIdIncrement
          val reader = new LogReader(info.file, info.position)
          set.add(readerId)
          readerCacheReaders.put(readerId, reader)
          (set, readerId, reader)
        } else {
          val readerId = set.head
          set.remove(readerId)
          (set, readerId, readerCacheReaders.get(readerId))
        }
      }

      try {
        func(reader)
      } finally {
        // check him back in..
        readerCacheFiles.synchronized {
          set.add(readerId)
        }
      }
    }
  }

  def read(pos:Long) = {
    getReader(pos)(_.read(pos))
  }
  def read(pos:Long, length:Int) = {
    getReader(pos)(_.read(pos, length))
  }

}
