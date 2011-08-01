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
import org.apache.activemq.apollo.broker.store.StoreBenchmarkSupport
import org.apache.activemq.apollo.broker.store.Store
import org.apache.activemq.apollo.util.FileSupport._

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class LevelDBStoreBenchmark extends StoreBenchmarkSupport {

  def create_store(flushDelay:Long):Store = {
    val rc = new LevelDBStore({
      val rc = new LevelDBStoreDTO
      rc.directory = basedir / "activemq-data"
      rc
    })
    rc.config.flush_delay = flushDelay
    rc
  }

}
