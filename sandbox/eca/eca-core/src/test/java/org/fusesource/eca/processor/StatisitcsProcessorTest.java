/**
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

package org.fusesource.eca.processor;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.direct.DirectEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fusesource.eca.TestStat;

public class StatisitcsProcessorTest extends CamelTestSupport {
    final int COUNT = 10;

    public void testStatsProcessor() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");

        final Processor statsProcessor = new StatisticsProcessor(context);
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(de).process(statsProcessor).to("mock:result");
            }
        });
        context.start();

        MockEndpoint mock = context.getEndpoint("mock:result", MockEndpoint.class);
        mock.expectedMessageCount(COUNT);

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de, exchange);
        }

        mock.assertIsSatisfied(context);

        List<Exchange> list = mock.getReceivedExchanges();
        for (Exchange exchange : list) {
            assertTrue(String.class.isAssignableFrom(exchange.getIn().getBody().getClass()));
        }
    }

    protected Exchange createExchange(int queueDepth, long enqueueTime) {
        Exchange exchange = new DefaultExchange(context);
        Message message = exchange.getIn();

        TestStat testStat = new TestStat();
        testStat.setQueueDepth(queueDepth);
        testStat.setEnqueueTime(enqueueTime);

        message.setBody(testStat);
        return exchange;
    }

}