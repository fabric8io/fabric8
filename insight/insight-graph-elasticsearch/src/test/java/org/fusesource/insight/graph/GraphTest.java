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
package org.fusesource.insight.graph;

import org.fusesource.insight.graph.model.MBeanAttrs;
import org.fusesource.insight.graph.model.MBeanOpers;
import org.fusesource.insight.graph.model.Query;
import org.fusesource.insight.graph.model.QueryResult;
import org.fusesource.insight.graph.model.Request;
import org.fusesource.insight.graph.model.Server;
import org.fusesource.insight.graph.support.JSONReader;
import org.fusesource.insight.graph.support.JmxUtils;
import org.fusesource.insight.graph.support.Renderer;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GraphTest {

    @Test
    public void testJvm() throws Exception {
        Query query = new Query("jvm", new HashSet<Request>(Arrays.asList(
                        new MBeanAttrs("memory", "java.lang:type=Memory",
                                        Arrays.asList("HeapMemoryUsage", "NonHeapMemoryUsage")),
                        new MBeanAttrs("pool", "java.lang:type=MemoryPool,*",
                                        Arrays.asList("Usage", "PeakUsage")),
                        new MBeanAttrs("threading", "java.lang:type=Threading",
                                        Arrays.asList("DaemonThreadCount", "PeakThreadCount", "ThreadCount")),
                        new MBeanAttrs("buffer_pools", "java.nio:type=BufferPool,*",
                                        Arrays.asList("Count", "MemoryUsed", "TotalCapacity")),
                        new MBeanAttrs("gc", "java.lang:type=GarbageCollector,*",
                                        Arrays.asList("CollectionCount", "CollectionTime"))
                )), null, null, 0);

        System.gc();

        QueryResult qrs = JmxUtils.execute(new Server("local"), query,
                                           ManagementFactory.getPlatformMBeanServer());
        String output = new Renderer().render(qrs);

        Object json = new JSONReader().read(output);
        assertNotNull(json);
        assertTrue(json instanceof Map);
        Map map = (Map) json;
        assertEquals("local", map.get("host"));
        assertNotNull(map.get("timestamp"));
        assertNotNull(map.get("mem"));
        assertNotNull(map.get("threads"));
        assertNotNull(map.get("gc"));
        assertNotNull(map.get("buffer_pools"));

    }

    @Test
    public void testDefault() throws Exception {
        Query query = new Query("test", new HashSet<Request>(Arrays.asList(
                new MBeanAttrs("memory", "java.lang:type=Memory",
                        Arrays.asList("HeapMemoryUsage", "NonHeapMemoryUsage")),
                new MBeanOpers("deadlocks", "java.lang:type=Threading", "dumpAllThreads",
                        Arrays.<Object>asList(true, true), Arrays.<String>asList(boolean.class.getName(), boolean.class.getName()))
        )), null, null, 0);

        System.gc();

        QueryResult qrs = JmxUtils.execute(new Server("local"), query,
                ManagementFactory.getPlatformMBeanServer());
        String output = new Renderer().render(qrs);

        Object json = new JSONReader().read(output);
        assertNotNull(json);
        assertTrue(json instanceof Map);
        Map map = (Map) json;
        assertEquals("local", map.get("host"));
        assertNotNull(map.get("timestamp"));
    }
}
