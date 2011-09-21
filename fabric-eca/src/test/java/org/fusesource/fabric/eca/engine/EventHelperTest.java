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

import junit.framework.TestCase;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.fusesource.fabric.eca.eventcache.EventCacheManager;

public class EventHelperTest extends TestCase {

    public void testGetCacheManager() throws Exception {
        CamelContext context = new DefaultCamelContext();
        EventCacheManager cacheManager1 = EventHelper.getEventCacheManager(context, "default");
        assertNotNull(cacheManager1);
        EventCacheManager cacheManager2 = EventHelper.getEventCacheManager(context, "default");
        assertNotNull(cacheManager2);
        assertTrue(cacheManager1 == cacheManager2);
    }

    public void testEventEngineManager() throws Exception {
        CamelContext context = new DefaultCamelContext();
        EventEngine eventEngine1 = EventHelper.getEventEngine(context, "default");
        assertNotNull(eventEngine1);
        EventEngine eventEngine2 = EventHelper.getEventEngine(context, "default");
        assertNotNull(eventEngine2);
        assertTrue(eventEngine1 == eventEngine2);
    }
}
