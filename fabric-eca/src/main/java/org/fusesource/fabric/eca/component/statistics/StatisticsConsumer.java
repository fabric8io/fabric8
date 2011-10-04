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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.seda.SedaConsumer;
import org.fusesource.fabric.eca.processor.StatisticsProcessor;
import org.fusesource.fabric.eca.util.ParsingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsConsumer extends SedaConsumer {
    private static final transient Logger LOG = LoggerFactory.getLogger(StatisticsConsumer.class);
    private volatile Exchange lastGeneratedExchange;
    private final boolean polling;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> future;

    public StatisticsConsumer(StatisticsEndpoint statisticsEndpoint, Processor processor) {
        super(statisticsEndpoint, processor);
        this.polling = statisticsEndpoint.isPolling();
        if (polling) {
            this.executor = statisticsEndpoint.getCamelContext().getExecutorServiceStrategy()
                    .newScheduledThreadPool(this, statisticsEndpoint.getEndpointUri(), 1);
        } else {
            this.executor = null;
        }
    }

    @Override
    public StatisticsEndpoint getEndpoint() {
        return (StatisticsEndpoint) super.getEndpoint();
    }

    protected void sendToConsumers(Exchange exchange) throws Exception {
        Object result = null;
        if (!StatisticsProcessor.isAlreadyProcessedForStatistics(exchange)) {
            result = getEndpoint().getStatsProcessor().processExchange(exchange);
        }
        if (result != null) {
            Exchange copy = exchange.copy();
            copy.getIn().setBody(result);
            lastGeneratedExchange = copy;
        } else {
            lastGeneratedExchange = exchange;
        }
        if (!polling) {
            doSuperSendToConsumers(lastGeneratedExchange);
        }
    }

    protected void doSuperSendToConsumers(Exchange exchange) throws Exception {
        super.sendToConsumers(exchange);
    }

    protected void doStart() throws Exception {
        super.doStart();

        if (polling) {
            long scheduleTime = ParsingUtil.getTimeAsMilliseconds(getEndpoint().getBatchUpdateTime());
            LOG.debug("Scheduled StatisticsConsumer to batch every {}", getEndpoint().getBatchUpdateTime());
            final Runnable runnable = new Runnable() {
                public void run() {
                    Exchange exchange = lastGeneratedExchange;
                    if (exchange != null) {
                        try {
                            StatisticsConsumer.this.doSuperSendToConsumers(exchange);
                        } catch (Throwable e) {
                            LOG.warn("Failed to send batch statistics to " + getEndpoint().getEndpointUri() + ". This exception will be ignored.", e);
                        }
                    }
                }
            };
            future = executor.scheduleAtFixedRate(runnable, scheduleTime, scheduleTime, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (future != null) {
            future.cancel(false);
        }
        super.doStop();
    }

}
