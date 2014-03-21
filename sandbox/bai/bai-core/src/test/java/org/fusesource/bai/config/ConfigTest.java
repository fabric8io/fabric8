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

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.management.event.AbstractExchangeEvent;
import org.apache.camel.management.event.ExchangeCreatedEvent;
import org.apache.camel.management.event.ExchangeFailureHandledEvent;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.model.language.XPathExpression;
import org.fusesource.bai.AuditEvent;
import org.fusesource.bai.xml.ConfigHelper;
import org.junit.Test;

import static org.fusesource.bai.config.AuditAssertions.assertMatchesContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfigTest {
    private CamelContext camelContext = new DefaultCamelContext();
    private Endpoint endpointSeda = camelContext.getEndpoint("seda:a");
    private Endpoint endpointVm = camelContext.getEndpoint("vm:b");
    private Object bodyA = "<person name='James'/>";
    private Object bodyB = "<person name='Raul'/>";

    @Test
    public void configMatches() throws Exception {
        PolicySet config = ConfigHelper.loadConfigFromClassPath("simpleConfig.xml");

        assertMatchesContext(config, true, "com.acme.foo", "myContext");
        assertMatchesContext(config, false, "com.acme.foo", "audit-foo");

        assertMatchesEvent(config, true, endpointSeda, EventType.CREATED, bodyA);
        assertMatchesEvent(config, false, endpointVm, EventType.CREATED, bodyA);
        assertMatchesEvent(config, false, endpointSeda, EventType.FAILURE_HANDLED, bodyA);
        assertMatchesEvent(config, false, endpointSeda, EventType.CREATED, bodyB);

        Policy policy = config.getPolicies().get(0);
        assertEquals("policies[0].to", "seda:dummy", policy.getTo());
        ExchangeFilter filter = policy.getFilter();
        assertNotNull(filter);
        ExpressionDefinition expression = filter.getExpression();
        assertTrue("expression should be XPath but was " + expression, expression instanceof XPathExpression);
        XPathExpression xpath = (XPathExpression) expression;
        assertEquals("XPath expression", "/person/@name = 'James'", xpath.getExpression());
    }

    @Test
    public void configMatchesWithNoContextDefintions() throws Exception {
        assertMatchesContext("noContexts.xml", true, "com.acme.foo", "myContext");
    }

    protected AuditEvent createAuditEvent(Endpoint endpoint, EventType eventType, Object body) {
        Exchange exchange = new DefaultExchange(endpoint);
        exchange.getIn().setBody(body);
        AbstractExchangeEvent event = createEvent(eventType, exchange);
        return new AuditEvent(exchange, event);
    }

    protected AbstractExchangeEvent createEvent(EventType eventType, Exchange exchange) {
        switch (eventType) {
            case FAILURE_HANDLED:
                return new ExchangeFailureHandledEvent(exchange, new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                    }
                }, false);
            default:
                return new ExchangeCreatedEvent(exchange);
        }
    }

    protected void assertMatchesEvent(PolicySet config, boolean expected, Endpoint endpoint, EventType eventType, Object body) {
        AuditEvent auditEvent = createAuditEvent(endpoint, eventType, body);
        boolean actual = config.matchesEvent(auditEvent);
        assertEquals("Matches " + endpoint + " " + eventType + " body: " + body + " for config " + config + " event: " + auditEvent, expected, actual);
    }
}
