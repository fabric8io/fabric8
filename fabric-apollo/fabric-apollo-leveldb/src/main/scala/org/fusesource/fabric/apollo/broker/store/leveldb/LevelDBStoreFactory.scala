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
import org.apache.activemq.apollo.broker.store.StoreFactory
import org.apache.activemq.apollo.dto.StoreDTO
import org.apache.activemq.apollo.util._

/**
 * <p>
 * Hook to use a HawtDBStore when a HawtDBStoreDTO is
 * used in a broker configuration.
 * </p>
 * <p>
 * This class is discovered using the following resource file:
 * <code>META-INF/services/org.apache.activemq.apollo/stores</code>
 * </p>
 * 
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class LevelDBStoreFactory extends StoreFactory {
  def create(config: StoreDTO) =  config match {
    case config:LevelDBStoreDTO =>
      if( config.getClass == classOf[LevelDBStoreDTO] ) {
        new LevelDBStore(config)
      } else {
        null
      }
    case _ => null
  }

}
