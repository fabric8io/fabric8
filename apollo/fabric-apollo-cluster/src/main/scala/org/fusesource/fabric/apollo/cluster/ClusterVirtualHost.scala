/*
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

package org.fusesource.fabric.apollo.cluster

import dto.ClusterVirtualHostDTO
import org.apache.activemq.apollo.dto.VirtualHostDTO
import org.apache.activemq.apollo.broker.{VirtualHost, Broker, VirtualHostFactory}
import org.apache.activemq.apollo.util.{Log, BaseService}

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ClusterVirtualHostFactory extends VirtualHostFactory {

  def create(broker: Broker, dto: VirtualHostDTO): VirtualHost = dto match {
    case dto:ClusterVirtualHostDTO =>
      val rc = new ClusterVirtualHost(broker, dto.id)
      rc.config = dto
      rc
    case _ => null
  }

}
object ClusterVirtualHost extends Log

class ClusterVirtualHost(broker: Broker, id: String) extends VirtualHost(broker, id) {
  import ClusterVirtualHost._
  override val router:ClusterRouter = new ClusterRouter(this)


  //
  // We have to do some life cycle gymnastics here so that the virtual host
  // is only started when it's requested to be started by the admin and the
  // cluster state allows us to be the the master.
  //

  val master = new BaseService() {
    def dispatch_queue = ClusterVirtualHost.this.dispatch_queue
    protected def _start(on_completed: Runnable) = super_start(on_completed)
    protected def _stop(on_completed: Runnable) = super_stop(on_completed)
  }

  protected def super_start(on_completed: Runnable) = {
    println("virtual host started")
    super._start(on_completed)
  }

  protected def super_stop(on_completed: Runnable) = {
    println("virtual host stopped")
    super._stop(on_completed)
  }

  var start_requested = false
  var active = false

  def make_master = {
    println("Virtual host is a master")
    dispatch_queue.assertExecuting()
    active = true
    this.client_redirect = None
    if( start_requested ) {
      master.start
    }
  }

  def make_slave(master_url:Option[String]) = {
    println("Virtual host is a slave of: "+master_url)
    dispatch_queue.assertExecuting()
    this.client_redirect = master_url
    active = false
    master.stop
  }

  override protected def _start(on_completed: Runnable) {
    start_requested = true
    if( active ) {
      master.start(on_completed)
    } else {
      on_completed.run()
    }
  }

  override protected def _stop(on_completed: Runnable) {
    start_requested = false
    master.stop(on_completed)
  }



}