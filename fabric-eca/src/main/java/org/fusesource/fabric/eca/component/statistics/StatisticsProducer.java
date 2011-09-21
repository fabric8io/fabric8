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

import java.util.concurrent.BlockingQueue;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.WaitForTaskToComplete;
import org.apache.camel.component.seda.SedaProducer;
import org.fusesource.fabric.eca.processor.StatisticsProcessor;

public class StatisticsProducer extends SedaProducer {
    private final StatisticsEndpoint statisticsEndpoint;

    public StatisticsProducer(StatisticsEndpoint endpoint, BlockingQueue<Exchange> queue, WaitForTaskToComplete waitForTaskToComplete, long timeout) {
        super(endpoint, queue, waitForTaskToComplete, timeout);
        this.statisticsEndpoint = endpoint;
    }

    public StatisticsEndpoint getStatisticsEndpoint() {
        return this.statisticsEndpoint;
    }

    public boolean process(final Exchange exchange, final AsyncCallback callback) {
        Object result = null;
        if (!StatisticsProcessor.isAlreadyProcessedForStatistics(exchange)) {
            try {
                result = getStatisticsEndpoint().getStatsProcessor().processExchange(exchange);
            } catch (Exception e) {
                exchange.setException(e);
            }
            if (result != null) {
                if (exchange.getPattern().isOutCapable()) {
                    exchange.getOut().setBody(result);
                } else {
                    exchange.getIn().setBody(result);
                }
            }
        }

        return super.process(exchange, callback);
    }
}
