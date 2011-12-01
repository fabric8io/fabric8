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
package org.fusesource.eca.expression;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.ServiceSupport;
import org.fusesource.eca.engine.EventEngine;
import org.fusesource.eca.eventcache.CacheItem;
import org.fusesource.eca.eventcache.EventCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CEP {@link Expression}.
 */
public class CepExpression extends ServiceSupport implements Expression {
    private static final transient Logger LOG = LoggerFactory.getLogger(CepExpression.class);
    private final String fromId;
    private final EventEngine eventEngine;
    private final String eventWindow;
    private EventCache<Exchange> eventCache;
    private final String id;

    /**
     * Create a CepExpression
     */
    public CepExpression(EventEngine eventEngine, String fromId, String window) {
        this.eventEngine = eventEngine;
        this.fromId = fromId;
        this.eventWindow = window;
        this.id = System.identityHashCode(this) + ":" + fromId;
        LOG.debug("CepExpression created for route {}", id);
    }

    public List<CacheItem<Exchange>> getMatching() {
        List<CacheItem<Exchange>> result = null;
        if (isMatch()) {
            result = eventCache.getCacheItems();
        }
        return result;
    }

    public boolean isMatch() {
        return !eventCache.isEmpty();
    }

    public void validate(CamelContext context) {
        //check if a route
        Object result = context.getRouteDefinition(fromId);
        if (result == null) {
            //check if an endpoint
            result = context.getEndpoint(fromId);
        }
        if (result == null) {
            throw new RuntimeCamelException("Failed to find RouteDefinition with id: " + fromId);
        }
    }

    public String getFromIds() {
        return fromId;
    }

    public String toString() {
        return "CepExpression:" + id + "[from:" + fromId + "]";
    }

    @Override
    protected void doStart() throws Exception {
        this.eventCache = eventEngine.addRoute(fromId, eventWindow);
    }

    @Override
    protected void doStop() throws Exception {
        // noop
    }
}
