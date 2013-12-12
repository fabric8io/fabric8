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

package io.fabric8.service.jclouds.firewall;

import java.io.IOException;
import java.util.Set;
import org.jclouds.compute.ComputeService;

public class FirewallManager {

    private final ComputeService computeService;
    private final ApiFirewallSupport firewallSupport;

    /**
     * Constructor
     */
    public FirewallManager(ComputeService computeService, ApiFirewallSupport firewallSupport) {
        this.computeService = computeService;
        this.firewallSupport = firewallSupport;
    }

    public void addRules(Rule... rules) throws IOException {
        for (Rule rule : rules) {
            addRule(rule);
        }
    }

    public void addRules(Set<Rule> rules) throws IOException {
        for (Rule rule : rules) {
            addRule(rule);
        }
    }


    public void addRule(Rule rule) throws IOException {
        switch (rule.getType()) {
            case FLUSH:
                firewallSupport.flush(computeService, rule.getDestination());
            break;
            case AUTHORIZE:
                firewallSupport.authorize(computeService, rule.getDestination(), rule.getSource(), rule.getPorts());
            break;
            case REVOKE:
                firewallSupport.revoke(computeService, rule.getDestination(), rule.getSource(), rule.getPorts());
            break;
            default:
        }
    }

    public boolean isSupported() {
        return firewallSupport != null;
    }
}
