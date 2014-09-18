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

import io.fabric8.gateway.model.HttpProxyRuleBase;
import org.junit.Before;

/**
 * Base class for test cases for mapping rules
 */
public abstract class MappingRuleTestSupport {
    private MappingRuleResolver resolver = new MappingRuleResolver();

    @Before
    public void init() {
        loadMappingRules(getResolver().getMappingRules());
    }

    /**
     * Strategy method to load the mapping rules from the test case
     */
    protected abstract void loadMappingRules(HttpProxyRuleBase ruleBase);

    public MappingRuleResolver getResolver() {
        return resolver;
    }

    public void setResolver(MappingRuleResolver resolver) {
        this.resolver = resolver;
    }
}
