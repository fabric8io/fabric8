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
package org.fusesource.fabric.redirect;

import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

public class Activator implements BundleActivator {
    private RedirectServlet redirect = new RedirectServlet();
    private HttpServiceTracker serviceTracker;
    private ServiceRegistration managedContextRegistration;

    @Override
    public void start(BundleContext bundleContext) {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.pid", "org.fusesource.fabric.redirect");
        managedContextRegistration = bundleContext.registerService(ManagedService.class.getName(), redirect, properties);

        serviceTracker = new HttpServiceTracker(bundleContext, redirect);
        serviceTracker.open();
    }

    @Override
    public void stop(BundleContext pBundleContext) {
        serviceTracker.close();
        serviceTracker = null;
        managedContextRegistration.unregister();
    }
}
