/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.insight.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.insight.metrics.model.MBeanAttrs;
import io.fabric8.insight.metrics.model.MBeanOpers;
import io.fabric8.insight.metrics.model.Query;
import io.fabric8.insight.metrics.model.Request;
import io.fabric8.insight.metrics.service.support.JmxUtils;
import io.fabric8.insight.metrics.mvel.Renderer;
import io.fabric8.insight.metrics.model.QueryResult;
import io.fabric8.insight.metrics.model.Server;
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
