/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
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