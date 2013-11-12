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

import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.jcip.GuardedBy;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.visibility.VisibleForExternal;
import org.fusesource.fabric.service.jclouds.firewall.ApiFirewallSupport;
import org.fusesource.fabric.service.jclouds.firewall.FirewallManager;
import org.fusesource.fabric.service.jclouds.firewall.FirewallManagerFactory;
import org.fusesource.fabric.service.jclouds.firewall.FirewallNotSupportedOnProviderException;
import org.jclouds.compute.ComputeService;
@ThreadSafe
@Component(name = "org.fusesource.fabric.jclouds.firewall.manager.factory", description = "Fabric Firewall Manager", immediate = true)
@Service(FirewallManagerFactory.class)
public final class FirewallManagerFactoryImpl extends AbstractComponent implements FirewallManagerFactory {

    @Reference(referenceInterface = ApiFirewallSupport.class, bind = "bindFirewallSupport", unbind = "unbindFirewallSupport", cardinality = OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    @GuardedBy("CopyOnWriteArraySet") private final Set<ApiFirewallSupport> firewallSupport = new CopyOnWriteArraySet<ApiFirewallSupport>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    /**
     * Returns a {@link FirewallManager} for the specified {@link ComputeService}.
     */
    @Override
    public FirewallManager getFirewallManager(ComputeService computeService) throws FirewallNotSupportedOnProviderException {
        assertValid();
        ApiFirewallSupport firewallSupport = findApiFirewallSupport(computeService);
        if (firewallSupport == null) {
            throw new FirewallNotSupportedOnProviderException("Service is currently not supported for firewall operations");
        }
        FirewallManager firewallManager = new FirewallManager(computeService, firewallSupport);
        return firewallManager;
    }

    private ApiFirewallSupport findApiFirewallSupport(ComputeService computeService) {
        for (ApiFirewallSupport s : firewallSupport) {
            if (s.supports(computeService)) {
                return s;
            }
        }
        return null;
    }

    @VisibleForExternal
    public void bindFirewallSupport(ApiFirewallSupport providerSupport) {
        firewallSupport.add(providerSupport);
    }

    @VisibleForExternal
    public void unbindFirewallSupport(ApiFirewallSupport providerSupport) {
        firewallSupport.remove(providerSupport);
    }
}
