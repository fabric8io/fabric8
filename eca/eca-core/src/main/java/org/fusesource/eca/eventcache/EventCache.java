/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.eca.eventcache;

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
