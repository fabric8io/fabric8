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

import dto.ClusterVirtualHostDTO
import org.apache.activemq.apollo.dto.VirtualHostDTO
import org.apache.activemq.apollo.broker.{VirtualHost, Broker, VirtualHostFactory}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ClusterVirtualHostFactory extends VirtualHostFactory.Provider {

  def create(broker: Broker, dto: VirtualHostDTO): VirtualHost = dto match {
    case dto:ClusterVirtualHostDTO =>
      val rc = new ClusterVirtualHost(broker, dto.id)
      rc.config = dto
      rc
    case _ => null
  }

}

class ClusterVirtualHost(broker: Broker, id: String) extends VirtualHost(broker, id) {
  override val router:ClusterRouter = new ClusterRouter(this)

}