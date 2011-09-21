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
package org.fusesource.fabric.eca.engine;

import org.apache.camel.CamelContext;
import org.fusesource.fabric.eca.eventcache.EventCacheManager;

/**
 * Helper class to retrieve caches
 */
public class EventHelper {
    private static final String NAME = "EventRefComponent";

    /**
     * Get the default EventEngine
     */
    public static EventEngine getEventEngine(CamelContext context) throws Exception {
        return getEventEngine(context, "default");
    }

    /**
     * Get the named EventEngine
     */
    public static synchronized EventEngine getEventEngine(CamelContext context, String type) throws Exception {
        EventRefComponent eventRefComponent = (EventRefComponent) context.getComponent(NAME);
        if (eventRefComponent == null) {
            eventRefComponent = new EventRefComponent();
            context.addComponent(NAME, eventRefComponent);
        }
        return eventRefComponent.getEventEngine(context, type);
    }

    /**
     * Get the default EventCacheManager
     */
    public static EventCacheManager getEventCacheManager(CamelContext context) throws Exception {
        return getEventCacheManager(context, "default");
    }

    /**
     * Get the named EventCacheManager
     */
    public static synchronized EventCacheManager getEventCacheManager(CamelContext context, String type) throws Exception {
        EventRefComponent eventRefComponent = (EventRefComponent) context.getComponent(NAME);
        if (eventRefComponent == null) {
            eventRefComponent = new EventRefComponent();
            context.addComponent(NAME, eventRefComponent);
        }
        return eventRefComponent.getEventCachManager(context, type);
    }
}
