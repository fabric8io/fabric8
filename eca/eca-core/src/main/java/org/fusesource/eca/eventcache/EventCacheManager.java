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
package org.fusesource.eca.eventcache;

import org.apache.camel.Service;

/**
 * Manager for EventCaches
 */
public interface EventCacheManager extends Service {

    /**
     * Retrieve or create a cache
     */
    <T> EventCache<T> getCache(Class<T> type, Object id, String size);

    /**
     * retrieve an existing cache
     *
     * @return the cache or null, if it doesn't exist
     */
    <T> EventCache<T> lookupCache(Class<T> type, Object id);

    /**
     * Remove a cache
     */
    boolean removeCache(Object id);
}
