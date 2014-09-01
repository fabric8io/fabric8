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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import io.fabric8.fab.osgi.FabResolverFactory;
import io.fabric8.fab.osgi.FabURLHandler;
import io.fabric8.fab.osgi.ServiceConstants;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Activator for the fab protocol
 */
public class Activator implements BundleActivator {
    private static Activator instance;

    private BundleContext bundleContext;
    private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configAdminTracker;
    private final List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();

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
        instance = this;
    }

    @Override
    public void start(BundleContext bundleContext) {
        this.bundleContext = bundleContext;

        configAdminTracker = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(
                bundleContext,
                ConfigurationAdmin.class,
                null) {
            @Override
            public ConfigurationAdmin addingService(ServiceReference<ConfigurationAdmin> reference) {
                ConfigurationAdmin ca = super.addingService(reference);
                bindConfigAdmin(ca);
                return ca;
            }

            @Override
            public void removedService(ServiceReference<ConfigurationAdmin> reference, ConfigurationAdmin service) {
                unbindConfigAdmin();
                super.removedService(reference, service);
            }
        };
        configAdminTracker.open();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        configAdminTracker.close();
    }

    protected void bindConfigAdmin(ConfigurationAdmin configAdmin) {
        File data = new File(System.getProperty("karaf.data", "."));
        OsgiModuleRegistry registry = new OsgiModuleRegistry();
        registry.setDirectory(new File(data, "fab-module-registry"));
        registry.setConfigurationAdmin(configAdmin);
        registry.setPid("io.fabric8.fab.osgi.registry");
        registry.load();

        // Create and register the FabResolverFactory
        final FabResolverFactoryImpl factory = new FabResolverFactoryImpl();
        factory.setRegistry(registry);
        factory.setBundleContext(bundleContext);
        factory.setConfigurationAdmin(configAdmin);
        registerFabResolverFactory(factory);

        // track FeaturesService for use by factory
        ServiceTracker<FeaturesService, FeaturesService> featuresServiceTracker = new ServiceTracker<FeaturesService, FeaturesService>(
                bundleContext,
                FeaturesService.class,
                null) {
            @Override
            public FeaturesService addingService(ServiceReference<FeaturesService> reference) {
                FeaturesService fs = super.addingService(reference);
                factory.setFeaturesService(fs);
                return fs;
            }

            @Override
            public void removedService(ServiceReference<FeaturesService> reference, FeaturesService service) {
                factory.setFeaturesService(null);
                super.removedService(reference, service);
            }
        };
        featuresServiceTracker.open();

        // Create and register the fab: URL handler
        FabURLHandler handler = new FabURLHandler();
        handler.setFabResolverFactory(factory);
        handler.setServiceProvider(factory);
        registerURLHandler(handler);
    }

    protected void unbindConfigAdmin() {
        for (ServiceRegistration registration : registrations) {
            if (registration != null) {
                registration.unregister();
            }
        }
        registrations.clear();
    }

    /*
     * Register the URL handler
     */
    private void registerURLHandler(FabURLHandler handler) {
        if (bundleContext != null && handler != null) {
            Hashtable props = new Hashtable();
            props.put("url.handler.protocol", ServiceConstants.PROTOCOL_FAB);
            ServiceRegistration registration = bundleContext.registerService(URLStreamHandlerService.class, handler, props);
            registrations.add(registration);
        }
    }

    /*
     * Register the {@link FabResolverFactory}
     */
    private void registerFabResolverFactory(FabResolverFactoryImpl factory) {
        if (bundleContext != null && factory != null) {
            ServiceRegistration registration = bundleContext.registerService(FabResolverFactory.class, factory, null);
            registrations.add(registration);
        }
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

}
