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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.camel.test.TestSupport;

public class EventCacheTest extends TestSupport {

    public void testMaxSize() throws Exception {
        EventCache<String> eventCache = new DefaultEventCache<String>();
        int LIMIT = 10;
        int COUNT = LIMIT * 2;
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < COUNT; i++) {
            list.add("test " + i);
        }
        eventCache.setWindow("" + LIMIT);
        assertEquals(LIMIT, eventCache.getWindowCount());

        for (String s : list) {
            eventCache.add(s);
        }

        assertEquals(LIMIT, eventCache.size());
        List<String> eventCacheList = eventCache.getWindow();
        for (int i = 0; i < LIMIT; i++) {
            list.remove(0);
        }
        assertEquals(list.size(), eventCacheList.size());
        for (int i = 0; i < list.size(); i++) {
            assertEquals(list.get(i), eventCacheList.get(i));
        }
    }

    public void testTimeBasedExpiration() throws Exception {
        EventCache<String> eventCache = new MockEventCache<String>();
        EventClock clock = eventCache.getEventClock();
        clock.setCurrentTime(0, TimeUnit.MILLISECONDS);
        eventCache.setWindow("30 s");
        long windowTime = eventCache.getWindowTime();
        assertEquals((30 * 1000), windowTime);

        int COUNT = 10;
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < COUNT; i++) {
            list.add("test " + i);
        }

        for (String s : list) {
            eventCache.add(s);
        }
        assertEquals("Expecting ", COUNT, eventCache.size());
        clock.advanceClock(31, TimeUnit.SECONDS);
        assertTrue("Event Cache size = " + eventCache.size(), eventCache.isEmpty());
    }

    public void testTimeBasedExpirationEdgeCases() throws Exception {
        EventCache<String> eventCache = new MockEventCache<String>();
        EventClock clock = eventCache.getEventClock();
        clock.setCurrentTime(0, TimeUnit.MILLISECONDS);
        eventCache.setWindow("2 s");
        long windowTime = eventCache.getWindowTime();
        assertEquals((2 * 1000), windowTime);
        String str1 = "test1";
        eventCache.add(str1);
        assertEquals("Size should be 1", 1, eventCache.size());
        clock.advanceClock(5, TimeUnit.SECONDS);
        assertEquals("Size should be 0", 0, eventCache.size());

        String str2 = "test 2";
        eventCache.add(str2);
        assertEquals("Size should be 1", 1, eventCache.size());
    }

    public void testTimeAndMaxSizeExpiration() throws Exception {
        int COUNT = 500000;
        int LIMIT = 1000;
        EventCache<String> eventCache = new MockEventCache<String>();
        EventClock clock = eventCache.getEventClock();
        clock.setCurrentTime(0, TimeUnit.MILLISECONDS);
        eventCache.setWindow("5 s," + LIMIT);
        long windowTime = eventCache.getWindowTime();
        assertEquals((5 * 1000), windowTime);

        for (int i = 0; i < COUNT; i++) {
            eventCache.add("test " + i);
            clock.advanceClock(1, TimeUnit.MILLISECONDS);
            assertTrue("Event Cache should be less or equal to " + LIMIT + " is actually " + eventCache.size(), eventCache.size() <= LIMIT);
        }
    }
}
