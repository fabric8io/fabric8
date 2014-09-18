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
package io.fabric8.demo.cxf.client;

import java.util.Dictionary;
import java.util.Hashtable;

import io.fabric8.demo.cxf.Hello;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * This class is used for integration testing this example in Apache Karaf.
 * <p/>
 * This is not needed for Java standalone clients to use OSGi.
 */
public class Activator implements BundleActivator {

    private ServiceRegistration registration;

    public void start(BundleContext bundleContext) throws Exception {
        Dictionary<String, String> props = new Hashtable<String, String>();
        registration = bundleContext.registerService(
                Hello.class.getName(),
                new Client().getProxy(),
                props);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        registration.unregister();
    }
}
