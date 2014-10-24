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

import io.fabric8.gateway.loadbalancer.ClientRequestFacade;
import io.fabric8.gateway.model.HttpProxyRuleBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 */
public class MappingRuleResolverTest extends MappingRuleTestSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(MappingRuleResolverTest.class);

    protected ClientRequestFacade requestFacade = new ClientRequestFacade() {
        @Override
        public String getClientRequestKey() {
            return "dummyClientKey";
        }
    };

    @Test
    public void testResolver() throws Exception {
        assertRuleMatch("/members", "http://foo.com/rest/members");
        assertRuleMatch("/members/10001", "http://foo.com/rest/members/10001");
        assertRuleMatch("/foo/something/else", "http://foo.com/cheese/something/else");
        assertRuleMatch("/customers/c123/address/abc", "http://another.com/addresses/abc/customerThingy/c123");
    }

    @Override
    protected void loadMappingRules(HttpProxyRuleBase ruleBase) {
        ruleBase.rule("/members").to("http://foo.com/rest/members");
        ruleBase.rule("/members/{id}").to("http://foo.com/rest/members/{id}");
        ruleBase.rule("/foo/{path}").to("http://foo.com/cheese/{path}");
        ruleBase.rule("/customers/{customerId}/address/{addressId}").to("http://another.com/addresses/{addressId}/customerThingy/{customerId}");
    }

    protected void assertRuleMatch(String requestUri, String expectedDestinationUrl) {
        MappingResult mappingRule = getResolver().findMappingRule(requestUri);
        assertNotNull("Could not find a HTTP Mapping rule for " + requestUri, mappingRule);
        LOG.info("request URI " + requestUri + " matched parameters :" + mappingRule.getParameterNameValues());
        String destinationUrl = mappingRule.getDestinationUrl(requestFacade);
        assertEquals("destinationUrl", expectedDestinationUrl, destinationUrl);
    }

}
