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

import org.apache.camel.test.TestSupport;

public class EventCacheSetWindowTest extends TestSupport {

    public void testSetWindow() throws Exception {
        EventCache wc = new DefaultEventCache();
        wc.setWindow("1000,10 sec");
        assertEquals(wc.getWindowTime(), 10 * 1000);
        assertTrue(wc.getWindowCount() == 1000);

        wc.setWindow("10 sec,50");
        assertEquals(wc.getWindowTime(), 10 * 1000);
        assertTrue(wc.getWindowCount() == 50);

        wc = new DefaultEventCache();
        wc.setWindow("10 m");
        assertTrue(wc.getWindowTime() == 10);
        assertTrue(wc.getWindowCount() == DefaultEventCache.NOT_SET);

        wc.setWindow("10 ms");
        assertTrue(wc.getWindowTime() == 10);
        assertTrue(wc.getWindowCount() == DefaultEventCache.NOT_SET);

        wc.setWindow("10 milliseconds");
        assertTrue(wc.getWindowTime() == 10);
        assertTrue(wc.getWindowCount() == DefaultEventCache.NOT_SET);

        wc = new DefaultEventCache();
        wc.setWindow("10 s");
        assertTrue(wc.getWindowTime() == 10 * 1000);
        assertTrue(wc.getWindowCount() == DefaultEventCache.NOT_SET);

        wc = new DefaultEventCache();
        wc.setWindow("10s");
        assertTrue(wc.getWindowTime() == 10 * 1000);
        assertTrue(wc.getWindowCount() == DefaultEventCache.NOT_SET);

        wc = new DefaultEventCache();
        wc.setWindow("10 sec");
        assertTrue(wc.getWindowTime() == 10 * 1000);
        assertTrue(wc.getWindowCount() == DefaultEventCache.NOT_SET);

        wc = new DefaultEventCache();
        wc.setWindow("10 seconds");
        assertTrue(wc.getWindowTime() == 10 * 1000);
        assertTrue(wc.getWindowCount() == DefaultEventCache.NOT_SET);

        wc = new DefaultEventCache();
        wc.setWindow("10 secs");
        assertTrue(wc.getWindowTime() == 10 * 1000);
        assertTrue(wc.getWindowCount() == DefaultEventCache.NOT_SET);

        wc = new DefaultEventCache();
        wc.setWindow("10 mins");
        assertTrue(wc.getWindowTime() == 10 * 60 * 1000);
        assertTrue(wc.getWindowCount() == DefaultEventCache.NOT_SET);

        wc = new DefaultEventCache();
        wc.setWindow("10 minutes");
        assertTrue(wc.getWindowTime() == 10 * 60 * 1000);
        assertTrue(wc.getWindowCount() == DefaultEventCache.NOT_SET);

        wc = new DefaultEventCache();
        wc.setWindow("10 hour");
        assertTrue(wc.getWindowTime() == 10 * 60 * 60 * 1000);
        assertTrue(wc.getWindowCount() == DefaultEventCache.NOT_SET);

        wc = new DefaultEventCache();
        wc.setWindow("10 hours");
        assertTrue(wc.getWindowTime() == 10 * 60 * 60 * 1000);
        assertTrue(wc.getWindowCount() == DefaultEventCache.NOT_SET);

        wc = new DefaultEventCache();
        wc.setWindow("10 hr");
        assertTrue(wc.getWindowTime() == 10 * 60 * 60 * 1000);
        assertTrue(wc.getWindowCount() == DefaultEventCache.NOT_SET);

        wc = new DefaultEventCache();
        wc.setWindow("10 hrs");
        assertTrue(wc.getWindowTime() == 10 * 60 * 60 * 1000);
        assertTrue(wc.getWindowCount() == DefaultEventCache.NOT_SET);
    }
}
