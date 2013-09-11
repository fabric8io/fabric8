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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.service.jclouds.firewall.ApiFirewallSupport;
import org.fusesource.fabric.service.jclouds.firewall.FirewallManager;
import org.fusesource.fabric.service.jclouds.firewall.FirewallManagerFactory;
import org.fusesource.fabric.service.jclouds.firewall.FirewallNotSupportedOnProviderException;
import org.jclouds.compute.ComputeService;

import java.util.HashSet;
import java.util.Set;

import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;

@Component(name = "org.fusesource.fabric.jclouds.firewall.manager.factory",
        description = "Fabric Firewall Manager",
        immediate = true)
@Service(FirewallManagerFactory.class)
public class FirewallManagerFactoryImpl implements FirewallManagerFactory {

    @Reference(cardinality = OPTIONAL_MULTIPLE, bind = "bindApiFirewallSupport", unbind = "unbindApiFirewallSupport", referenceInterface = ApiFirewallSupport.class, policy = ReferencePolicy.DYNAMIC)
    private final Set<ApiFirewallSupport> firewallSupportModules = new HashSet<ApiFirewallSupport>();

    @Activate
    public void init() {

    }

    @Deactivate
    public void destroy() {

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

    public void bindApiFirewallSupport(ApiFirewallSupport providerSupport) {
        firewallSupportModules.add(providerSupport);
    }

    public void unbindApiFirewallSupport(ApiFirewallSupport providerSupport) {
        firewallSupportModules.remove(providerSupport);
    }
}
