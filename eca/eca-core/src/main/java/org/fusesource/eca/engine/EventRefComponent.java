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