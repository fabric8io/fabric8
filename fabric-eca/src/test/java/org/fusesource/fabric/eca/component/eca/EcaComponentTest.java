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
package org.fusesource.fabric.eca.component.eca;

import org.apache.camel.Endpoint;
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
public class EcaComponentTest extends CamelTestSupport {
    final int COUNT = 1000;

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }


    public void testSimpleEvaluationCep() throws Exception {
        final DirectEndpoint de = new DirectEndpoint();
        de.setCamelContext(context);
        de.setEndpointUriIfNotSpecified("direct://foo");

        final DirectEndpoint de2 = new DirectEndpoint();
        de2.setCamelContext(context);
        de2.setEndpointUriIfNotSpecified("direct://foo2");

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                Endpoint cep = getContext().getEndpoint("eca://test?pattern=route1 And route2");

                from(de).to(cep);
                from(de2).to(cep);
                from(cep.getEndpointUri()).to("mock:result");
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