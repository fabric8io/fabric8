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
package org.fusesource.fabric.eca.eventcache;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.impl.ServiceSupport;

public class DefaultEventCacheManager extends ServiceSupport implements EventCacheManager {

    private final Map<Object, EventCache<?>> caches = new HashMap<Object, EventCache<?>>();

    public synchronized <T> EventCache<T> getCache(Class<T> type, Object id, String size) {
        EventCache result = caches.get(id);
        if (result == null) {
            result = new DefaultEventCache<T>(id, size);
            caches.put(id, result);
        }
        return result;
    }

    /**
     * retrieve an existing cache
     *
     * @return the cache or null, if it doesn't exist
     */
    public <T> EventCache<T> lookupCache(Class<T> type, Object id) {
        EventCache result = caches.get(id);
        return result;
    }

    public synchronized boolean removeCache(Object id) {
        EventCache result = caches.remove(id);
        return result != null;
    }

    @Override
    protected void doStart() throws Exception {
        // noop
    }

    @Override
    protected void doStop() throws Exception {
        caches.clear();
    }
}
