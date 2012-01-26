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
import org.apache.activemq.apollo.broker.store._
import org.apache.activemq.apollo.util.FileSupport._

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class HALevelDBStoreTest extends StoreFunSuiteSupport with HdfsServerMixin {

  def create_store(flushDelay:Long):Store = {
    val rc = new HALevelDBStore({
      val rc = new HALevelDBStoreDTO
      rc.dfs_directory = "localhost"
      rc.directory = basedir / "target" / "apollo-data"
      rc.flush_delay = flushDelay
      rc
    })
    rc.fs = fs
    rc
  }

}
