/**
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

package org.fusesource.fabric.apollo.broker.store.haleveldb

import collection.immutable.TreeMap
import org.apache.activemq.apollo.util.{MemoryPropertyEditor, IntCounter, OptionSupport, Log}
import org.fusesource.hawtbuf.Buffer

// import org.apache.activemq.apollo.broker.store.leveldb.dto._
import org.fusesource.fabric.apollo.broker.store.haleveldb.dto._
import org.apache.activemq.apollo.util.FileSupport._
import org.apache.activemq.apollo.web.resources.ViewHelper
import org.apache.hadoop.fs.{FileSystem, Path}
import java.io._
import collection.mutable.HashMap
import org.apache.activemq.apollo.dto.JsonCodec
import org.apache.activemq.apollo.broker.store.leveldb.{LevelDBClient, RecordLog}
import org.fusesource.leveldbjni.internal.Util

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

  override def config = store.config
  def fs = store.fs
  var directory_path:Path = _

  var dfs_block_size = 0L
  var dfs_replication = 0

  override def start() = {
    import OptionSupport._

    directory_path = new Path(config.dfs_directory)
    dfs_block_size = Option(config.dfs_block_size).map(MemoryPropertyEditor.parse(_)).getOrElse(((log_size / 4) + (1024*1024)))
    dfs_replication = config.dfs_replication.getOrElse(1)

    retry {
      fs.mkdirs(directory_path)
      download_log_files
      fs.mkdirs(remote_index_path)
      download_index_files
    }

    super.start()
  }

  override def snapshot_index = {
    val previous_snapshot = last_index_snapshot_pos
    super.snapshot_index
    // upload the snapshot to the dfs
    upload_index_files(last_index_snapshot_pos)
    snapshot_refs.get(previous_snapshot).foreach(_.counter.decrementAndGet())
    gc_snapshot_refs
  }

  // downloads missing log files...
  def download_log_files {
    val log_files = find_sequence_status(fs, directory_path, LOG_SUFFIX)
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
      info("Downloading %s in log files", ViewHelper.memory(total_size))
      downloads.foreach {
        case (id, status) =>
          val target = LevelDBClient.create_sequence_file(directory, id, LOG_SUFFIX)
          // is it missing or does the size not match?
          if (!target.exists() || target.length() != status.getLen) {
            using(fs.open(status.getPath, 32*1024)) { is=>
              using(new FileOutputStream(target)) { os=>
                copy(is, os)
              }
            }
          }
      }
    }
  }


  def remote_index_path = new Path(directory_path, "index")

  def decode[T](is: InputStream, `type` : Class[T]): T = {
    var original: ClassLoader = Thread.currentThread.getContextClassLoader
    Thread.currentThread.setContextClassLoader(classOf[JsonCodec].getClassLoader)
    try {
      return JsonCodec.mapper.readValue(is, `type`)
    }
    finally {
      Thread.currentThread.setContextClassLoader(original)
    }
  }

  case class SnapshotRef(files:Set[String], counter:IntCounter=new IntCounter)
  var file_refs = HashMap[String, IntCounter]()
  var snapshot_refs = Map[Long, SnapshotRef]()

  // See if there is a more recent index that can be downloaded.
  def download_index_files {

    var manifests = Map[Long, IndexManifestDTO]()
    fs.listStatus(remote_index_path).foreach { status =>
      val name = status.getPath.getName
      if( name endsWith MANIFEST_SUFFIX ) {
        val mf = using(fs.open(status.getPath)) { is =>
          decode(is, classOf[IndexManifestDTO])
        }
        manifests += (mf.snapshot_id -> mf)
      } else {
        file_refs += (name-> new IntCounter())
      }
    }

    import collection.JavaConversions._

    // Remove invalid manifests..
    manifests = manifests.filter { case (_, mf)=>
      val files:Set[String] = asScalaSet(mf.files).toSet
      if( (file_refs.keySet & files).isEmpty ) {
        fs.delete(create_sequence_path(remote_index_path, mf.snapshot_id, MANIFEST_SUFFIX), true)
        false
      } else {
        true
      }
    }

    // Increment file reference counters.
    manifests.foreach { case (_, mf)=>
      mf.files.foreach { file =>
        file_refs.get(file).get.incrementAndGet()
      }
    }

    // Remove un-referenced files.
    file_refs = file_refs.filter { case (name, counter)=>
      if( counter.get()==0 ) {
        fs.delete(new Path(remote_index_path, name), true)
        false
      } else {
        true
      }
    }

    snapshot_refs = manifests.mapValues(x=>SnapshotRef(asScalaSet(x.files).toSet))

    val local_snapshots = LevelDBClient.find_sequence_files(directory, INDEX_SUFFIX)

    // Lets download the last index snapshot..
    // TODO: don't download files that exist in the previous snapshot.
    manifests.lastOption.foreach { case (id, mf)=>

      // increment the ref..
      snapshot_refs.get(id).get.counter.incrementAndGet()

      temp_index_file.recursive_delete
      temp_index_file.mkdirs

      mf.files.foreach { file =>
        val target = temp_index_file / file

        // The file might be in a local snapshot already..
        local_snapshots.values.find(_.getName == file) match {
          case Some(f) =>
            // had it locally.. link it.
            Util.link(f, target)
          case None =>
            // download..
            using(fs.open(new Path(remote_index_path, file), 32*1024)) { is=>
              using(new FileOutputStream(target)) { os=>
                copy(is, os)
              }
            }
        }
      }

      val current = temp_index_file / "CURRENT"
      current.write_text(mf.current_manifest)

      // We got everything ok, now rename.
      temp_index_file.renameTo(LevelDBClient.create_sequence_file(directory, mf.snapshot_id, INDEX_SUFFIX))
    }

    gc_snapshot_refs
  }

  def gc_snapshot_refs = {
    snapshot_refs = snapshot_refs.filter { case (id, ref)=>
      if (ref.counter.get()>0) {
        true
      } else {
        ref.files.foreach { file =>
          file_refs.get(file).foreach { ref=>
            if( ref.decrementAndGet() == 0 ) {
              fs.delete(new Path(remote_index_path, file), true)
              file_refs.remove(file)
            }
          }
        }
        false
      }
    }
  }

  def upload_index_files(snapshot_id:Long):Unit = {

    val source = LevelDBClient.create_sequence_file(directory, snapshot_id, INDEX_SUFFIX)
    try {

      // Build the new manifest..
      val mf = new IndexManifestDTO
      mf.snapshot_id = snapshot_id
      mf.current_manifest = (source / "CURRENT").read_text()
      source.list_files.foreach { file =>
        val name = file.getName
        if( name !="LOCK" && name !="CURRENT") {
          mf.files.add(name)
        }
      }

      import collection.JavaConversions._
      mf.files.foreach { file =>
        val refs = file_refs.getOrElseUpdate(file, new IntCounter())
        if(refs.get()==0) {
          // Upload if not not yet on the remote.
          val target = new Path(remote_index_path, file)
          using(new FileInputStream(source / file)) { is=>
            using(fs.create(target, true, 1024*32, dfs_replication.toShort, dfs_block_size)) { os=>
              copy(is, os)
            }
          }
        }
        refs.incrementAndGet()
      }

      val target = create_sequence_path(remote_index_path, mf.snapshot_id, MANIFEST_SUFFIX)
      using(fs.create(target, true, 1024*32, dfs_replication.toShort, dfs_block_size)) { os=>
        JsonCodec.mapper.writeValue(os, mf)
      }

      val ref = SnapshotRef(asScalaSet(mf.files).toSet)
      ref.counter.incrementAndGet()
      snapshot_refs.put(snapshot_id, ref)

    } catch {
      case e: Exception =>
        warn(e, "Could not upload the index: " + e)
    }
  }

  // Override the log appender implementation so that it
  // stores the logs on the local and remote file systems.
  override def create_log = new RecordLog(directory, LOG_SUFFIX) {


    override protected def onDelete(file: File) = {
      super.onDelete(file)
      // also delete the file on the dfs.
      fs.delete(new Path(directory_path, file.getName), false)
    }

    override def create_log_appender(position: Long) = {
      new LogAppender(next_log(position), position) {

        val dfs_path = new Path(directory_path, file.getName)
        val dfs_os = fs.create(dfs_path, true, RecordLog.BUFFER_SIZE, dfs_replication.toShort, dfs_block_size )

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
