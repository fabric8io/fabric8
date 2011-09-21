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

package org.fusesource.fabric.eca.processor;

import org.apache.camel.AsyncCallback;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.processor.DelegateAsyncProcessor;
import org.apache.camel.processor.Traceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates statistics on exchanges passed in
 *
 * @version $Revision: 959391 $
 */
public class StatisticsProcessor extends DelegateAsyncProcessor implements Traceable {
    public final static String STATS_CALCULATED = "statisticsCalculated";
    protected final transient Logger LOG = LoggerFactory.getLogger(getClass());
    private final String STATS_PROCESSED = "ProcessedForStatistics";
    private final CamelContext context;
    private StatisticsCalculator statisticsCalculator;
    private String cacheId;
    private String eventWindow = "30s,1000";
    private String batchUpdateTime = "";
    private String statisticsType = "ALL";
    private String queryString;
    private String cacheImplementation = "default";


    /**
     * Constructor
     */
    public StatisticsProcessor(CamelContext context) {
        this(context, null);
    }


    /**
     * Constructor
     */
    public StatisticsProcessor(CamelContext context, Processor processor) {
        this(context, processor, null);
    }

    /**
     * Constructor
     *
     * @param queryString - can be a comma separated list:
     *                    If the first parameter equals a language, that will be used to create the query of the exchange,
     *                    else it will default to <I>simple<I>
     */

    public StatisticsProcessor(CamelContext context, Processor processor, String queryString) {
        this(context, processor, "StatisticsProcessor:" + System.identityHashCode(context), "30s", "", "ALL", queryString);
    }

    /**
     * @param statisticsType default = "ALL" - one of ALL, MEAN, MIN, MAX, VARIANCE, STDDEV, SKEWNESS, KUTOSIS, RATE - or a comma separated list of any of these
     * @param queryString    - can be a comma separated list:
     *                       If the first parameter equals a language, that will be used to create the query of the exchange,
     *                       else it will default to <I>simple<I>
     */

    public StatisticsProcessor(CamelContext context, Processor processor, String statisticsType, String queryString) {
        this(context, processor, "StatisticsProcessor:" + System.identityHashCode(context), "30s", "", statisticsType, queryString);
    }

    /**
     * Constructor
     *
     * @param batchUpdateTime - default is none - use a number (ms) - or defined type - e.g. 10ms, 10s(ecs) etc
     * @param statisticsType  - default = "ALL" - one of ALL, MEAN, MIN, MAX, VARIANCE, STDDEV, SKEWNESS, KUTOSIS, RATE - or a comma separated list of any of these
     * @param queryString     - can be a comma separated list:
     *                        If the first parameter equals a language, that will be used to create the query of the exchange,
     *                        else it will default to <I>simple<I>
     */
    public StatisticsProcessor(CamelContext context, Processor processor, String cacheId, String eventWindow, String batchUpdateTime, String statisticsType, String queryString) {
        super(processor);
        this.context = context;
        this.cacheId = cacheId;
        this.eventWindow = eventWindow;
        this.batchUpdateTime = batchUpdateTime;
        this.statisticsType = statisticsType;
        this.queryString = queryString;
    }

    public static boolean isAlreadyProcessedForStatistics(Exchange exchange) {
        boolean result = false;
        if (exchange != null) {
            Boolean b = (Boolean) exchange.getProperty(StatisticsProcessor.STATS_CALCULATED, Boolean.class);
            if (b != null && b.booleanValue()) {
                result = true;
            }
        }
        return result;
    }

    public static void markProcessedForStatistics(Exchange exchange) {
        if (exchange != null) {
            exchange.setProperty(StatisticsProcessor.STATS_CALCULATED, Boolean.TRUE);
        }
    }


    public boolean process(Exchange exchange, AsyncCallback callback) {
        Object statistics = processExchange(exchange);
        if (statistics != null) {
            if (exchange.getPattern().isOutCapable()) {
                exchange.getOut().setBody(statistics);
            } else {
                exchange.getIn().setBody(statistics);
            }
        }

        callback.done(true);
        return true;
    }

    public Object processExchange(Exchange exchange) {
        Object result = null;
        try {
            result = getStatisticsCalculator().calculateStatistics(exchange);
            StatisticsProcessor.markProcessedForStatistics(exchange);
        } catch (Exception e) {
            exchange.setException(e);
        }
        return result;
    }

    @Override
    public String toString() {
        return "statistics [" + queryString + "]";
    }

    public String getTraceLabel() {
        return "stats";
    }

    public String getCacheId() {
        return cacheId;
    }

    public void setCacheId(String id) {
        this.cacheId = id;
    }

    public String getEventWindow() {
        return eventWindow;
    }

    public void setEventWindow(String eventWindow) {
        this.eventWindow = eventWindow;
    }

    public String getBatchUpdateTime() {
        return batchUpdateTime;
    }

    public void setBatchUpdateTime(String batchUpdateTime) {
        this.batchUpdateTime = batchUpdateTime;
    }

    public String getStatisticsType() {
        return statisticsType;
    }

    public void setStatisticsType(String statisticsType) {
        this.statisticsType = statisticsType;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getCacheImplementation() {
        return cacheImplementation;
    }

    public void setCacheImplementation(String cacheImplementation) {
        this.cacheImplementation = cacheImplementation;
    }

    public StatisticsCalculator getStatisticsCalculator() throws Exception {
        if (isStarted()) {
            if (this.statisticsCalculator == null) {
                this.statisticsCalculator = new StatisticsCalculator(context, getCacheId(), getEventWindow(), getQueryString());
                this.statisticsCalculator.setstatisticsType(getStatisticsType());
                this.statisticsCalculator.setCacheImplementation(getCacheImplementation());
                this.statisticsCalculator.start();
            }
        }
        return statisticsCalculator;
    }

    @Override
    protected void doStop() throws Exception {
        this.statisticsCalculator.stop();
        super.doStop();
    }

}