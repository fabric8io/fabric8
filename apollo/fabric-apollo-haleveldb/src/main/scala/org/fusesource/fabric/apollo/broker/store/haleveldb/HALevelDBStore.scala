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

import dto.HALevelDBStoreDTO
import org.apache.activemq.apollo.util._
import org.apache.activemq.apollo.broker.store.leveldb.LevelDBStore
import org.apache.activemq.apollo.broker.store.leveldb.dto._
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.conf.Configuration
import org.fusesource.hawtdispatch._

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object HALevelDBStore extends Log {
}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class HALevelDBStore(override val config:HALevelDBStoreDTO) extends LevelDBStore(config) {

  var fs:FileSystem = _
  var fs_url:String = _

  override def create_client = new HALevelDBClient(this)


  override def store_kind = "haleveldb"

  override def toString = {
    super.toString + Option(fs_url).map(" => "+_).getOrElse("")
  }

  override protected def _start(on_completed: Runnable) = {
    if(fs==null) {
      Thread.currentThread().setContextClassLoader(getClass.getClassLoader)
      val dfs_config = new Configuration()
      dfs_config.set("fs.hdfs.impl.disable.cache", "true")
      dfs_config.set("fs.file.impl.disable.cache", "true")
      Option(config.dfs_config).foreach(dfs_config.addResource(_))
      Option(config.dfs_url).foreach(dfs_config.set("fs.default.name", _))
      fs_url = dfs_config.get("fs.default.name")
      fs = FileSystem.get(dfs_config)
    }
    super._start(on_completed)
  }

  override protected def _stop(on_completed: Runnable) = {
    super._stop(^{
      if(fs!=null){
        fs.close()
      }
      on_completed.run()
    })
  }
}
