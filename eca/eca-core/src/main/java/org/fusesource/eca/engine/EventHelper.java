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

package org.fusesource.eca.engine;

import org.apache.camel.CamelContext;
import org.fusesource.eca.eventcache.EventCacheManager;

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
        return eventRefComponent.getEventCacheManager(context, type);
    }
}
