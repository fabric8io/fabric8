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

import org.fusesource.mq.leveldb.util._

import collection.immutable.TreeMap

import org.apache.hadoop.fs.{FileSystem, Path}
import collection.mutable.HashMap
import org.fusesource.leveldbjni.internal.Util
import FileSupport._
import org.codehaus.jackson.map.ObjectMapper
import org.fusesource.hawtbuf.{ByteArrayOutputStream, Buffer}
import java.io._

/**
 *
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object JsonCodec {

  final val mapper: ObjectMapper = new ObjectMapper

  def decode[T](buffer: Buffer, clazz: Class[T]): T = {
    val original = Thread.currentThread.getContextClassLoader
    Thread.currentThread.setContextClassLoader(this.getClass.getClassLoader)
    try {
      return mapper.readValue(buffer.in, clazz)
    } finally {
      Thread.currentThread.setContextClassLoader(original)
    }
  }

  def decode[T](is: InputStream, clazz : Class[T]): T = {
    var original: ClassLoader = Thread.currentThread.getContextClassLoader
    Thread.currentThread.setContextClassLoader(this.getClass.getClassLoader)
    try {
      return JsonCodec.mapper.readValue(is, clazz)
    }
    finally {
      Thread.currentThread.setContextClassLoader(original)
    }
  }


  def encode(value: AnyRef): Buffer = {
    var baos = new ByteArrayOutputStream
    mapper.writeValue(baos, value)
    return baos.toBuffer
  }

}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object HALevelDBClient extends Log {

  val MANIFEST_SUFFIX = ".mf"
  val LOG_SUFFIX = LevelDBClient.LOG_SUFFIX
  val INDEX_SUFFIX = LevelDBClient.INDEX_SUFFIX


  def create_sequence_path(directory:Path, id:Long, suffix:String) = new Path(directory, ("%016x%s".format(id, suffix)))

  def find_sequence_status(fs:FileSystem, directory:Path, suffix:String) = {
    TreeMap((fs.listStatus(directory).flatMap { f =>
      val name = f.getPath.getName
      if( name.endsWith(suffix) ) {
        try {
          val base = name.stripSuffix(suffix)
          val position = java.lang.Long.parseLong(base, 16);
          Some(position -> f )
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
class HALevelDBClient(val store:HALevelDBStore) extends LevelDBClient(store) {
  import HALevelDBClient._

  case class SnapshotRef(files:Set[String], counter:LongCounter=new LongCounter)
  var fileRefs = HashMap[String, LongCounter]()
  var snapshotRefs = HashMap[Long, SnapshotRef]()

  def dfs = store.dfs
  def dfsDirectory = new Path(store.dfsDirectory)
  def dfsBlockSize = store.dfsBlockSize
  def dfsReplication = store.dfsReplication
  def remoteIndexPath = new Path(dfsDirectory, "index")

  override def start() = {
    retry {
      directory.mkdirs()
      dfs.mkdirs(dfsDirectory)
      downloadLogFiles
      dfs.mkdirs(remoteIndexPath)
      downloadIndexFiles
    }
    super.start()
  }

  override def snapshotIndex(sync: Boolean) = {
    val previous_snapshot = lastIndexSnapshotPos
    super.snapshotIndex(sync)
    // upload the snapshot to the dfs
    uploadIndexFiles(lastIndexSnapshotPos)
    snapshotRefs.get(previous_snapshot).foreach(_.counter.decrementAndGet())
    gcSnapshotRefs
  }

  // downloads missing log files...
  def downloadLogFiles {
    val log_files = find_sequence_status(dfs, dfsDirectory, LOG_SUFFIX)
    val downloads = log_files.flatMap( _ match {
      case (id, status) =>
        val target = LevelDBClient.create_sequence_file(directory, id, LOG_SUFFIX)
        // is it missing or does the size not match?
        if (!target.exists() || target.length() != status.getLen) {
          Some((id, status))
        } else {
          None
        }
    })
    if( !downloads.isEmpty ) {
      val total_size = downloads.foldLeft(0L)((a,x)=> a+x._2.getLen)
      info("Downloading %s in log files", total_size)
      downloads.foreach {
        case (id, status) =>
          val target = LevelDBClient.create_sequence_file(directory, id, LOG_SUFFIX)
          // is it missing or does the size not match?
          if (!target.exists() || target.length() != status.getLen) {
            using(dfs.open(status.getPath, 32*1024)) { is=>
              using(new FileOutputStream(target)) { os=>
                copy(is, os)
              }
            }
          }
      }
    }
  }

  // See if there is a more recent index that can be downloaded.
  def downloadIndexFiles {

    var manifests = HashMap[Long, IndexManifestDTO]()
    dfs.listStatus(remoteIndexPath).foreach { status =>
      val name = status.getPath.getName
      if( name endsWith MANIFEST_SUFFIX ) {
        val mf = using(dfs.open(status.getPath)) { is =>
          JsonCodec.decode(is, classOf[IndexManifestDTO])
        }
        manifests += (mf.snapshot_id -> mf)
      } else {
        fileRefs += (name-> new LongCounter())
      }
    }

    import collection.JavaConversions._

    // Remove invalid manifests..
    manifests = manifests.filter { case (_, mf)=>
      val files:Set[String] = asScalaSet(mf.files).toSet
      if( (fileRefs.keySet & files).isEmpty ) {
        dfs.delete(create_sequence_path(remoteIndexPath, mf.snapshot_id, MANIFEST_SUFFIX), true)
        false
      } else {
        true
      }
    }

    // Increment file reference counters.
    manifests.foreach { case (_, mf)=>
      mf.files.foreach { file =>
        fileRefs.get(file) match {
          case Some(file) => file.incrementAndGet()
          case None =>
            println("Not Referenced.")
        }
      }
    }

    // Remove un-referenced files.
    fileRefs = fileRefs.filter { case (name, counter)=>
      if( counter.get()==0 ) {
        dfs.delete(new Path(remoteIndexPath, name), true)
        false
      } else {
        true
      }
    }

    snapshotRefs.clear
    for( (key, value) <- manifests) {
      snapshotRefs.put(key, SnapshotRef(asScalaSet(value.files).toSet))
    }

    val local_snapshots = LevelDBClient.find_sequence_files(directory, INDEX_SUFFIX)

    // Lets download the last index snapshot..
    // TODO: don't download files that exist in the previous snapshot.
    manifests.lastOption.foreach { case (id, mf)=>

      // increment the ref..
      snapshotRefs.get(id).get.counter.incrementAndGet()
      tempIndexFile.recursiveDelete
      tempIndexFile.mkdirs

      mf.files.foreach { file =>
        val target = tempIndexFile / file

        // The file might be in a local snapshot already..
        local_snapshots.values.find(_.getName == file) match {
          case Some(f) =>
            // had it locally.. link it.
            Util.link(f, target)
          case None =>
            // download..
            using(dfs.open(new Path(remoteIndexPath, file), 32*1024)) { is=>
              using(new FileOutputStream(target)) { os=>
                copy(is, os)
              }
            }
        }
      }

      val current = tempIndexFile / "CURRENT"
      current.writeText(mf.current_manifest)

      // We got everything ok, now rename.
      tempIndexFile.renameTo(LevelDBClient.create_sequence_file(directory, mf.snapshot_id, INDEX_SUFFIX))
    }

    gcSnapshotRefs
  }

  def gcSnapshotRefs = {
    snapshotRefs = snapshotRefs.filter { case (id, ref)=>
      if (ref.counter.get()>0) {
        true
      } else {
        ref.files.foreach { file =>
          fileRefs.get(file).foreach { ref=>
            if( ref.decrementAndGet() == 0 ) {
              dfs.delete(new Path(remoteIndexPath, file), true)
              fileRefs.remove(file)
            }
          }
        }
        false
      }
    }
  }

  def uploadIndexFiles(snapshot_id:Long):Unit = {

    val source = LevelDBClient.create_sequence_file(directory, snapshot_id, INDEX_SUFFIX)
    try {

      // Build the new manifest..
      val mf = new IndexManifestDTO
      mf.snapshot_id = snapshot_id
      mf.current_manifest = (source / "CURRENT").readText()
      source.listFiles.foreach { file =>
        val name = file.getName
        if( name !="LOCK" && name !="CURRENT") {
          mf.files.add(name)
        }
      }

      import collection.JavaConversions._
      mf.files.foreach { file =>
        val refs = fileRefs.getOrElseUpdate(file, new LongCounter())
        if(refs.get()==0) {
          // Upload if not not yet on the remote.
          val target = new Path(remoteIndexPath, file)
          using(new FileInputStream(source / file)) { is=>
            using(dfs.create(target, true, 1024*32, dfsReplication.toShort, dfsBlockSize)) { os=>
              copy(is, os)
            }
          }
        }
        refs.incrementAndGet()
      }

      val target = create_sequence_path(remoteIndexPath, mf.snapshot_id, MANIFEST_SUFFIX)
      using(dfs.create(target, true, 1024*32, dfsReplication.toShort, dfsBlockSize)) { os=>
        JsonCodec.mapper.writeValue(os, mf)
      }

      val ref = SnapshotRef(asScalaSet(mf.files).toSet)
      ref.counter.incrementAndGet()
      snapshotRefs.put(snapshot_id, ref)

    } catch {
      case e: Exception =>
        warn(e, "Could not upload the index: " + e)
    }
  }



  // Override the log appender implementation so that it
  // stores the logs on the local and remote file systems.
  override def createLog = new RecordLog(directory, LOG_SUFFIX) {


    override protected def onDelete(file: File) = {
      super.onDelete(file)
      // also delete the file on the dfs.
      dfs.delete(new Path(dfsDirectory, file.getName), false)
    }

    override def create_log_appender(position: Long) = {
      new LogAppender(next_log(position), position) {

        val dfs_path = new Path(dfsDirectory, file.getName)
        val dfs_os = dfs.create(dfs_path, true, RecordLog.BUFFER_SIZE, dfsReplication.toShort, dfsBlockSize )

        override def flush = this.synchronized {
          if( write_buffer.position() > 0 ) {

            var buffer: Buffer = write_buffer.toBuffer
            // Write it to DFS..
            buffer.writeTo(dfs_os.asInstanceOf[OutputStream]);

            // Now write it to the local FS.
            val byte_buffer = buffer.toByteBuffer
            val pos = append_offset-byte_buffer.remaining
            flushed_offset.addAndGet(byte_buffer.remaining)
            channel.write(byte_buffer, pos)
            if( byte_buffer.hasRemaining ) {
              throw new IOException("Short write")
            }

            write_buffer.reset()
          }
        }

        override def force = {
          dfs_os.sync()
        }

        override def dispose() = {
          try {
            super.dispose()
          } finally {
            dfs_os.close()
          }
        }

      }
    }
  }
}
