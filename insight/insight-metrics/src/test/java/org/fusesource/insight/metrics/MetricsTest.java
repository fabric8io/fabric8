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
package org.fusesource.insight.metrics;

import org.codehaus.jackson.map.ObjectMapper;
import org.fusesource.insight.metrics.model.MBeanAttrs;
import org.fusesource.insight.metrics.model.MBeanOpers;
import org.fusesource.insight.metrics.model.Query;
import org.fusesource.insight.metrics.model.QueryResult;
import org.fusesource.insight.metrics.model.Request;
import org.fusesource.insight.metrics.model.Server;
import org.fusesource.insight.metrics.support.JmxUtils;
import org.fusesource.insight.metrics.support.Renderer;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MetricsTest {

    @Test
    public void testDefault() throws Exception {
        Query query = new Query("test", new HashSet<Request>(Arrays.asList(
                new MBeanAttrs("memory", "java.lang:type=Memory",
                        Arrays.asList("HeapMemoryUsage", "NonHeapMemoryUsage")),
                new MBeanOpers("deadlocks", "java.lang:type=Threading", "dumpAllThreads",
                        Arrays.<Object>asList(true, true), Arrays.<String>asList(boolean.class.getName(), boolean.class.getName()))
        )), null, null, null, 0, 0);

        System.gc();

        QueryResult qrs = JmxUtils.execute(new Server("local"), query,
                ManagementFactory.getPlatformMBeanServer());
        String output = new Renderer().render(qrs);

        Map map = new ObjectMapper().readValue(output, Map.class);
        assertEquals("local", map.get("host"));
        assertNotNull(map.get("timestamp"));
    }
}
