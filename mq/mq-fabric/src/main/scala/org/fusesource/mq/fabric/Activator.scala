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
package org.fusesource.mq.fabric

import org.osgi.framework.{ServiceRegistration, ServiceReference, BundleActivator, BundleContext}
import org.osgi.util.tracker.{ServiceTracker, ServiceTrackerCustomizer}
import java.util.Hashtable
import org.osgi.service.cm.ManagedServiceFactory
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.state.{ConnectionState, ConnectionStateListener}
import org.fusesource.fabric.api.FabricService

class Activator extends BundleActivator with ServiceTrackerCustomizer[CuratorFramework, CuratorFramework] with ConnectionStateListener {

  var context: BundleContext = _
  var tracker: ServiceTracker[CuratorFramework, CuratorFramework] = _
  var fabricTracker: ServiceTracker[FabricService, FabricService] = _
  var curator: CuratorFramework = _
  var factory: ActiveMQServiceFactory = _
  var registration: ServiceRegistration[ManagedServiceFactory] = _

  def start(context: BundleContext) {
    this.context = context
    this.fabricTracker = new ServiceTracker(this.context, classOf[FabricService], new ServiceTrackerCustomizer[FabricService, FabricService] {
      def addingService(reference: ServiceReference[FabricService]): FabricService = {
        val fabric = context.getService(reference)
        if (factory != null) {
          factory.bindFabricService(fabric)
        }
        fabric
      }
      def modifiedService(reference: ServiceReference[FabricService], service: FabricService): Unit = {
      }
      def removedService(reference: ServiceReference[FabricService], service: FabricService): Unit = {
        if (factory != null) {
          factory.unbindFabricService(service)
        }
        context.ungetService(reference)
      }
    })
    this.tracker = new ServiceTracker(this.context, classOf[CuratorFramework], this)
    this.fabricTracker.open()
    this.tracker.open()
  }

  def stop(context: BundleContext) {
    this.tracker.close()
    this.context = null
  }

  def addingService(reference: ServiceReference[CuratorFramework]): CuratorFramework = {
    curator = this.context.getService(reference)
    if (registration != null) {
      registration.unregister()
      registration != null
    }
    if (factory != null) {
      factory.destroy()
      factory = null
    }
    if (curator != null) {
      curator.getConnectionStateListenable.addListener(this)
      if (curator.getZookeeperClient.isConnected) {
        stateChanged(curator, ConnectionState.CONNECTED)
      }
    }
    curator
  }

  def modifiedService(reference: ServiceReference[CuratorFramework], service: CuratorFramework): Unit = {
  }

  def removedService(reference: ServiceReference[CuratorFramework], service: CuratorFramework): Unit = {
    if (registration != null) {
      registration.unregister()
      registration = null
    }
    if (factory != null) {
      factory.destroy()
      factory = null
    }
    curator = null
    service.getConnectionStateListenable.removeListener(this)
    this.context.ungetService(reference)
  }


  def stateChanged(client: CuratorFramework, newState: ConnectionState): Unit = {
    if (newState == ConnectionState.CONNECTED || newState == ConnectionState.RECONNECTED) {
      if (factory == null) {
        val props = new Hashtable[String, Object]()
        props.put("service.pid", "org.fusesource.mq.fabric.server")
        factory = new ActiveMQServiceFactory(this.context, curator)
        factory.bindFabricService(fabricTracker.getService)
        registration = this.context.registerService(classOf[ManagedServiceFactory], factory, props)
      }
    } else if (newState == ConnectionState.SUSPENDED || newState == ConnectionState.LOST) {
      if (registration != null) {
        registration.unregister()
        registration = null
      }
      if (factory != null) {
        factory.destroy()
        factory = null
      }
    }
  }

}
