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
package io.fabric8.jolokia;

import java.util.Hashtable;
import org.jolokia.osgi.JolokiaActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpContext;

public class Activator extends JolokiaActivator {

    private HttpContext secureJolokiaContext = new JolokiaSecureHttpContext("karaf", "admin");
    private ServiceRegistration managedContextRegistration;

    @Override
    public void start(BundleContext pBundleContext) {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.pid", "io.fabric8.jolokia");
        managedContextRegistration = pBundleContext.registerService(ManagedService.class.getName(), secureJolokiaContext, properties);
        super.start(pBundleContext);
    }

    @Override
    public void stop(BundleContext pBundleContext) {
        super.stop(pBundleContext);
        managedContextRegistration.unregister();
    }


    /**
     * Get the security context for out servlet. Dependent on the configuration,
     * this is either a no-op context or one which authenticates with a given user
     *
     * @return the HttpContext with which the agent servlet gets registered.
     */
    @Override
    public synchronized HttpContext getHttpContext() {
        return secureJolokiaContext;
    }

}
