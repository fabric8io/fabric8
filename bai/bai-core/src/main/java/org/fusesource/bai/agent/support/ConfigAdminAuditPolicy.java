/*
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
package org.fusesource.bai.agent.support;

import java.util.Dictionary;

import org.fusesource.bai.AuditEventNotifier;
import org.fusesource.bai.agent.CamelContextService;
import org.fusesource.bai.policy.model.Constants.ScopeElement;
import org.fusesource.bai.policy.model.PolicySet;
import org.fusesource.bai.policy.slurper.PropertyMapPolicySlurper;
import org.osgi.service.cm.ConfigurationException;

/**
 */
@SuppressWarnings("rawtypes")
public class ConfigAdminAuditPolicy extends ConfigAdminAuditPolicySupport {

    private PolicySet policies = null;

    @Override
    public void updated(Dictionary dict) throws ConfigurationException {
        System.out.println("Updating BAI Agent configuration " + dict);
        PropertyMapPolicySlurper pmps = new PropertyMapPolicySlurper(dict);
        this.policies = pmps.slurp();
        // obtain all policies whose scope is only a Bundle, a Context or both, a Bundle and a Context
        PolicySet excludedCamelContextsPolicies = policies.policiesWithExactScopeElements(ScopeElement.BUNDLE, ScopeElement.CONTEXT);
        excludedCamelContextsPolicies.addAll(policies.policiesWithExactScopeElements(ScopeElement.BUNDLE));
        excludedCamelContextsPolicies.addAll(policies.policiesWithExactScopeElements(ScopeElement.CONTEXT));
        excludedCamelContextsPolicies = excludedCamelContextsPolicies.queryAllExclusions();
        
        if (excludedCamelContextsPolicies.size() == 0) {
        	setExcludeCamelContextPattern(DEFAULT_EXCLUDE_CAMEL_CONTEXT_FILTER);
        } else {
        	setExcludeCamelContextPolicies(excludedCamelContextsPolicies);
        }
        updateNotifiersWithNewPolicy();
    }

    @Override
    public void configureNotifier(CamelContextService camelContextService, AuditEventNotifier notifier) {
        // TODO
        // apply the current policy to the given notifier given the notifier for the camelContextService
        System.out.println("Updating AuditEventNotifier " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + " camelContext: " + camelContextService);
    }

}
