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
