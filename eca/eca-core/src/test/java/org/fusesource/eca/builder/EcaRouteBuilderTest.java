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
package org.fusesource.eca.builder;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.direct.DirectEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fusesource.eca.TestBlob;
import org.fusesource.eca.TestStat;

public class EcaRouteBuilderTest extends CamelTestSupport {

    final int COUNT = 1000;

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    public void XtestCepLookUp() throws Exception {
        context.start();
        Endpoint cep1 = context.getEndpoint("eca:test");
        Endpoint cep2 = context.getEndpoint("eca:test?window=10s");
        assertNotSame(cep1, cep2);
    }

    public void testSimpleEndpointCep() throws Exception {
        final String testEndPointUri = "direct://foo2?synchronous=false";
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct:foo");
        final Endpoint de2 = context.getEndpoint(testEndPointUri);

        context.addRoutes(new EcaRouteBuilder() {
            @Override
            public void configure() throws Exception {
                RouteDefinition route = from(de).to("mock:boo");
                eca("test").win("30 s").evaluate(testEndPointUri + " And route1").to("mock:result");
            }
        });
        context.start();

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(COUNT);

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de, exchange);
        }

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de2, exchange);
        }
        assertMockEndpointsSatisfied();
    }


    public void testSimpleCep() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");

        final DirectEndpoint de2 = new DirectEndpoint();
        de2.setCamelContext(context);
        de2.setEndpointUriIfNotSpecified("direct://foo2");

        context.addRoutes(new EcaRouteBuilder() {
            @Override
            public void configure() throws Exception {
                RouteDefinition route = from(de2);
                RouteDefinition route1 = from(de).to("mock:boo");
                eca("test").win("30 s").evaluate("route1 And route2").to("mock:result");
            }
        });
        context.start();

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(COUNT);

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de, exchange);
        }

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de2, exchange);
        }
        assertMockEndpointsSatisfied();
    }


    public void testSimpleAndCep() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");

        final DirectEndpoint de2 = new DirectEndpoint();
        de2.setCamelContext(context);
        de2.setEndpointUriIfNotSpecified("direct://foo2");

        context.addRoutes(new EcaRouteBuilder() {
            @Override
            public void configure() throws Exception {
                RouteDefinition route = from(de2);
                RouteDefinition route1 = from(de).to("mock:boo");

                eca("test").win("30 s").when(route).and(route1).to("mock:result");
            }
        });
        context.start();

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(COUNT);

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de, exchange);
        }

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de2, exchange);
        }
        assertMockEndpointsSatisfied();
    }

    public void testSimpleOrCep() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");

        final DirectEndpoint de2 = new DirectEndpoint();
        de2.setCamelContext(context);
        de2.setEndpointUriIfNotSpecified("direct://foo2");

        context.addRoutes(new EcaRouteBuilder() {
            @Override
            public void configure() throws Exception {
                RouteDefinition route = from(de2);
                RouteDefinition route1 = from(de).to("mock:boo");
                eca("test").win("30 s").when(route).or(route1).to("mock:result");

            }
        });
        context.start();

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(COUNT * 2);

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de, exchange);
        }

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de2, exchange);
        }
        assertMockEndpointsSatisfied();
    }


    public void testSimpleAfterCep() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");

        final DirectEndpoint de2 = new DirectEndpoint();
        de2.setCamelContext(context);
        de2.setEndpointUriIfNotSpecified("direct://foo2");

        context.addRoutes(new EcaRouteBuilder() {
            @Override
            public void configure() throws Exception {
                RouteDefinition route = from(de2);
                RouteDefinition route1 = from(de).to("mock:boo");
                eca("test").win("30 s").when(route).after(route1).to("mock:result");

            }
        });
        context.start();

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(COUNT);

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de, exchange);
        }

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de2, exchange);
        }
        assertMockEndpointsSatisfied();
    }

    public void testSimpleBeforeCep() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");

        final DirectEndpoint de2 = new DirectEndpoint();
        de2.setCamelContext(context);
        de2.setEndpointUriIfNotSpecified("direct://foo2");

        context.addRoutes(new EcaRouteBuilder() {
            @Override
            public void configure() throws Exception {
                RouteDefinition route = from(de2);
                RouteDefinition route1 = from(de).to("mock:boo");
                eca("test").win("30 s").when(route1).before(route).to("mock:result");

            }
        });
        context.start();

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(COUNT);

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de, exchange);
        }

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de2, exchange);
        }
        assertMockEndpointsSatisfied();
    }

    public void testCorrelationCep() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");

        final DirectEndpoint de2 = new DirectEndpoint();
        de2.setCamelContext(context);
        de2.setEndpointUriIfNotSpecified("direct://foo2");

        final String testEndPointUri = "direct://foo3?synchronous=false";
        final String testEndPointUri2 = "direct://foo4?synchronous=false";

        context.addRoutes(new EcaRouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(de2).filter(simple("${body.correlationID} == 'ID:2'")).to(testEndPointUri);
                from(de).filter(simple("${in.body.id} == 'ID:2'")).to(testEndPointUri2);
                eca("test").win("30 s").evaluate(testEndPointUri + " and " + testEndPointUri2).to("mock:result");

            }
        });
        context.start();

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createTestStatExchange(i);
            template.send(de, exchange);
        }

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createTestBlobExchange(i);
            template.send(de2, exchange);
        }

        assertMockEndpointsSatisfied();
    }

    public void testSimpleNotCep() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");

        final DirectEndpoint de2 = new DirectEndpoint();
        de2.setCamelContext(context);
        de2.setEndpointUriIfNotSpecified("direct://foo2");

        context.addRoutes(new EcaRouteBuilder() {
            @Override
            public void configure() throws Exception {
                RouteDefinition route = from(de2);
                RouteDefinition route1 = from(de).to("mock:boo");
                eca("test").when(route).not(route1).to("mock:result");
            }
        });
        context.start();

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(COUNT);

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de2, exchange);
        }
        assertMockEndpointsSatisfied();
    }

    public void testCompoundCep() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");

        final DirectEndpoint de2 = new DirectEndpoint();
        de2.setCamelContext(context);
        de2.setEndpointUriIfNotSpecified("direct://foo2");

        final String testEndPointUri = "direct://foo3?synchronous=false";

        context.addRoutes(new EcaRouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(de2);
                from(de).to("mock:boo");

                eca("test").win("30 s").evaluate("route1 and route2").to(testEndPointUri);

                eca("foo").evaluate(testEndPointUri).to("mock:result");

            }
        });
        context.start();

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(COUNT);

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de, exchange);
        }

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de2, exchange);
        }
        assertMockEndpointsSatisfied();
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

    protected Exchange createTestStatExchange(int id) {
        Exchange exchange = new DefaultExchange(context);
        Message message = exchange.getIn();

        TestStat testStat = new TestStat();
        testStat.setId("ID:" + id);

        message.setBody(testStat);
        return exchange;
    }

    protected Exchange createTestBlobExchange(int id) {
        Exchange exchange = new DefaultExchange(context);
        Message message = exchange.getIn();

        TestBlob testBlob = new TestBlob();
        testBlob.setCorrelationID("ID:" + id);

        message.setBody(testBlob);
        return exchange;
    }
}