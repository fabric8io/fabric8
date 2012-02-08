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
import org.apache.activemq.apollo.broker.store.StoreFactory
import org.apache.activemq.apollo.dto.StoreDTO

/**
 * <p>
 * Hook to use a HALevelDBStore when a HALevelDBStoreDTO is
 * used in a broker configuration.
 * </p>
 * <p>
 * This class is discovered using the following resource file:
 * <code>META-INF/services/org.apache.activemq.apollo/stores</code>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object HALevelDBStoreFactory extends StoreFactory {

  def create(config: StoreDTO) = config match {
    case config:HALevelDBStoreDTO => new HALevelDBStore(config)
    case _ => null
  }

}
