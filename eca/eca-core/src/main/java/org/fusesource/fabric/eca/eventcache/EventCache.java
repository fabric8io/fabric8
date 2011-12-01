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

import java.util.List;

/**
 * An insertion ordered cache of objects
 */
public interface EventCache<T> {

    /**
     * Add an item to the cache
     *
     * @return true if not already added
     */
    boolean add(T item);


    /**
     * Get a List of items in the Cache
     */
    List<T> getWindow();

    /**
     * Get raw cacheItems
     *
     * @return List of CacheItems
     */
    List<CacheItem<T>> getCacheItems();

    /**
     * Set the window for the cache
     */
    void setWindow(String text) throws IllegalArgumentException;

    /**
     * Get the maximum number of items for the cache
     *
     * @return the maximum number of items in the cache
     */
    int getWindowCount();

    /**
     * Set the maximum number of items for the cache
     */
    void setWindowCount(int windowCount);

    /**
     * Get the time items can live in the cache
     *
     * @return the time in milliseconds
     */
    long getWindowTime();

    /**
     * Set the time items can live in the cache
     */
    void setWindowTime(long windowTime);

    /**
     * IS the cache empty ?
     *
     * @return true if the cache is empty
     */
    boolean isEmpty();

    /**
     * Get the number of items in the cache
     *
     * @return the number of items in the cache
     */
    int size();

    /**
     * remove all entries from the cache
     */
    void clear();

    /**
     * @return the EventClock being used
     */
    EventClock getEventClock();

    /**
     * Set the EventClock to use
     */
    void setEventClock(EventClock eventClock);

}
