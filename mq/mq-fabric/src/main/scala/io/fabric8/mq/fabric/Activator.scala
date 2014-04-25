/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.mq.fabric

import org.osgi.framework.{ServiceRegistration, BundleActivator, BundleContext}
import java.util.Hashtable
import org.osgi.service.cm.ManagedServiceFactory

class Activator extends BundleActivator {

  var registration: ServiceRegistration[ManagedServiceFactory] = _
  var factory: ActiveMQServiceFactory = _

  def start(context: BundleContext) {
    factory = new ActiveMQServiceFactory(context)
    val props = new Hashtable[String, Object]()
    props.put("service.pid", "io.fabric8.mq.fabric.server")
    registration = context.registerService(classOf[ManagedServiceFactory], factory, props)
  }

  def stop(context: BundleContext) {
    this.factory.destroy()
    this.registration.unregister()
  }

}
