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
package org.fusesource.eca.engine;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Service;
import org.fusesource.eca.eventcache.EventCache;
import org.fusesource.eca.expression.Expression;

/**
 * Event engine for handling Complex Event Processing
 */
public interface EventEngine extends Service {

    /**
     * Initialize the engine - which will add itself to the context
     *
     * @param context the Camel context
     * @param cacheImplementation name of the cache implementation, use <tt>default</tt> for default implementation.
     */
    public void initialize(CamelContext context, String cacheImplementation) throws Exception;

    /**
     * Add a route
     *
     * @param fromId the route id
     * @param window the window
     * @return the event cache
     */
    public EventCache<Exchange> addRoute(String fromId, String window);

    /**
     * Remove a route
     *
     * @param fromId  route id
     */
    public void removeRoute(String fromId);

    /**
     * Process an {@link Exchange}
     *
     * @param exchange the exchange
     */
    public void process(Exchange exchange);

    /**
     * Add an expression - equivalent of a rule
     *
     * @param expression the expression
     * @param listener   the expression listener
     */
    public void addExpression(Expression expression, ExpressionListener listener);

    /**
     * Remove an expression
     *
     * @param expression the expression
     */
    public void removeExpression(Expression expression);
}
