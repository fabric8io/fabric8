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
