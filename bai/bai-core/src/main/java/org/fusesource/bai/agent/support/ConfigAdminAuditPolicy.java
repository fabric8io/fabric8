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
import java.util.EnumMap;

import org.apache.camel.Predicate;
import org.apache.camel.spi.Language;
import org.apache.camel.util.ObjectHelper;
import org.fusesource.bai.AuditEventNotifier;
import org.fusesource.bai.config.EventType;
import org.fusesource.bai.EventTypeConfiguration;
import org.fusesource.bai.agent.CamelContextService;
import org.fusesource.bai.agent.filters.CamelContextFilters;
import org.fusesource.bai.policy.model.Constants.ActionType;
import org.fusesource.bai.policy.model.Constants.ScopeElement;
import org.fusesource.bai.policy.model.EnumerationFilter;
import org.fusesource.bai.policy.model.ExpressionFilter;
import org.fusesource.bai.policy.model.Policy;
import org.fusesource.bai.policy.model.PolicySet;
import org.fusesource.bai.policy.slurper.PropertyMapPolicySlurper;
import org.fusesource.common.util.Pair;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit Policy Injecter which uses the Policy Model constructed by {@link PropertyMapPolicySlurper}.
 * @author Raul Kripalani
 */
@SuppressWarnings("rawtypes")
public class ConfigAdminAuditPolicy extends ConfigAdminAuditPolicySupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(ConfigAdminAuditPolicy.class);
	
    private PolicySet policies;

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

    /**
     * Apply the current policy to a notifier which is being registered.
     * Scopes: BUNDLE, CONTEXT ID, ENDPOINT, EVENT, EXCHANGE.
     * Algorithm as follows:
     *  1. Restrict PolicySet to policies that apply in the current scenario:
     *  	- that match the Bundle Filter and/or Context ID filter
     *  	- or don't carry Bundle/Context filters.
     *  2. Configure policies that apply to ALL events: exclude / expressions.
     *  3. Check if there are event-specific policies and apply them.
     *  4. Apply endpoint policies.
     *  @author Raul Kripalani
     */
    @Override
    public void configureNotifier(CamelContextService camelContextService, AuditEventNotifier notifier) {
        LOG.info("Updating AuditEventNotifier " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + " camelContext: " + camelContextService);
        
        // 0. Start with the entire universe of Policies
        PolicySet applicablePolicies = (PolicySet) policies.clone();
        LOG.debug("Calculating applicable policies for notifier " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + " camelContext: " + camelContextService);

        // 1. Discard policies with BUNDLE and CONTEXT filters that don't match our CamelContext
        PolicySet p = policies.policiesContainingAnyScopeElements(ScopeElement.BUNDLE, ScopeElement.CONTEXT);
        for (Policy policy : p) {
			if (!CamelContextFilters.createCamelContextFilter(policy).matches(camelContextService)) {
				applicablePolicies.remove(policy);
			}
		}
        LOG.debug("Policies retained for " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + " after removal of non-matching ones " +
        		"due to context/bundle filtering: " + applicablePolicies);

        // 2. Discard general policies, i.e. policies that only apply to bundle and context
        applicablePolicies.removeAll(applicablePolicies.policiesWithExactScopeElements(ScopeElement.BUNDLE, ScopeElement.CONTEXT));
        applicablePolicies.removeAll(applicablePolicies.policiesWithExactScopeElements(ScopeElement.BUNDLE));
        applicablePolicies.removeAll(applicablePolicies.policiesWithExactScopeElements(ScopeElement.CONTEXT));
        LOG.debug("Policies retained for " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + " after removal of global excludes: " + applicablePolicies);
        
        // NOTE: At this point we have: (a) policies that DON'T filter by BUNDLE and CONTEXT and 
        // (b) policies that DO filter by BUNDLE and CONTEXT and they match us
        
        // 3. Configure policies that apply to ALL events
        // if multiple policies match, just pick one; as discussed with jstrachan, we'll be undeterministic
        PolicySet allEvents = applicablePolicies.filtersForScopeElement(ScopeElement.EVENT)
					        	.filtersOfType(EnumerationFilter.class)
					        	.allMatchesFor(EventType.ALL)
					        	.returnPolicies();
        LOG.debug("Selected ALL EVENT policies for " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + ": " + allEvents);
        if (!allEvents.isEmpty()) {
        	applyPoliciesMatchingAllEvents(camelContextService, notifier, allEvents);
        }
        
        // 4. Apply policies specific to event types
        // get all policies with a ScopeElement = EVENT, except for the one containing ALL events
        PolicySet eventPolicies = applicablePolicies.policiesContainingAnyScopeElements(ScopeElement.EVENT);
        eventPolicies.removeAll(allEvents);
        LOG.debug("Selected INDIVIDUAL EVENT policies for " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + ": " + eventPolicies);
        if (!eventPolicies.isEmpty()) {
        	applyPoliciesToIndividualEvents(camelContextService, notifier, eventPolicies);
        }
        
        // 5. Apply endpoint policies
        PolicySet endpointPolicies = applicablePolicies.policiesContainingAnyScopeElements(ScopeElement.ENDPOINT);
        LOG.debug("Selected ENDPOINT policies for " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + ": " + endpointPolicies);
        for (Policy policy : endpointPolicies) {
            LOG.debug("Applying ENDPOINT policy to " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + ": " + policy);
            Pair<EventType, EventTypeConfiguration> etc = getEventTypeConfigForPolicy(notifier, policy);
			EventTypeConfiguration config = etc.getSecond();
			ExpressionFilter ef = policy.getTypedFilterFor(ScopeElement.ENDPOINT, ExpressionFilter.class);
        	if (ef != null && ef.getExpression() != null) {
        		config.addEndpointIncludeRegexp(ef.getExpression());
        	}
            LOG.debug("Resulting EventTypeConfiguration(" + etc.getFirst() + ") for " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + ": " + config);
		}
        
    }

	private Pair<EventType, EventTypeConfiguration> getEventTypeConfigForPolicy(AuditEventNotifier notifier, Policy policy) {
		EnumerationFilter ef = policy.getTypedFilterFor(ScopeElement.EVENT, EnumerationFilter.class);
		EventType et = EventType.valueOf(ef.getOneEnumValue());
		EventTypeConfiguration config = notifier.getConfig(et);
		return new Pair<EventType, EventTypeConfiguration>(et, config);
	}

	private void applyPoliciesToIndividualEvents(CamelContextService camelContextService, AuditEventNotifier notifier, PolicySet ps) {
		for (Policy policy : ps) {
            LOG.debug("Applying INDIVIDUAL EVENT policy to " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + ": " + policy);
            Pair<EventType, EventTypeConfiguration> etc = getEventTypeConfigForPolicy(notifier, policy);
			EventTypeConfiguration config = etc.getSecond();
    		config.setInclude(policy.getAction().getType() == ActionType.INCLUDE);
    		ExpressionFilter exchangeFilter = policy.getTypedFilterFor(ScopeElement.EXCHANGE, ExpressionFilter.class);
    		if (exchangeFilter != null) {
    			Language language = camelContextService.getCamelContext().resolveLanguage(exchangeFilter.getLanguage());
    			ObjectHelper.notNull(language, "Predicate language", policy);
    			Predicate predicate = language.createPredicate(exchangeFilter.getExpression());
    			ObjectHelper.notNull(predicate, "Predicate object", policy);
    			config.getExchangeFilters().add(predicate);
    		}
            LOG.debug("Resulting EventTypeConfiguration(" + etc.getFirst() + ") for " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + ": " + config);
        }
	}

	private void applyPoliciesMatchingAllEvents(CamelContextService camelContextService, AuditEventNotifier notifier, PolicySet allEvents) {
        // for all event types, apply the policies
        // TODO: if there are multiple policies applicable to ALL events, the final behaviour will be undeterministic!
        EnumMap<EventType, Boolean> endpointFilterSet = new EnumMap<EventType, Boolean>(EventType.class);
        EnumMap<EventType, Boolean> exchangeFilterSet = new EnumMap<EventType, Boolean>(EventType.class);
        for (Policy policy : allEvents) {
            LOG.debug("Applying ALL EVENTS policy to " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + ": " + policy);
        	for (EventType et : EventType.values()) {
        		if (et == EventType.ALL) {
        			continue;
        		}
        		EventTypeConfiguration config = notifier.getConfig(et);
        		// cater for event.abc.camelContextExpression = false/true
        		config.setInclude(policy.getAction().getType() == ActionType.INCLUDE);
        		// cater for exchange.filter.*.language... = expression
        		// there can be multiple of these, so we want to add predicates instead of overriding them
        		ExpressionFilter exchangeFilter = policy.getTypedFilterFor(ScopeElement.EXCHANGE, ExpressionFilter.class);
        		if (exchangeFilter != null) {
        			if (exchangeFilterSet.get(et)) {
        				LOG.warn("Duplicate policy definition. Exchange filter had already been set for event: %s", et);
        			}
        			endpointFilterSet.put(et, Boolean.TRUE);
        			Language language = camelContextService.getCamelContext().resolveLanguage(exchangeFilter.getLanguage());
        			ObjectHelper.notNull(language, "Predicate language", policy);
        			Predicate predicate = language.createPredicate(exchangeFilter.getExpression());
        			ObjectHelper.notNull(predicate, "Predicate object", policy);
        			config.getExchangeFilters().add(predicate);
        		}
                LOG.debug("Resulting EventTypeConfiguration(" + et + ") for " + notifier + " for bundle: " + camelContextService.getBundleSymbolicName() + ": " + config);
        	}
        }
	}

	
	
}
