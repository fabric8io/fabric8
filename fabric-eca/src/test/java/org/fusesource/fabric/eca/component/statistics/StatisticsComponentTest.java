/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 FuseSource Corporation, a Progress Software company. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").
 * You may not use this file except in compliance with the License. You can obtain
 * a copy of the License at http://www.opensource.org/licenses/CDDL-1.0.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at resources/META-INF/LICENSE.txt.
 *
 */

package org.fusesource.fabric.eca.component.statistics;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.direct.DirectEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.CamelTestSupport;
import org.fusesource.fabric.eca.TestStat;

/**
 * @version $Revision: 1042541 $
 */
public class StatisticsComponentTest extends CamelTestSupport {
    final int COUNT = 1000;

    public void testStatsBatchUpdateWireTap() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");


        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(de).wireTap("statistics:test").to("mock:foo");
                from("statistics:test?batchUpdateTime=2sec").to("mock:result");
            }
        });
        context.start();

        MockEndpoint mock = context.getEndpoint("mock:result", MockEndpoint.class);
        mock.expectedMinimumMessageCount(1);

        for (int i = 0; i < COUNT; i++) {
            Exchange exchange = createExchange(i, i);
            template.send(de, exchange);
        }


        mock.assertIsSatisfied(context);
    }


    public void testStatsWireTap() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");


        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(de).wireTap("statistics:test").to("mock:foo");
                from("statistics:test").to("mock:result");
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

    public void testStatsProcessor() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");


        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from(de).to("statistics:foo").to("mock:result");
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

    public void testStatsQuery() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");


        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from(de).to("statistics:foo?queryString=simple,${body.queueDepth},$(body.enqueueTime)").to("mock:result");
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
            //System.err.println(exchange.getIn().getBody());
            Object result = exchange.getIn().getBody();
            assertTrue(String.class.isAssignableFrom(result.getClass()));
            String string = result.toString();
            assertTrue(string.indexOf("queueDepth") >= 0 && string.indexOf("enqueueTime") >= 0 && string.indexOf("testTime") < 0);
        }
    }

    public void testStatsType() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");


        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(de).to("statistics:foo?statisticsType=mean,min,max").to("mock:result");
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
            //System.err.println(exchange.getIn().getBody());
            Object result = exchange.getIn().getBody();
            assertTrue(String.class.isAssignableFrom(result.getClass()));
            String string = result.toString();
            assertTrue(string.indexOf("mean") >= 0 && string.indexOf("min") >= 0 && string.indexOf("rate") < 0);
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