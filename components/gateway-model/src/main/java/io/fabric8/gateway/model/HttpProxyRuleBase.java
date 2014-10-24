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
package io.fabric8.gateway.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a collection of HTTP proxy mapping rules.
 * <p/>
 * These can be created via a Java DSL and XML DSL (JAXB) or loaded from a database.
 */
public class HttpProxyRuleBase {
    private Map<String, HttpProxyRule> mappingRules = new HashMap<String, HttpProxyRule>();

    public Map<String, HttpProxyRule> getMappingRules() {
        return mappingRules;
    }

    public void setMappingRules(Map<String, HttpProxyRule> mappingRules) {
        this.mappingRules = mappingRules;
    }

    /**
     * DSL API to create or update a mapping rule for the given URI template
     */
    public HttpProxyRule rule(String uriTemplate) {
        HttpProxyRule answer = getMappingRules().get(uriTemplate);
        if (answer == null) {
            answer = new HttpProxyRule(uriTemplate);
            getMappingRules().put(uriTemplate, answer);
        }
        return answer;
    }
}
