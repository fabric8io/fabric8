/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.fabric.service.jclouds.firewall.internal;

import org.fusesource.fabric.service.jclouds.firewall.ApiFirewallSupport;
import org.fusesource.fabric.service.jclouds.firewall.FirewallManager;
import org.fusesource.fabric.service.jclouds.firewall.FirewallManagerFactory;
import org.fusesource.fabric.service.jclouds.firewall.FirewallNotSupportedOnProviderException;
import org.jclouds.compute.ComputeService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.HashSet;
import java.util.Set;

public class FirewallManagerFactoryImpl implements FirewallManagerFactory {

    private final Set<ApiFirewallSupport> firewallSupportModules = new HashSet<ApiFirewallSupport>();

    private BundleContext bundleContext;
    private ServiceTracker firewallSupportModuleTracker;

    public void init() {
        firewallSupportModuleTracker = new ServiceTracker(bundleContext,ApiFirewallSupport.class.getName(), null) {

            @Override
            public Object addingService(ServiceReference reference) {
                ApiFirewallSupport support = (ApiFirewallSupport) bundleContext.getService(reference);
                bind(support);
                return support;
            }


            @Override
            public void removedService(ServiceReference reference, Object service) {
                ApiFirewallSupport support = (ApiFirewallSupport) service;
                unbind(support);
                super.removedService(reference, service);
            }

            @Override
            public void modifiedService(ServiceReference reference, Object service) {
                ApiFirewallSupport support = (ApiFirewallSupport) service;
                bind(support);
                super.modifiedService(reference, service);
            }
        };
        firewallSupportModuleTracker.open();
    }

    public void destroy() {
        if (firewallSupportModuleTracker != null) {
            firewallSupportModuleTracker.close();
        }
    }

    /**
     * Returns a {@link org.fusesource.fabric.service.jclouds.firewall.FirewallManager} for the specified {@link org.jclouds.compute.ComputeService}.
     *
     * @param computeService
     * @return
     */
    @Override
    public synchronized FirewallManager getFirewallManager(ComputeService computeService) throws FirewallNotSupportedOnProviderException {
            ApiFirewallSupport firewallSupport = findApiFirewallSupport(computeService);
            if (firewallSupport == null) {
                throw new FirewallNotSupportedOnProviderException("Service is currently not supported for firewall operations");
            }
            FirewallManager firewallManager = new FirewallManager(computeService, firewallSupport);
            return firewallManager;
    }

    private ApiFirewallSupport findApiFirewallSupport(ComputeService computeService) {
        for (ApiFirewallSupport s : firewallSupportModules) {
           if (s.supports(computeService)) {
               return s;
           }
        }
        return null;
    }

    public void bind(ApiFirewallSupport providerSupport) {
        firewallSupportModules.add(providerSupport);
    }

    public void unbind(ApiFirewallSupport providerSupport) {
        firewallSupportModules.remove(providerSupport);
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
