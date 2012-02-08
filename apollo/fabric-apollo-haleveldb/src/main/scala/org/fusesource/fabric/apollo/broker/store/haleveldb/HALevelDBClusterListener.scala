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

import org.fusesource.fabric.apollo.cluster.{ClusterListener, ClusterConnector, ClusterListenerFactory}
import org.apache.activemq.apollo.util.BaseService
import org.fusesource.hawtdispatch.DispatchQueue

/**
 * <p>
 * Hooks into the Cluster Connector events so that we can keep
 * a warm standby of a HALevelDB store on the slaves.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
object HALevelDBClusterListenerFactory extends ClusterListenerFactory {
  def create(connector: ClusterConnector): Option[ClusterListener] = {
    Some(new HALevelDBClusterListener(connector))
  }
}

class  HALevelDBClusterListener(connector:ClusterConnector) extends ClusterListener{

  /**
   * This gets called anytime the cluster membership changes.
   */
  def on_change {
    // TODO:
    // Check to see if we are now a slave of another node, and connect
    // to it so that we can keep a warm copy of the message store.
  }

  /**
   * This gets called when the cluster connector shuts down.
   */
  def close() = {

  }
}