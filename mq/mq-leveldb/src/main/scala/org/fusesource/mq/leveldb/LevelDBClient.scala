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

import java.util.concurrent.locks.ReentrantReadWriteLock
import collection.immutable.TreeMap
import collection.mutable.{HashMap, ListBuffer}
import org.iq80.leveldb._

import org.fusesource.hawtdispatch._
import record.{CollectionKey, EntryKey, EntryRecord, CollectionRecord}
import util._
import java.util.Collections
import java.util.concurrent._
import org.fusesource.hawtbuf._
import java.io.{ObjectInputStream, ObjectOutputStream, File}
import org.apache.activemq.command.MessageId
import scala.Option._

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object LevelDBClient extends Log {

  final val THREAD_POOL_STACK_SIZE = System.getProperty("leveldb.thread.stack.size", "" + 1024 * 512).toLong
  final val THREAD_POOL: ThreadPoolExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 10, TimeUnit.SECONDS, new SynchronousQueue[Runnable], new ThreadFactory {
    def newThread(r: Runnable): Thread = {
      var rc: Thread = new Thread(null, r, "LevelDB Store Task", THREAD_POOL_STACK_SIZE)
      rc.setDaemon(true)
      return rc
    }
  }) {
    override def shutdown: Unit = {}
    override def shutdownNow = Collections.emptyList[Runnable]
  }

  final val DIRTY_INDEX_KEY = bytes(":dirty")
  final val LOG_REF_INDEX_KEY = bytes(":log-refs")
  final val TRUE = bytes("true")
  final val FALSE = bytes("false")

  final val COLLECTION_PREFIX = 'c'.toByte
  final val COLLECTION_PREFIX_ARRAY = Array(COLLECTION_PREFIX)
  final val ENTRY_PREFIX = 'e'.toByte
  final val ENTRY_PREFIX_ARRAY = Array(ENTRY_PREFIX)

  final val LOG_ADD_COLLECTION      = 1.toByte
  final val LOG_REMOVE_COLLECTION   = 2.toByte
  final val LOG_ADD_ENTRY           = 3.toByte
  final val LOG_REMOVE_ENTRY        = 4.toByte
  final val LOG_DATA                = 5.toByte
  final val LOG_TRACE               = 6.toByte

  final val LOG_SUFFIX  = ".log"
  final val INDEX_SUFFIX  = ".index"
  
  def objectEncode(value: AnyRef): Array[Byte] = {
    val baos = new ByteArrayOutputStream()
    val os = new ObjectOutputStream(baos);
    os.writeObject(value);
    os.close()
    val buffer = baos.toByteArray
    buffer
  }

  def objectDecode(value: Array[Byte]): AnyRef = {
    val bais = new ByteArrayInputStream(value)
    val is = new ObjectInputStream(bais);
    is.readObject();
  }

  def encodeCollectionRecord(v: CollectionRecord.Buffer) = v.toFramedByteArray
  def decodeCollectionRecord(data: Array[Byte]):CollectionRecord.Buffer = CollectionRecord.FACTORY.parseFramed(data)
  def encodeCollectionKeyRecord(v: CollectionKey.Buffer) = v.toFramedByteArray
  def decodeCollectionKeyRecord(data: Array[Byte]):CollectionKey.Buffer = CollectionKey.FACTORY.parseFramed(data)

  def encodeEntryRecord(v: EntryRecord.Buffer) = v.toFramedBuffer
  def decodeEntryRecord(data: Array[Byte]):EntryRecord.Buffer = EntryRecord.FACTORY.parseFramed(data)

  def encodeEntryKeyRecord(v: EntryKey.Buffer) = v.toFramedByteArray
  def decodeEntryKeyRecord(data: Array[Byte]):EntryKey.Buffer = EntryKey.FACTORY.parseFramed(data)

  def encodeLocator(pos:Long, len:Int):Array[Byte] = {
    val out = new DataByteArrayOutputStream(
      AbstractVarIntSupport.computeVarLongSize(pos)+
      AbstractVarIntSupport.computeVarIntSize(len)
    )
    out.writeVarLong(pos)
    out.writeVarInt(len)
    out.getData
  }
  def decodeLocator(bytes:Buffer):(Long,  Int) = {
    val in = new DataByteArrayInputStream(bytes)
    (in.readVarLong(), in.readVarInt())
  }
  def decodeLocator(bytes:Array[Byte]):(Long,  Int) = {
    val in = new DataByteArrayInputStream(bytes)
    (in.readVarLong(), in.readVarInt())
  }

  def encodeLong(a1:Long) = {
    val out = new DataByteArrayOutputStream(8)
    out.writeLong(a1)
    out.toBuffer
  }

  def encodeVLong(a1:Long):Array[Byte] = {
    val out = new DataByteArrayOutputStream(
      AbstractVarIntSupport.computeVarLongSize(a1)
    )
    out.writeVarLong(a1)
    out.getData
  }

  def decodeVLong(bytes:Array[Byte]):Long = {
    val in = new DataByteArrayInputStream(bytes)
    in.readVarLong()
  }

  def encodeLongKey(a1:Byte, a2:Long):Array[Byte] = {
    val out = new DataByteArrayOutputStream(9)
    out.writeByte(a1.toInt)
    out.writeLong(a2)
    out.getData
  }
  def decodeLongKey(bytes:Array[Byte]):(Byte, Long) = {
    val in = new DataByteArrayInputStream(bytes)
    (in.readByte(), in.readLong())
  }

  def decodeLong(bytes:Buffer):Long = {
    val in = new DataByteArrayInputStream(bytes)
    in.readLong()
  }
  def decodeLong(bytes:Array[Byte]):Long = {
    val in = new DataByteArrayInputStream(bytes)
    in.readLong()
  }

  def encodeEntryKey(a1:Byte, a2:Long, a3:Long):Array[Byte] = {
    val out = new DataByteArrayOutputStream(13)
    out.writeByte(a1.toInt)
    out.writeLong(a2)
    out.writeLong(a3)
    out.getData
  }

  def encodeEntryKey(a1:Byte, a2:Long, a3:Buffer):Array[Byte] = {
    val out = new DataByteArrayOutputStream(9+a3.length)
    out.writeByte(a1.toInt)
    out.writeLong(a2)
    out.write(a3)
    out.getData
  }
  
  def decodeEntryKey(bytes:Array[Byte]):(Byte, Long, Buffer) = {
    val in = new DataByteArrayInputStream(bytes)
    (in.readByte(), in.readLong(), in.readBuffer(in.available()))
  }

  final class RichDB(val db: DB) {

    val isPureJavaVersion = db.getClass.getName == "org.iq80.leveldb.impl.DbImpl"

    def getProperty(name:String) = db.getProperty(name)

    def getApproximateSizes(ranges:Range*) = db.getApproximateSizes(ranges:_*)

    def get(key:Array[Byte], ro:ReadOptions=new ReadOptions):Option[Array[Byte]] = {
      Option(db.get(key, ro))
    }

    def close:Unit = db.close()

    def delete(key:Array[Byte], wo:WriteOptions=new WriteOptions):Unit = {
      db.delete(key, wo)
    }

    def put(key:Array[Byte], value:Array[Byte], wo:WriteOptions=new WriteOptions):Unit = {
      db.put(key, value, wo)
    }

    def write[T](wo:WriteOptions=new WriteOptions)(func: WriteBatch=>T):T = {
      val updates = db.createWriteBatch()
      try {

        val rc=Some(func(updates))
        db.write(updates, wo)
        return rc.get
      } finally {
        updates.close();
      }
    }

    def snapshot[T](func: Snapshot=>T):T = {
      val snapshot = db.getSnapshot
      try {
        func(snapshot)
      } finally {
        snapshot.close()
      }
    }

    def cursorKeys(ro:ReadOptions=new ReadOptions)(func: Array[Byte] => Boolean): Unit = {
      val iterator = db.iterator(ro)
      iterator.seekToFirst();
      try {
        while( iterator.hasNext && func(iterator.peekNext.getKey) ) {
          iterator.next()
        }
      } finally {
        iterator.close();
      }
    }

    def cursorKeysPrefixed(prefix:Array[Byte], ro:ReadOptions=new ReadOptions)(func: Array[Byte] => Boolean): Unit = {
      val iterator = db.iterator(ro)
      iterator.seek(prefix);
      try {
        def check(key:Array[Byte]) = {
          key.startsWith(prefix) && func(key)
        }
        while( iterator.hasNext && check(iterator.peekNext.getKey) ) {
          iterator.next()
        }
      } finally {
        iterator.close();
      }
    }

    def cursorPrefixed(prefix:Array[Byte], ro:ReadOptions=new ReadOptions)(func: (Array[Byte],Array[Byte]) => Boolean): Unit = {
      val iterator = db.iterator(ro)
      iterator.seek(prefix);
      try {
        def check(key:Array[Byte]) = {
          key.startsWith(prefix) && func(key, iterator.peekNext.getValue)
        }
        while( iterator.hasNext && check(iterator.peekNext.getKey) ) {
          iterator.next()
        }
      } finally {
        iterator.close();
      }
    }

    def compare(a1:Array[Byte], a2:Array[Byte]):Int = {
      new Buffer(a1).compareTo(new Buffer(a2))
    }

    def cursorRangeKeys(startIncluded:Array[Byte], endExcluded:Array[Byte], ro:ReadOptions=new ReadOptions)(func: Array[Byte] => Boolean): Unit = {
      val iterator = db.iterator(ro)
      iterator.seek(startIncluded);
      try {
        def check(key:Array[Byte]) = {
          if ( compare(key,endExcluded) < 0) {
            func(key)
          } else {
            false
          }
        }
        while( iterator.hasNext && check(iterator.peekNext.getKey) ) {
          iterator.next()
        }
      } finally {
        iterator.close();
      }
    }

    def cursorRange(startIncluded:Array[Byte], endExcluded:Array[Byte], ro:ReadOptions=new ReadOptions)(func: (Array[Byte],Array[Byte]) => Boolean): Unit = {
      val iterator = db.iterator(ro)
      iterator.seek(startIncluded);
      try {
        def check(key:Array[Byte]) = {
          (compare(key,endExcluded) < 0) && func(key, iterator.peekNext.getValue)
        }
        while( iterator.hasNext && check(iterator.peekNext.getKey) ) {
          iterator.next()
        }
      } finally {
        iterator.close();
      }
    }

    def lastKey(prefix:Array[Byte], ro:ReadOptions=new ReadOptions): Option[Array[Byte]] = {
      val last = new Buffer(prefix).deepCopy().data
      if ( last.length > 0 ) {
        val pos = last.length-1
        last(pos) = (last(pos)+1).toByte
      }

      if(isPureJavaVersion) {
        // The pure java version of LevelDB does not support backward iteration.
        var rc:Option[Array[Byte]] = None
        cursorRangeKeys(prefix, last) { key=>
          rc = Some(key)
          true
        }
        rc
      } else {
        val iterator = db.iterator(ro)
        try {

          iterator.seek(last);
          if ( iterator.hasPrev ) {
            iterator.prev()
          } else {
            iterator.seekToLast()
          }

          if ( iterator.hasNext ) {
            val key = iterator.peekNext.getKey
            if(key.startsWith(prefix)) {
              Some(key)
            } else {
              None
            }
          } else {
            None
          }
        } finally {
          iterator.close();
        }
      }
    }
  }


  def bytes(value:String) = value.getBytes("UTF-8")

  import FileSupport._
  def createSequenceFile(directory:File, id:Long, suffix:String) = directory / ("%016x%s".format(id, suffix))

  def findSequenceFiles(directory:File, suffix:String):TreeMap[Long, File] = {
    TreeMap((directory.listFiles.flatMap { f=>
      if( f.getName.endsWith(suffix) ) {
        try {
          val base = f.getName.stripSuffix(suffix)
          val position = java.lang.Long.parseLong(base, 16);
          Some(position -> f)
        } catch {
          case e:NumberFormatException => None
        }
      } else {
        None
      }
    }): _* )
  }

}

