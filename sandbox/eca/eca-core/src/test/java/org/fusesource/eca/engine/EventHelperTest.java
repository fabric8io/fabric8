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

import junit.framework.TestCase;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.fusesource.eca.eventcache.EventCacheManager;

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
