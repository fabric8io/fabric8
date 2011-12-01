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
import org.apache.camel.Service;
import org.fusesource.eca.eventcache.CacheItem;

/**
 * Represents an expression
 */
public interface Expression extends Service {

    /**
     * If {@link Expression} is positive all the exchanges held by the event cache
     * which matches will be returned.
     *
     * @return A list of matching results, or <tt>null</tt> if no matching results.
     */
    List<CacheItem<Exchange>> getMatching();

    /**
     * Determines if currently matched.
     *
     * @return true if currently matched
     */
    boolean isMatch();

    /**
     * Validate the expression
     *
     * @param context the camel context
     */
    void validate(CamelContext context);

    /**
     * Gets the key used to create the expression, or multiple, comma separated keys
     *
     * @return the key used to create the expression, or multiple, comma separated keys
     */
    String getFromIds();

}
