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

package org.fusesource.bai.config;

import org.apache.camel.model.language.ExpressionDefinition;
import org.fusesource.bai.xml.PolicySetPropertiesSlurper;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.fusesource.bai.config.AuditAssertions.assertPolicyEnabled;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit Test for {@link PolicySetPropertiesSlurper}
 *
 * @author Raul Kripalani
 */
public class PolicySetPropertiesSlurperTest {

    @Test
    public void testLoadProperties() throws Exception {
        Properties properties = new Properties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream("policySet.properties"));

        PolicySetPropertiesSlurper slurper = new PolicySetPropertiesSlurper(properties);
        PolicySet policySet = slurper.slurp();
        List<Policy> policies = policySet.getPolicies();
        assertEquals("Policies were " + policies, 2, policies.size());

        Policy foo = policySet.policy("foo");
        Policy bar = policySet.policy("bar");
        assertNotNull(foo);
        assertNotNull(bar);

        assertEquals("Looks like policies were just created - wrong IDs loaded maybe?" + policies, 2, policies.size());

        System.out.println("foo: " + foo);
        System.out.println("bar: " + bar);

        assertPolicyEnabled(foo, true);
        assertPolicyEnabled(bar, false);

        assertEquals("foo.to", "mock:audit", foo.getTo());
        assertEquals("bar.to", "vm:audit", bar.getTo());

        assertNull("foo.context.excludes", foo.getContexts().getExcludeContextFilters());
        List<ContextFilter> fooContextIncludes = foo.getContexts().getIncludeContextFilters();
        assertEquals("foo.context.includes size", 1, fooContextIncludes.size());
        ContextFilter fooContextInclude0 = fooContextIncludes.get(0);
        assertNotNull(fooContextInclude0);
        assertEquals("foo.context.includes[0].bundle", "com.fusesource.mybundle.one", fooContextInclude0.getBundle());
        assertEquals("foo.context.includes[0].name", "context1", fooContextInclude0.getName());

        assertNull("bar.context.includes", bar.getContexts().getIncludeContextFilters());
        List<ContextFilter> barContextExcludes = bar.getContexts().getExcludeContextFilters();
        assertEquals("bar.context.includes size", 1, barContextExcludes.size());
        ContextFilter barContextExclude0 = barContextExcludes.get(0);
        assertNotNull(barContextExclude0);
        assertEquals("bar.context.excludes[0].bundle", "*", barContextExclude0.getBundle());
        assertEquals("bar.context.excludes[0].name", "audit-*", barContextExclude0.getName());

        assertNull("foo.endpoints.exclude", foo.getEndpoints().getExcludeEndpointFilters());
        List<EndpointFilter> fooEndpointsInclude = foo.getEndpoints().getIncludeEndpointFilters();
        assertEquals("foo.endpoint.includes size", 1, fooEndpointsInclude.size());
        EndpointFilter fooEndpointInclude0 = fooEndpointsInclude.get(0);
        assertNotNull(fooEndpointInclude0);
        assertEquals("foo.endpoint.include[0].pattern", "activemq:*", fooEndpointInclude0.getPattern());

        assertNull("foo.events.include", foo.getEvents().getIncludeEventFilters());
        List<EventFilter> fooEventsExclude = foo.getEvents().getExcludeEventFilters();
        assertEquals("foo.event.excludes size", 1, fooEventsExclude.size());
        EventFilter fooEventExclude0 = fooEventsExclude.get(0);
        assertNotNull(fooEventExclude0);
        assertEquals("foo.event.exclude[0].pattern", EventType.FAILURE_HANDLED, fooEventExclude0.getEventType());

        assertNull("bar.endpoints", bar.getEndpoints());
        assertNull("bar.events", bar.getEvents());
        assertNull("bar.filter", bar.getFilter());

        ExchangeFilter fooFilter = foo.getFilter();
        assertNotNull("foo.filter", fooFilter);
        ExpressionDefinition fooFilterExp = fooFilter.getExpression();
        assertNotNull("foo.filter.exp", fooFilterExp);
        assertEquals("foo.filter.language", "xpath", fooFilterExp.getLanguage());
        assertEquals("foo.filter.expression", "/foo/@id = 'bar'", fooFilterExp.getExpression());
    }

}
