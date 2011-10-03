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
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.component.seda.SedaEndpoint;
import org.fusesource.fabric.eca.processor.StatisticsProcessor;


public class StatisticsEndpoint extends SedaEndpoint {

    private StatisticsProcessor statisticsProcessor;
    private String eventWindow = "30s,1000";
    private String batchUpdateTime = "";
    private String cacheImplementation = "default";
    private String cacheId = "";
    private String queryString;
    private String statisticsType = "ALL";

    public StatisticsEndpoint() {
    }

    public StatisticsEndpoint(String endpointUri, Component component, BlockingQueue<Exchange> queue) {
        this(endpointUri, component, queue, 1);
    }

    public StatisticsEndpoint(String endpointUri, Component component, BlockingQueue<Exchange> queue, int concurrentConsumers) {
        super(endpointUri, component, queue, concurrentConsumers);
    }

    public StatisticsEndpoint(String endpointUri, BlockingQueue<Exchange> queue) {
        this(endpointUri, queue, 1);
    }

    public StatisticsEndpoint(String endpointUri, BlockingQueue<Exchange> queue, int concurrentConsumers) {
        super(endpointUri, queue, concurrentConsumers);
    }

    /**
     * Creates a new producer which is used send messages into the endpoint
     *
     * @return a newly created producer
     * @throws Exception can be thrown
     */
    public Producer createProducer() throws Exception {
        return new StatisticsProducer(this, getQueue(), getWaitForTaskToComplete(), getTimeout());
    }

    /**
     * Creates a new <a
     * href="http://camel.apache.org/event-driven-consumer.html">Event
     * Driven Consumer</a> which consumes messages from the endpoint using the
     * given processor
     *
     * @param processor the given processor
     * @return a newly created consumer
     * @throws Exception can be thrown
     */
    public Consumer createConsumer(Processor processor) throws Exception {
        return new StatisticsConsumer(this, processor);
    }

    public String getEventWindow() {
        return eventWindow;
    }

    public String getWin() {
        return getEventWindow();
    }

    public String getWindow() {
        return getEventWindow();
    }

    public void setEventWindow(String eventWindow) {
        this.eventWindow = eventWindow;
    }

    public void setWin(String win) {
        setEventWindow(win);
    }

    public void setWindow(String window) {
        setEventWindow(window);
    }

    public String getCacheImplementation() {
        return cacheImplementation;
    }

    public void setCacheImplementation(String cacheImplementation) {
        this.cacheImplementation = cacheImplementation;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getCacheId() {
        return cacheId;
    }

    public void setCacheId(String cacheId) {
        this.cacheId = cacheId;
    }

    public String getBatchUpdateTime() {
        return batchUpdateTime;
    }

    public boolean isPolling() {
        return batchUpdateTime != null && batchUpdateTime.length() > 0;
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

    /**
     * Processes the message exchange.
     * Similar to {@link org.apache.camel.Processor#process}, but the caller supports having the exchange asynchronously processed.
     * <p/>
     * If there was a failure processing then the caused {@link Exception} would be set on the {@link org.apache.camel.Exchange}.
     *
     * @param exchange the message exchange
     * @param callback the {@link org.apache.camel.AsyncCallback} will be invoked when the processing of the exchange is completed.
     *                 If the exchange is completed synchronously, then the callback is also invoked synchronously.
     *                 The callback should therefore be careful of starting recursive loop.
     * @return (doneSync) <tt>true</tt> to continue execute synchronously, <tt>false</tt> to continue being executed asynchronously
     * @see org.apache.camel.util.AsyncProcessorHelper#process(org.apache.camel.AsyncProcessor, org.apache.camel.Exchange, org.apache.camel.AsyncCallback)
     */
    public boolean process(Exchange exchange, AsyncCallback callback) {
        try {
            return getStatsProcessor().process(exchange, callback);
        } catch (Exception e) {
            exchange.setException(e);
        }
        callback.done(true);
        return true;
    }

    /**
     * Processes the message exchange
     *
     * @param exchange the message exchange
     * @throws Exception if an internal processing error has occurred.
     */
    public void process(Exchange exchange) throws Exception {
        statisticsProcessor.process(exchange);
    }

    public synchronized StatisticsProcessor getStatsProcessor() throws Exception {
        if (isStarted()) {
            if (this.statisticsProcessor == null) {
                Processor childProcessor = null;
                this.statisticsProcessor = new StatisticsProcessor(getCamelContext(), childProcessor, getCacheId(), getEventWindow(), getBatchUpdateTime(), getStatisticsType(), getQueryString());
                this.statisticsProcessor.setCacheImplementation(getCacheImplementation());
                this.statisticsProcessor.start();
            }
        }
        return this.statisticsProcessor;
    }

    @Override
    protected void doStop() throws Exception {
        if (statisticsProcessor != null) {
            statisticsProcessor.stop();
        }
        super.doStop();
    }
}
