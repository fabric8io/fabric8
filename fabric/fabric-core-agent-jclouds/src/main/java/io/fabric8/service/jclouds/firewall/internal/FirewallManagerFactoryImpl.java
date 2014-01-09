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

package io.fabric8.service.jclouds.firewall.internal;

import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.visibility.VisibleForExternal;
import io.fabric8.service.jclouds.firewall.ApiFirewallSupport;
import io.fabric8.service.jclouds.firewall.FirewallManager;
import io.fabric8.service.jclouds.firewall.FirewallManagerFactory;
import io.fabric8.service.jclouds.firewall.FirewallNotSupportedOnProviderException;
import org.jclouds.compute.ComputeService;
@ThreadSafe
@Component(name = "io.fabric8.jclouds.firewall.manager.factory", label = "Fabric8 Firewall Manager", immediate = true, metatype = false)
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
