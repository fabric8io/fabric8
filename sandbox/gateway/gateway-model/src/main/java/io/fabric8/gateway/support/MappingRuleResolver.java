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
package io.fabric8.gateway.support;

import io.fabric8.gateway.model.HttpProxyRule;
import io.fabric8.gateway.model.HttpProxyRuleBase;

/**
 * A helper class to map a request URI to a mapping rule
 */
public class MappingRuleResolver {
    private HttpProxyRuleBase mappingRules = new HttpProxyRuleBase();

    public MappingResult findMappingRule(String requestURI) {
        String[] paths = Paths.splitPaths(requestURI);
        MappingResult answer = null;
        // TODO we could build a path based tree to do more efficient matching?
        for (HttpProxyRule mappingRule : mappingRules.getMappingRules().values()) {
            answer = mappingRule.matches(paths);
            if (answer != null) {
                break;
            }
        }
        return answer;
    }

    public HttpProxyRuleBase getMappingRules() {
        return mappingRules;
    }

    public void setMappingRules(HttpProxyRuleBase mappingRules) {
        this.mappingRules = mappingRules;
    }

}
