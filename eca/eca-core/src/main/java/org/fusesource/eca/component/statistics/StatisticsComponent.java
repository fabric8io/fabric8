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
package org.fusesource.eca.component.statistics;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.component.seda.SedaComponent;

/**
 * The statistics component.
 * <p/>
 * Valid properties are:
 * <ul>
 * <li>eventWindow - default =  "30s,1000" can be either depth based (numeric value), time based (ms,sec,min) or both</li>
 * <li>batchUpdateTime - default = null - a time based parameter for how often a stats message should be sent</li>
 * <li>cacheImplementation = "default" - the name of the cache implementation</li>
 * <li>queryString can be a comma separated list: If the first parameter equals a language, that will be used to create the query of the exchange,
 * else it will default to <I>simple<I></li>
 * <li>statisticsType  default = "ALL" - one of ALL, MEAN, MIN, MAX, VARIANCE, STDDEV, SKEWNESS, KUTOSIS, RATE - or a comma separated list of any of these</li>
 * </ul>
 */
public class StatisticsComponent extends SedaComponent {

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        int consumers = getAndRemoveParameter(parameters, "concurrentConsumers", Integer.class, 1);
        boolean limitConcurrentConsumers = getAndRemoveParameter(parameters, "limitConcurrentConsumers", Boolean.class, true);
        if (limitConcurrentConsumers && consumers > maxConcurrentConsumers) {
            throw new IllegalArgumentException("The limitConcurrentConsumers flag in set to true. ConcurrentConsumers cannot be set at a value greater than "
                    + maxConcurrentConsumers + " was " + consumers);
        }

        StatisticsEndpoint answer = new StatisticsEndpoint(uri, this, createQueue(uri, parameters), consumers);
        answer.configureProperties(parameters);
        return answer;
    }

}
