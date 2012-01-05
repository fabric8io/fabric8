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

import java.util.zip.CRC32
import java.util.Map.Entry
import collection.immutable.TreeMap
import org.apache.activemq.util.LRUCache
import util.FileSupport._
import java.nio.channels.FileChannel
import org.fusesource.hawtdispatch.BaseRetained
import org.fusesource.hawtbuf.{BufferEditor, Buffer, DataByteArrayOutputStream}
import java.util.concurrent.atomic.AtomicLong
import java.io._

object RecordLog {

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
}

case class RecordLog(directory: File, logSuffix:String) {
  import RecordLog._

  directory.mkdirs()

  var logSize = 1024 * 1024 * 100
  var currentAppender:LogAppender = _
  var paranoidChecks = false

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

  def checksum(data: Buffer): Int = {
    val checksum = new CRC32
    checksum.update(data.data, data.offset, data.length)
    (checksum.getValue & 0xFFFFFFFF).toInt
  }

  class LogAppender(val file:File, val start:Long) {
    val fos = new FileOutputStream(file)
    def channel = fos.getChannel

    val length = new AtomicLong(0)
    def limit = start+length.get()

    // set the file size ahead of time so that we don't have to sync the file
    // meta-data on every log sync.
    channel.position(logSize-1)
    channel.write(new Buffer(1).toByteBuffer)
    channel.force(true)
    channel.position(0)

    val os = new DataOutputStream(new BufferedOutputStream(fos))
   
    def sync = {
      // only need to update the file metadata if the file size changes..
      flush
      channel.force(length.get() > logSize)
    }

    def flush = os.flush()

    /**
     * returns the offset position of the data record.
     */
    def append(id:Byte, data: Buffer): Long = {
      val rc = limit
      val data_length = data.length
      val total_length = LOG_HEADER_SIZE + data_length

      // Write the header.
      os.writeByte(LOG_HEADER_PREFIX)
      os.writeByte(id)
      os.writeInt(checksum(data))
      os.writeInt(data_length)
      os.write(data.data, data.offset, data_length)

      length.addAndGet(total_length)
      rc
    }

    def close = {
      sync
      fos.close()
    }
  }

  case class LogReader(file:File, start:Long) extends BaseRetained {
    val is = new RandomAccessFile(file, "r")
    def channel = is.getChannel

    override def dispose() {
      is.close()
    }
    
    def read(pos:Long, length:Int) = this.synchronized {
      val offset = (pos-start).toInt
      if(paranoidChecks) {
        val data = new Buffer(LOG_HEADER_SIZE+length)
        if( channel.read(data.toByteBuffer, offset) != data.length ) {
          throw new IOException("short record")
        }

        val is:BufferEditor = data.bigEndianEditor()
        val prefix = is.readByte()
        if( prefix != LOG_HEADER_PREFIX ) {
          // Does not look like a record.
          throw new IOException("invalid record position")
        }

        val id = is.readByte()
        val expectedChecksum = is.readInt()
        val expectedLength = is.readInt()

        // If your reading the whole record we can verify the data checksum
        if( expectedLength == length ) {
          if( expectedChecksum != checksum(data) ) {
            throw new IOException("checksum does not match")
          }
        }
        data
      } else {
        val data = new Buffer(length)
        if( channel.read(data.toByteBuffer, offset+LOG_HEADER_SIZE) != data.length ) {
          throw new IOException("short record")
        }
        data
      }
    }

    def read(pos:Long) = this.synchronized {
      val offset = (pos-start).toInt
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
      (id, data, pos+LOG_HEADER_SIZE+length)
    }

    def check(pos:Long):Option[Long] = this.synchronized {
      var offset = (pos-start).toInt
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
      return Some(pos+LOG_HEADER_SIZE+length)
    }

    def verifyAndGetEndPosition:Long = this.synchronized {
      var pos = start;
      val limit = start+channel.size()
      while(pos < limit) {
        check(pos) match {
          case Some(next) => pos = next
          case None => return pos
        }
      }
      pos
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
          val rc = r.verifyAndGetEndPosition
          file.length.set(rc - file.position)
          if( file.file.length != file.length.get() ) {
            // we need to truncate.
            using(new RandomAccessFile(file.file, "rw")) ( _.setLength(file.length.get()) )
          }
          rc
        } finally {
          r.release()
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

  val readerCacheReaders = new LRUCache[File, LogReader](100) {
    protected override def onCacheEviction(entry: Entry[File, LogReader]) = {
      entry.getValue.release()
    }
  }

  def logInfo(pos:Long) = logMutex.synchronized(logInfos.range(0L, pos+1).lastOption.map(_._2))

  private def getReader[T](pos:Long)(func: (LogReader)=>T) = {
    
    logInfo(pos).map { info =>

      // Checkout a reader from the cache...
      val reader = readerCacheReaders.synchronized {
        var reader = readerCacheReaders.get(info.file)
        if(reader==null) {
          reader = LogReader(info.file, info.position)
          readerCacheReaders.put(info.file, reader)
        }
        reader
      }

      reader.retain()
      try {
        func(reader)
      } finally {
        reader.release
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
