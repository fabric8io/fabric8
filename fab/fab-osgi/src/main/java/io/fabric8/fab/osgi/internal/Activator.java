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
package io.fabric8.fab.osgi.internal;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;

import io.fabric8.fab.osgi.ServiceConstants;
import org.ops4j.pax.url.commons.handler.HandlerActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Activator for the fab protocol
 */
public class Activator extends HandlerActivator<Configuration> {
    private static Activator instance;

    private BundleContext bundleContext;
    private ConfigAdmin configAdmin;
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
        this.configAdmin = new ConfigAdmin();
        this.configAdmin.open();

        File data = new File(System.getProperty("karaf.data", "."));
        registry.setDirectory(new File(data, "fab-module-registry"));
        registry.setConfigurationAdmin(new ConfigAdmin());
        registry.setPid("io.fabric8.fab.osgi.registry");
        registry.load();

        super.start(bundleContext);
    }

    @Override
    public void stop(BundleContext bundleContext) {
        this.configAdmin.close();
        super.stop(bundleContext);
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public class ConfigAdmin extends ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> implements ConfigurationAdmin {

        public ConfigAdmin() {
            super(bundleContext, ConfigurationAdmin.class, null);
        }

        private ConfigurationAdmin getConfigAdmin() throws IOException {
            try {
                ConfigurationAdmin ca = waitForService(5000l);
                if (ca != null) {
                    return ca;
                }
                throw new IllegalStateException("ConfigurationAdmin not present");
            } catch (InterruptedException e) {
                throw (InterruptedIOException) new InterruptedIOException("ConfigurationAdmin not present").initCause(e);
            }
        }

        @Override
        public org.osgi.service.cm.Configuration createFactoryConfiguration(String factoryPid) throws IOException {
            return getConfigAdmin().createFactoryConfiguration(factoryPid);
        }

        @Override
        public org.osgi.service.cm.Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
            return getConfigAdmin().createFactoryConfiguration(factoryPid, location);
        }

        @Override
        public org.osgi.service.cm.Configuration getConfiguration(String pid, String location) throws IOException {
            return getConfigAdmin().getConfiguration(pid, location);
        }

        @Override
        public org.osgi.service.cm.Configuration getConfiguration(String pid) throws IOException {
            return getConfigAdmin().getConfiguration(pid);
        }

        @Override
        public org.osgi.service.cm.Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
            return getConfigAdmin().listConfigurations(filter);
        }
    }
}
