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
package io.fabric8.mq.fabric;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;

public class Activator implements BundleActivator {

    private ServiceRegistration<ManagedServiceFactory> registration = null;
    private ActiveMQServiceFactory factory = null;

    @Override
    public void start(BundleContext context) throws Exception {
        factory = new ActiveMQServiceFactory(context);
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("service.pid", "io.fabric8.mq.fabric.server");
        registration = context.registerService(ManagedServiceFactory.class, factory, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        this.factory.destroy();
        this.registration.unregister();
    }

}
