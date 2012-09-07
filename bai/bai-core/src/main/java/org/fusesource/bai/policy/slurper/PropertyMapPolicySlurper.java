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

package org.fusesource.bai.policy.slurper;

import org.fusesource.bai.config.EventType;
import org.fusesource.bai.policy.model.Constants.ActionType;
import org.fusesource.bai.policy.model.Constants.ScopeElement;
import org.fusesource.bai.policy.model.Policy;
import org.fusesource.bai.policy.model.PolicySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This implementation of {@link PolicySlurper} reads a OSGi Config Admin-compatible text property file and constructs the Policy model.
 *
 * @author Raul Kripalani
 */
@SuppressWarnings("rawtypes")
public class PropertyMapPolicySlurper implements PolicySlurper {

    private static final transient Logger LOG = LoggerFactory.getLogger(PropertyMapPolicySlurper.class);

    private Dictionary properties;
    private PolicySet policyCache;

    public PropertyMapPolicySlurper() {
    }

    public PropertyMapPolicySlurper(Dictionary properties) {
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized PolicySet slurp() {
        if (policyCache != null) {
            return policyCache;
        }

        PolicySet answer = new PolicySet();
        List<Object> keys = Collections.list(properties.keys());

        for (Object k : keys) {
            String value = (String) properties.get((String) k);
            String[] splitKey = splitPropertyKey((String) k);

            String qualifier = splitKey[0];
            Set<Policy> result = null;
            if ("camelContext".equals(qualifier)) {
                result = parseCamelContext(splitKey, value);
            }

            if ("event".equals(qualifier)) {
                result = parseEvent(splitKey, value);
            }

            if ("exchange".equals(qualifier)) {
                result = parseExchange(splitKey, value);
            }

            if ("endpoint".equals(qualifier)) {
                result = parseEndpoint(splitKey, value);
            }

            if (result != null) {
                answer.addAll(result);
            }
        }

        policyCache = answer;
        return answer;
    }

    private String[] splitPropertyKey(String propertyKey) {
        // A. split at the slash first
        String[] splitSlash = propertyKey.split("\\/");
        // B. split what was before the slash with dots
        String[] splitDot = splitSlash[0].split("\\.");
        String[] splitKey = splitDot;
        // append what followed the slash to the dot-split portion before the slash (only if there was
        if (splitSlash.length > 1) {
            splitKey = Arrays.copyOf(splitKey, splitDot.length + (splitSlash.length - 1));
            System.arraycopy(splitSlash, 1, splitKey, splitDot.length, splitSlash.length - 1);
        }
        return splitKey;
    }

    @Override
    public PolicySet refresh() {
        policyCache = slurp();
        return policyCache;
    }

    /**
     * Parse this format: camelContext.exclude = camelContextPatterns. Where camelContextPatterns is a space-separated
     * list of camelContextPattern instances. A camelContextPatterns is of the form bundleSymbolicNamePattern[:camelContextIdPattern].
     *
     * @param splitKey
     * @param value
     * @return
     */
    private Set<Policy> parseCamelContext(String[] splitKey, String value) {
        Set<Policy> globalPolicies = new HashSet<Policy>();
        ActionType actionType = "exclude".equals(splitKey[1].toLowerCase()) ? ActionType.EXCLUDE : ActionType.INCLUDE;

        for (String token : value.split(" ")) {
            Policy policy = new Policy();
            String[] splitToken = token.split(":");
            policy.addNewExpressionFilterFor(ScopeElement.BUNDLE).language("wildcardAwareString").expression(splitToken[0]);
            if (splitToken.length == 2) {
                policy.addNewExpressionFilterFor(ScopeElement.CONTEXT).language("wildcardAwareString").expression(splitToken[1]);
            }

            policy.getAction().setType(actionType);
            globalPolicies.add(policy);
        }

        return globalPolicies;
    }

    /**
     * Parse this format: event.$eventName.$camelContextPattern = (true|false)
     *
     * @param splitKey
     * @param value
     * @return
     */
    private Set<Policy> parseEvent(String[] splitKey, String value) {
        Set<Policy> policies = new HashSet<Policy>();
        Policy policy = new Policy();

        // if the value = 'true', include; if 'false', exclude
        if (Boolean.parseBoolean(value)) {
            policy.getAction().setType(ActionType.INCLUDE);
        } else {
            policy.getAction().setType(ActionType.EXCLUDE);
        }

        // Scope this policy to the specific event
        EventType et = toEventType(splitKey[1]);
        if (et == null) {
            return null;
        }
        policy.addNewEnumerationFilterFor(ScopeElement.EVENT).enumValues(et.toString());

        // Scope this policy to the Bundle
        policy.addNewExpressionFilterFor(ScopeElement.BUNDLE).language("wildcardAwareString").expression(splitKey[2]);

        // Scope this policy to the Camel Context, if defined
        if (splitKey.length == 4) {
            policy.addNewExpressionFilterFor(ScopeElement.CONTEXT).language("wildcardAwareString").expression(splitKey[3]);
        }

        policies.add(policy);
        return policies;

    }

    /**
     * Parse this format: exchange.filter.$eventType.$language[/$bundleIDRegex[/$camelContextIDRegex]] = expression
     *
     * @param splitKey
     * @param value
     * @return
     */
    private Set<Policy> parseExchange(String[] splitKey, String value) {
        Set<Policy> policies = new HashSet<Policy>();
        Policy policy = new Policy();

        // Scope this policy to the specific event
        EventType et = toEventType(splitKey[2]);
        if (et == null) {
            return null;
        }
        policy.addNewEnumerationFilterFor(ScopeElement.EVENT).enumValues(et.toString());

        // Scope this policy to Exchanges matching the specified expression
        policy.addNewExpressionFilterFor(ScopeElement.EXCHANGE).language(splitKey[3]).expression(value);

        // Let's process the Bundle Symbolic Name
        if (splitKey.length >= 5) {
            policy.addNewExpressionFilterFor(ScopeElement.BUNDLE).language("wildcardAwareString").expression(splitKey[4]);
        }

        // Let's process the Context ID
        if (splitKey.length == 6) {
            policy.addNewExpressionFilterFor(ScopeElement.CONTEXT).language("wildcardAwareString").expression(splitKey[5]);
        }

        // TODO: according to the model, there's no capability to exclude; so you need to tailor your expression if you want to do that
        policy.getAction().setType(ActionType.INCLUDE);
        //policy.pruneRedundantScopes();
        policies.add(policy);
        return policies;
    }

    /**
     * Parse this format: endpoint.(include|exclude)[/$bundleIDRegex[/$camelContextIDRegex]] = $endpointUriRegex
     * TODO: Need to add support for XML namespace definition in XPath expressions, maybe following the Java QName format.
     *
     * @param splitKey
     * @param value
     * @return
     */
    private Set<Policy> parseEndpoint(String[] splitKey, String value) {
        Set<Policy> policies = new HashSet<Policy>();
        Policy policy = new Policy();

        // Scope this policy to the specific event
        policy.addNewExpressionFilterFor(ScopeElement.ENDPOINT).language("wildcardAwareString").expression(value);

        if ("exclude".equals(splitKey[1].toLowerCase())) {
            policy.getAction().setType(ActionType.EXCLUDE);
        } else {
            policy.getAction().setType(ActionType.INCLUDE);
        }

        // Let's process the Bundle Symbolic Name
        if (splitKey.length >= 3) {
            policy.addNewExpressionFilterFor(ScopeElement.BUNDLE).language("wildcardAwareString").expression(splitKey[2]);
        }

        // Let's process the Context ID
        if (splitKey.length == 4) {
            policy.addNewExpressionFilterFor(ScopeElement.CONTEXT).language("wildcardAwareString").expression(splitKey[3]);
        }

        //policy.pruneRedundantScopes();
        policies.add(policy);
        return policies;
    }

    private EventType toEventType(String eventType) {
        if ("*".equals(eventType)) {
            return EventType.ALL;
        }
        // Scope this policy to the specific event
        EventType et;
        try {
            et = EventType.valueOf(eventType.toUpperCase());
        } catch (Exception e) {
            LOG.warn("Event-type policy could not be parsed. Contains invalid event type: %s", eventType);
            return null;
        }
        return et;
    }

    public Dictionary getProperties() {
        return properties;
    }

    public void setProperties(Dictionary properties) {
        this.properties = properties;
    }

    public PolicySet getPolicies() {
        return policyCache;
    }

}
