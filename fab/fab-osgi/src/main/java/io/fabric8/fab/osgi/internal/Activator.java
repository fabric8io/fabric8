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

package io.fabric8.fab.osgi.internal;

import java.io.File;

import io.fabric8.fab.osgi.ServiceConstants;
import org.ops4j.pax.url.commons.handler.HandlerActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Activator for the fab protocol
 */
public class Activator extends HandlerActivator<Configuration> {
    private static Activator instance;

    private BundleContext bundleContext;
    public static OsgiModuleRegistry registry = new OsgiModuleRegistry();

    public static Activator getInstance() {
        return instance;
    }

    public static BundleContext getInstanceBundleContext() {
        Activator activator = getInstance();
        if (activator != null) {
            return activator.getBundleContext();
        }
        return null;
    }

    public Activator() {
        super(ServiceConstants.PROTOCOLS_SUPPORTED, ServiceConstants.PID, new FabConnectionFactory());
        instance = this;
    }

    @Override
    public void start(BundleContext bundleContext) {
        this.bundleContext = bundleContext;

        ServiceReference serviceReference = bundleContext.getServiceReference("org.osgi.service.cm.ConfigurationAdmin");
        ConfigurationAdmin configurationAdmin = (ConfigurationAdmin) bundleContext.getService(serviceReference);

        File data = new File(System.getProperty("karaf.data", "."));
        registry.setDirectory(new File(data, "fab-module-registry"));
        registry.setConfigurationAdmin(configurationAdmin);
        registry.setPid("io.fabric8.fab.osgi.registry");
        registry.load();

        super.start(bundleContext);
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }
}