/**
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class LevelDBClient(store: LevelDBStore) {

  import LevelDBClient._
  import FileSupport._

  val dispatchQueue = createQueue("leveldb")

  /////////////////////////////////////////////////////////////////////
  //
  // Helpers
  //
  /////////////////////////////////////////////////////////////////////

  def directory = store.directory

  /////////////////////////////////////////////////////////////////////
  //
  // Public interface used by the DBManager
  //
  /////////////////////////////////////////////////////////////////////

  def sync = store.sync;
  def verifyChecksums = store.verifyChecksums

  var log:RecordLog = _

  var index:RichDB = _
  var indexOptions:Options = _

  var lastIndexSnapshotPos:Long = _
  val snapshotRwLock = new ReentrantReadWriteLock(true)

  var factory:DBFactory = _
  val logRefs = HashMap[Long, LongCounter]()

  def dirtyIndexFile = directory / ("dirty"+INDEX_SUFFIX)
  def tempIndexFile = directory / ("temp"+INDEX_SUFFIX)
  def snapshotIndexFile(id:Long) = createSequenceFile(directory,id, INDEX_SUFFIX)

  def createLog: RecordLog = {
    new RecordLog(directory, LOG_SUFFIX)
  }

  var writeExecutor:ExecutorService = _

  def start() = {

    writeExecutor = Executors.newFixedThreadPool(1, new ThreadFactory() {
      def newThread(r: Runnable) = {
        val rc = new Thread(r, "LevelDB store io write")
        rc.setDaemon(true)
        rc
      }
    })

    val factoryNames = store.indexFactory
    factory = factoryNames.split("""(,|\s)+""").map(_.trim()).flatMap { name=>
      try {
        Some(this.getClass.getClassLoader.loadClass(name).newInstance().asInstanceOf[DBFactory])
      } catch {
        case x:Throwable => None
      }
    }.headOption.getOrElse(throw new Exception("Could not load any of the index factory classes: "+factoryNames.mkString(",")))

    if( factory.getClass.getName == "org.iq80.leveldb.impl.Iq80DBFactory") {
      warn("Using the pure java LevelDB implementation which is still experimental.  Production users should use the JNI based LevelDB implementation instead.")
    }

    indexOptions = new Options();
    indexOptions.createIfMissing(true);

    indexOptions.maxOpenFiles(store.indexMaxOpenFiles)
    indexOptions.blockRestartInterval(store.indexBlockRestartInterval)
    indexOptions.paranoidChecks(store.paranoidChecks)
    indexOptions.writeBufferSize(store.indexWriteBufferSize)
    indexOptions.blockSize(store.indexBlockSize)
    indexOptions.compressionType( store.indexCompression.toLowerCase match {
      case "snappy" => CompressionType.SNAPPY
      case "none" => CompressionType.NONE
      case _ => CompressionType.SNAPPY
    })

    indexOptions.cacheSize(store.indexCacheSize)
    indexOptions.logger(new Logger() {
      val LOG = Log(factory.getClass.getName)
      def log(msg: String) = LOG.debug(msg)
    })

    log = createLog
    log.writeBufferSize = store.logWriteBufferSize
    log.logSize = store.logSize
    log.onLogRotate = ()=> {
      // We snapshot the index every time we rotate the logs.
      writeExecutor {
        snapshotIndex(false)
      }
    }

    retry {
      log.open
    }

    // Find out what was the last snapshot.
    val snapshots = findSequenceFiles(directory, INDEX_SUFFIX)
    var lastSnapshotIndex = snapshots.lastOption
    lastIndexSnapshotPos = lastSnapshotIndex.map(_._1).getOrElse(0)

    // Only keep the last snapshot..
    snapshots.filterNot(_._1 == lastIndexSnapshotPos).foreach( _._2.recursiveDelete )
    tempIndexFile.recursiveDelete

    retry {

      // Delete the dirty indexes
      dirtyIndexFile.recursiveDelete
      dirtyIndexFile.mkdirs()

      lastSnapshotIndex.foreach { case (id, file) =>
        // Resume log replay from a snapshot of the index..
        try {
          file.listFiles.foreach { file =>
            file.linkTo(dirtyIndexFile / file.getName)
          }
        } catch {
          case e:Exception =>
            warn(e, "Could not recover snapshot of the index: "+e)
            lastSnapshotIndex  = None
        }
      }

      index = new RichDB(factory.open(dirtyIndexFile, indexOptions));
      try {
        loadLogRefs
        index.put(DIRTY_INDEX_KEY, TRUE)
        // Update the index /w what was stored on the logs..
        var pos = lastIndexSnapshotPos;

        try {
          while (pos < log.appenderLimit) {
            log.read(pos).map {
              case (kind, data, nextPos) =>
                kind match {
                  case LOG_ADD_COLLECTION =>
                    val record= decodeCollectionRecord(data)
                    index.put(encodeLongKey(COLLECTION_PREFIX, record.getKey), data)
                    
                  case LOG_REMOVE_COLLECTION =>
                    val record = decodeCollectionKeyRecord(data)
                    // Delete the entries in the collection.
                    index.cursorPrefixed(encodeLongKey(ENTRY_PREFIX, record.getKey), new ReadOptions) { (key, value)=>
                      val record = decodeEntryRecord(value)
                      val pos = if ( record.hasValueLocation ) {
                        Some(record.getValueLocation)
                      } else {
                        None
                      }
                      pos.foreach(logRefDecrement(_))
                      index.delete(key)
                      true
                    }
                    index.delete(data)

                  case LOG_ADD_ENTRY =>
                    val record = decodeEntryRecord(data)
                    index.put(encodeEntryKey(ENTRY_PREFIX, record.getCollectionKey, record.getEntryKey), data)

                    val pos = if ( record.hasValueLocation ) {
                      Some(record.getValueLocation)
                    } else {
                      None
                    }
                    pos.foreach(logRefIncrement(_))

                  case LOG_REMOVE_ENTRY =>
                    index.get(data, new ReadOptions).foreach { value=>
                      val record = decodeEntryRecord(value)
  
                      // Figure out which log file this message reference is pointing at..
                      val pos = if ( record.hasValueLocation ) {
                        Some(record.getValueLocation)
                      } else {
                        None
                      }
                      pos.foreach(logRefDecrement(_))
                      index.delete(data)
                    }

                  case LOG_DATA =>
                  case LOG_TRACE =>
                  case _ =>
                  // Skip unknown records like the RecordLog headers.
                }
                pos = nextPos
            }
          }
        }
        catch {
          case e:Throwable => e.printStackTrace()
        }


      } catch {
        case e:Throwable =>
          // replay failed.. good thing we are in a retry block...
          index.close
          throw e;
      }
    }
  }

  private def logRefDecrement(pos: Long) {
    log.logInfo(pos).foreach { logInfo =>
      logRefs.get(logInfo.position).foreach { counter =>
        if (counter.decrementAndGet() == 0) {
          logRefs.remove(logInfo.position)
        }
      }
    }
  }

  private def logRefIncrement(pos: Long) {
    log.logInfo(pos).foreach { logInfo =>
      logRefs.getOrElseUpdate(logInfo.position, new LongCounter()).incrementAndGet()
    }
  }

  private def storeLogRefs = {
    val map = new java.util.HashMap[Long, Long]
    logRefs.foreach{ case (k,v)=> map.put(k,v.get()) }
    index.put(LOG_REF_INDEX_KEY, objectEncode(map))
  }

  private def loadLogRefs = {
    logRefs.clear()
    index.get(LOG_REF_INDEX_KEY, new ReadOptions).foreach { value=>
      val javamap = objectDecode(value).asInstanceOf[java.util.Map[Long, Long]]
      collection.JavaConversions.mapAsScalaMap(javamap).foreach { case (k,v)=>
        logRefs.put(k, new LongCounter(v))
      }
    }
  }
  
  def stop() = {
    if( writeExecutor!=null ) {
      writeExecutor.shutdown
      writeExecutor.awaitTermination(60, TimeUnit.SECONDS)
      writeExecutor = null

      // this blocks until all io completes..
      // Suspend also deletes the index.
      suspend()

      if (log != null) {
        log.close
      }
      copyDirtyIndexToSnapshot
      log = null
    }
  }

  def usingIndex[T](func: =>T):T = {
    val lock = snapshotRwLock.readLock();
    lock.lock()
    try {
      func
    } finally {
      lock.unlock()
    }
  }

  def retryUsingIndex[T](func: =>T):T = retry(usingIndex( func ))

  /**
   * TODO: expose this via management APIs, handy if you want to
   * do a file system level snapshot and want the data to be consistent.
   */
  def suspend() = {
    // Make sure we are the only ones accessing the index. since
    // we will be closing it to create a consistent snapshot.
    snapshotRwLock.writeLock().lock()

    // Close the index so that it's files are not changed async on us.
    storeLogRefs
    index.put(DIRTY_INDEX_KEY, FALSE, new WriteOptions().sync(true))
    index.close
  }

  /**
   * TODO: expose this via management APIs, handy if you want to
   * do a file system level snapshot and want the data to be consistent.
   */
  def resume() = {
    // re=open it..
    retry {
      index = new RichDB(factory.open(dirtyIndexFile, indexOptions));
      index.put(DIRTY_INDEX_KEY, TRUE)
    }
    snapshotRwLock.writeLock().unlock()
  }

  def copyDirtyIndexToSnapshot {
    if( log.appenderLimit == lastIndexSnapshotPos  ) {
      // no need to snapshot again...
      return
    }

    // Where we start copying files into.  Delete this on
    // restart.
    val tmpDir = tempIndexFile
    tmpDir.mkdirs()

    try {

      // Hard link all the index files.
      dirtyIndexFile.listFiles.foreach { file =>
        file.linkTo(tmpDir / file.getName)
      }

      // Rename to signal that the snapshot is complete.
      val newSnapshotIndexPos = log.appenderLimit
      tmpDir.renameTo(snapshotIndexFile(newSnapshotIndexPos))
      snapshotIndexFile(lastIndexSnapshotPos).recursiveDelete
      lastIndexSnapshotPos = newSnapshotIndexPos

    } catch {
      case e: Exception =>
        // if we could not snapshot for any reason, delete it as we don't
        // want a partial check point..
        warn(e, "Could not snapshot the index: " + e)
        tmpDir.recursiveDelete
    }
  }

  def snapshotIndex(sync:Boolean=false):Unit = {
    suspend()
    try {
      if( sync ) {
        log.currentAppender.sync
      }
      if( log.appenderLimit == lastIndexSnapshotPos  ) {
        // no need to snapshot again...
        return
      }
      copyDirtyIndexToSnapshot
    } finally {
      resume()
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
        if ( !store.isStarted ) {
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
    suspend()
    try{
      log.close
      directory.listFiles.foreach(_.recursiveDelete)
    } finally {
      retry {
        log.open
      }
      resume()
    }
  }

  def addCollection(record: CollectionRecord.Buffer) = {
    val key = encodeLongKey(COLLECTION_PREFIX, record.getKey)
    val value = record.toFramedBuffer
    retryUsingIndex {
      log.appender { appender =>
        appender.append(LOG_ADD_COLLECTION, value)
        index.put(key, value.toByteArray)
      }
    }
  }

  def getLogAppendPosition = log.appenderLimit

  def listCollections: Seq[(Long, CollectionRecord.Buffer)] = {
    val rc = ListBuffer[(Long, CollectionRecord.Buffer)]()
    retryUsingIndex {
      val ro = new ReadOptions
      ro.verifyChecksums(verifyChecksums)
      ro.fillCache(false)
      index.cursorPrefixed(COLLECTION_PREFIX_ARRAY, ro) { (key, value) =>
        rc.append(( decodeLongKey(key)._2, CollectionRecord.FACTORY.parseFramed(value) ))
        true // to continue cursoring.
      }
    }
    rc
  }

  def removeCollection(collectionKey: Long) = {
    val key = encodeLongKey(COLLECTION_PREFIX, collectionKey)
    val value = encodeVLong(collectionKey)
    val entryKeyPrefix = encodeLongKey(ENTRY_PREFIX, collectionKey)
    retryUsingIndex {
      log.appender { appender =>
        appender.append(LOG_REMOVE_COLLECTION, new Buffer(value))
      }

      val ro = new ReadOptions
      ro.fillCache(false)
      ro.verifyChecksums(verifyChecksums)
      index.cursorPrefixed(entryKeyPrefix, ro) { (key, value)=>
        val record = decodeEntryRecord(value)
        val pos = if ( record.hasValueLocation ) {
          Some(record.getValueLocation)
        } else {
          None
        }
        pos.foreach(logRefDecrement(_))
        index.delete(key)
        true
      }
      index.delete(key)
    }
  }

  def collectionEmpty(collectionKey: Long) = {
    val key = encodeLongKey(COLLECTION_PREFIX, collectionKey)
    val value = encodeVLong(collectionKey)
    val entryKeyPrefix = encodeLongKey(ENTRY_PREFIX, collectionKey)

    retryUsingIndex {
      index.get(key).foreach { collectionData =>
        log.appender { appender =>
          appender.append(LOG_REMOVE_COLLECTION, new Buffer(value))
          appender.append(LOG_ADD_COLLECTION, new Buffer(collectionData))
        }

        val ro = new ReadOptions
        ro.fillCache(false)
        ro.verifyChecksums(verifyChecksums)
        index.cursorPrefixed(entryKeyPrefix, ro) { (key, value)=>
          val record = decodeEntryRecord(value)
          val pos = if ( record.hasValueLocation ) {
            Some(record.getValueLocation)
          } else {
            None
          }
          pos.foreach(logRefDecrement(_))
          index.delete(key)
          true
        }
      }
    }
  }

  def queueCursor(collectionKey: Long, cursorPosition:Long)(func: (QueueEntryRecord)=>Boolean) = {
    collectionCursor(collectionKey, encodeLong(cursorPosition)) { value =>
      val seq = decodeLong(value.getEntryKey)
      val msgId = new MessageId(value.getMeta.ascii().toString)
      msgId.setEntryLocator((collectionKey, seq))
      msgId.setDataLocator((value.getValueLocation, value.getValueLength))
      func(QueueEntryRecord(msgId, collectionKey, seq))
    }
  }

  def collectionCursor(collectionKey: Long, cursorPosition:Buffer)(func: (EntryRecord.Buffer)=>Boolean) = {
    val ro = new ReadOptions
    ro.fillCache(true)
    ro.verifyChecksums(verifyChecksums)
    val start = encodeEntryKey(ENTRY_PREFIX, collectionKey, cursorPosition)
    val end = encodeLongKey(ENTRY_PREFIX, collectionKey+1)
    retryUsingIndex {
      index.cursorRange(start, end, ro) { case (key, value) =>
        func(EntryRecord.FACTORY.parseFramed(value))
      }
    }
  }

  def collectionSize(collectionKey: Long) = {
    val entryKeyPrefix = encodeLongKey(ENTRY_PREFIX, collectionKey)
    var count = 0
    retryUsingIndex {
      val ro = new ReadOptions
      ro.fillCache(false)
      ro.verifyChecksums(verifyChecksums)
      index.cursorKeysPrefixed(entryKeyPrefix, ro) { key =>
        count += 1
        true
      }
    }
    count
  }

  def collectionIsEmpty(collectionKey: Long) = {
    val entryKeyPrefix = encodeLongKey(ENTRY_PREFIX, collectionKey)
    var empty = true
    retryUsingIndex {
      val ro = new ReadOptions
      ro.fillCache(false)
      ro.verifyChecksums(verifyChecksums)
      index.cursorKeysPrefixed(entryKeyPrefix, ro) { key =>
        empty = false
        false
      }
    }
    empty
  }

  def store(uows: Seq[DBManager#DelayableUOW]) {
    retryUsingIndex {
      log.appender { appender =>

        var syncNeeded = false
        index.write() { batch =>

          uows.foreach { uow =>

            uow.actions.foreach { case (msg, action) =>
              val messageRecord = action.messageRecord
              var pos = -1L
              var dataLocator:(Long, Int) = null

              if (messageRecord != null) {
                pos = appender.append(LOG_DATA, messageRecord.data)
                dataLocator = (pos, messageRecord.data.length)
                messageRecord.id.setDataLocator(dataLocator);
              }


              action.dequeues.foreach { entry =>
                val keyLocation = entry.id.getEntryLocator.asInstanceOf[(Long, Long)]
                val key = encodeEntryKey(ENTRY_PREFIX, keyLocation._1, keyLocation._2)
                val locator = entry.id.getDataLocator.asInstanceOf[(Long,Int)]


                if( dataLocator ==null ) {
                  dataLocator = locator
                } else {
                  assert(locator == dataLocator)
                }

                appender.append(LOG_REMOVE_ENTRY, new Buffer(key))
                batch.delete(key)

                logRefDecrement(dataLocator._1)
              }

              action.enqueues.foreach { entry =>

                val key = encodeEntryKey(ENTRY_PREFIX, entry.queueKey, entry.queueSeq)

                assert(dataLocator!=null)
                entry.id.setDataLocator(dataLocator)

                val record = new EntryRecord.Bean()
                record.setCollectionKey(entry.queueKey)
                record.setMeta(new AsciiBuffer(entry.id.toString))
                record.setEntryKey(new Buffer(key, 9, 8))
                record.setValueLocation(dataLocator._1)
                record.setValueLength(dataLocator._2)

                val encoded = encodeEntryRecord(record.freeze())
                appender.append(LOG_ADD_ENTRY, encoded)
                batch.put(key, encoded.toByteArray)
                logRefIncrement(dataLocator._1)
              }
            }
            if( !syncNeeded && uow.syncNeeded ) {
              syncNeeded = true
            }
          }
        }
        if( syncNeeded && sync ) {
          appender.flush
          appender.sync
        }
      }
    }
  }


  def getQueueEntries(collectionKey: Long, firstSeq:Long, lastSeq:Long): Seq[QueueEntryRecord] = {
    getCollectionEntries(collectionKey, firstSeq, lastSeq).map { case (key, value) =>
      val seq = key.bigEndianEditor().readLong()
      val msgId = new MessageId(value.getMeta.ascii().toString)
      msgId.setEntryLocator((collectionKey, seq))
      msgId.setDataLocator((value.getValueLocation, value.getValueLength))
      QueueEntryRecord(msgId, collectionKey, seq)
    }
  }
  
  def getCollectionEntries(collectionKey: Long, firstSeq:Long, lastSeq:Long): Seq[(Buffer, EntryRecord.Buffer)] = {
    var rc = ListBuffer[(Buffer, EntryRecord.Buffer)]()
    val ro = new ReadOptions
    ro.verifyChecksums(verifyChecksums)
    ro.fillCache(true)
    retryUsingIndex {
      index.snapshot { snapshot =>
        ro.snapshot(snapshot)
        val start = encodeEntryKey(ENTRY_PREFIX, collectionKey, firstSeq)
        val end = encodeEntryKey(ENTRY_PREFIX, collectionKey, lastSeq+1)
        index.cursorRange( start, end, ro ) { (key, value) =>
          val (_, _, seq) = decodeEntryKey(key)
          rc.append((seq, EntryRecord.FACTORY.parseFramed(value)))
          true
        }
      }
    }
    rc
  }

  def getLastQueueEntrySeq(collectionKey: Long): Long = {
    getLastCollectionEntryKey(collectionKey).map(_.bigEndianEditor().readLong()).getOrElse(0L)
  }

  def getLastCollectionEntryKey(collectionKey: Long): Option[Buffer] = {
    retryUsingIndex {
      index.lastKey(encodeLongKey(ENTRY_PREFIX, collectionKey)).map( decodeEntryKey(_)._3 )
    }
  }

  def gc:Unit = {
    lastIndexSnapshotPos
    val emptyJournals = log.logInfos.keySet.toSet -- logRefs.keySet

    // We don't want to delete any journals that the index has not snapshot'ed or
    // the the
    val deleteLimit = log.logInfo(lastIndexSnapshotPos).map(_.position).
          getOrElse(lastIndexSnapshotPos).min(log.appenderStart)

    emptyJournals.foreach { id =>
      if ( id < deleteLimit ) {
        log.delete(id)
      }
    }
  }

}
