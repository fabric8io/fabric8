/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.fabric.apollo.cluster

import org.apache.activemq.apollo.util.{Service, ClassFinder}

/**
 * This is a connector that handles establishing outbound connections.
 */
trait ClusterListener {
  def on_change:Unit
  def close():Unit
}

trait ClusterListenerFactory {
  def create(connector:ClusterConnector):Option[ClusterListener]
}

object ClusterListenerFactory {
  val finder = new ClassFinder[ClusterListenerFactory]("META-INF/services/org.fusesource.fabric.apollo/cluster-listener-factory.index",classOf[ClusterListenerFactory])

  def create(connector:ClusterConnector):List[ClusterListener] = {
    finder.singletons.flatMap(_.create(connector))
  }
}