/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.gateway.support;

import io.fabric8.gateway.model.HttpProxyRuleBase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 */
public class MappingRuleResolverTest extends MappingRuleTestSupport {

    @Test
    public void testResolver() throws Exception {
        assertRuleMatch("/foo/something/else", "http://something.whatnot.com/cheese/something/else");
    }

    protected void assertRuleMatch(String requestUri, String expectedAnswer) {
        MappingResult mappingRule = getResolver().findMappingRule(requestUri);
        assertNotNull("Could not find a HTTP Mapping rule for " + requestUri, mappingRule);
        System.out.println("Parameters: " + mappingRule.getParameterNameValues());
    }

    @Override
    protected void loadMappingRules(HttpProxyRuleBase ruleBase) {
        ruleBase.rule("/foo/{path}").to("http://foo.com/cheese/{path}");
/*
        ruleBase.rule("/xyz/bar/{path}").to("http://something.whatnot.com/cheese/{path}");
        ruleBase.rule("/another/{id}/thing/{path}").to("http://another.com/{id}/bar");
*/
    }
}
