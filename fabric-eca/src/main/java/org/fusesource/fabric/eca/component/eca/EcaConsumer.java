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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.seda.SedaConsumer;
import org.fusesource.fabric.eca.engine.ExpressionListener;
import org.fusesource.fabric.eca.expression.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EcaConsumer extends SedaConsumer implements ExpressionListener {
    private static final transient Logger LOG = LoggerFactory.getLogger(EcaConsumer.class);
    private final EcaEndpoint ecaEndpoint;

    public EcaConsumer(EcaEndpoint ecaEndpoint, Processor processor) {
        super(ecaEndpoint, processor);
        this.ecaEndpoint = ecaEndpoint;
    }

    public EcaEndpoint getEcaEndpoint() {
        return this.ecaEndpoint;
    }

    protected void sendToConsumers(Exchange exchange) throws Exception {
        getEcaEndpoint().evaluate(exchange);
    }

    protected void doSendToConsumers(Exchange exchange) throws Exception {
        super.sendToConsumers(exchange);
    }

    /**
     * Implementation of ExpressionListener
     */
    public void expressionFired(Expression expression, Exchange exchange) {
        try {
            /*
            Exchange copy = exchange.copy();
            copy.setFromEndpoint(getEcaEndpoint());
            copy.setFromRouteId(getEcaEndpoint().getCepRouteId());
            Object result = getEcaEndpoint().getEvaluatedResults();
            copy.getIn().setBody(result);

            doSendToConsumers(copy);
            */
            doSendToConsumers(exchange);
        } catch (Exception e) {
            LOG.warn("Failed to sendToConsumers " + e);
        }
    }

    protected void doStart() throws Exception {
        super.doStart();
        getEcaEndpoint().addExpression(this);
    }

    protected void doStop() throws Exception {
        super.doStop();
        getEcaEndpoint().removeExpression(this);
    }

}
