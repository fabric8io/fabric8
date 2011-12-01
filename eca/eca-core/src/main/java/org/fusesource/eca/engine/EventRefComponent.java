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

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.spi.FactoryFinder;
import org.fusesource.eca.eventcache.EventCacheManager;

// TODO: Find alternative way

/**
 * This is  a shameless hack.
 * In order to register or lookup the EventEngine
 */
public class EventRefComponent extends DefaultComponent {

    Map<String, EventEngine> engineMap = new HashMap<String, EventEngine>();
    Map<String, EventCacheManager> cacheMap = new HashMap<String, EventCacheManager>();

    public synchronized EventEngine getEventEngine(CamelContext context, String type) throws Exception {
        EventEngine eventEngine = engineMap.get(type);
        if (eventEngine == null) {
            FactoryFinder ff = context.getFactoryFinder("META-INF/services/org/fusesource/eca/engine/");
            if (ff != null) {
                eventEngine = (EventEngine) ff.newInstance(type);
                eventEngine.initialize(context, type);
                engineMap.put(type, eventEngine);
                if (context.getStatus().isStarted() || context.getStatus().isStarting()) {
                    eventEngine.start();
                }
            }
        }
        return eventEngine;
    }

    public synchronized EventCacheManager getEventCacheManager(CamelContext context, String type) throws Exception {
        EventCacheManager eventCacheManager = cacheMap.get(type);
        if (eventCacheManager == null) {
            FactoryFinder ff = context.getFactoryFinder("META-INF/services/org/fusesource/eca/eventcache/");
            if (ff != null) {
                eventCacheManager = (EventCacheManager) ff.newInstance(type);
                cacheMap.put(type, eventCacheManager);
                if (context.getStatus().isStarted()) {
                    eventCacheManager.start();
                }
            }
        }
        return eventCacheManager;
    }

    /**
     * Hope nobody ever calls this
     */
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        return null;
    }
}