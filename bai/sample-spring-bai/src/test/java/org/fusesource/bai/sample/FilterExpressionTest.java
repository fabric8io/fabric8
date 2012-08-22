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

package org.fusesource.bai.sample;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.management.event.AbstractExchangeEvent;
import org.apache.camel.management.event.ExchangeSentEvent;
import org.apache.camel.util.ExchangeHelper;
import org.fusesource.bai.AuditEvent;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit38.AbstractJUnit38SpringContextTests;

import java.util.List;

@ContextConfiguration
public class FilterExpressionTest extends AbstractJUnit38SpringContextTests {
    @EndpointInject(uri = "mock:audit")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @DirtiesContext
    public void testCamelRoute() throws Exception {
        String expectedBody = "<matched/>";
        String expectedHeader = "cheese";
        String ignoreBody = "<shouldNotMatch/>";

        resultEndpoint.expectedMessageCount(1);

        template.sendBodyAndHeader(ignoreBody, "foo", "ignored");
        template.sendBodyAndHeader(expectedBody, "foo", expectedHeader);
        template.sendBodyAndHeader(ignoreBody, "foo", "ignored");

        resultEndpoint.assertIsSatisfied();
        List<Exchange> exchanges = resultEndpoint.getExchanges();
        Exchange exchange = exchanges.get(0);
        AuditEvent auditEvent = exchange.getIn().getMandatoryBody(AuditEvent.class);
        System.out.println("Got: " + auditEvent);

        AbstractExchangeEvent event = auditEvent.getEvent();
        assertTrue("Should be a sent event", event instanceof ExchangeSentEvent);
        String body = auditEvent.getExchange().getIn().getBody(String.class);
        assertEquals("body of audit exchange", expectedBody, body);
        Object header = auditEvent.getExchange().getIn().getHeader("foo");
        assertEquals("foo header of audit exchange", expectedHeader, header);
    }
}
