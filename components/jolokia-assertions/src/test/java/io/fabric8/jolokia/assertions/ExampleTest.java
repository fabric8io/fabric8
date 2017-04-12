/**
 *  Copyright 2005-2016 Red Hat, Inc.
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
package io.fabric8.jolokia.assertions;

import io.fabric8.utils.Block;
import org.jolokia.client.J4pClient;
import org.jolokia.jvmagent.JvmAgent;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.jolokia.assertions.Assertions.assertThat;
import static io.fabric8.utils.Asserts.assertAssertionError;


/**
 */
public class ExampleTest {
    private static final transient Logger LOG = LoggerFactory.getLogger(ExampleTest.class);

    protected J4pClient client;

    @Before
    public void init() {
        // lets initialise the JVM agent and the client
        JvmAgent.agentmain("", null);

        client = J4pClient.url("http://localhost:8778/jolokia")
                .connectionTimeout(3000)
                .build();
    }

    @Test
    public void testDoubleAttribute() throws Exception {
        assertThat(client).doubleAttribute("java.lang:type=OperatingSystem", "SystemCpuLoad").isGreaterThanOrEqualTo(0.0);

        assertAssertionError(new Block() {
            @Override
            public void invoke() throws Exception {
                assertThat(client).doubleAttribute("java.lang:type=OperatingSystem", "SystemCpuLoad").isLessThan(-2000.0);
            }
        });
    }

    @Test
    public void testOperationNullResult() throws Exception {
        assertThat(client).operation("java.util.logging:type=Logging", "getLoggerLevel", "io.fabric8.jolokia.assertions").isNull();
    }

    @Test
    public void testOperationNotNullResult() throws Exception {
        assertThat(client).operation("java.lang:type=Threading", "dumpAllThreads", true, true).isNotNull();
    }

    @Test
    public void testJsonArrayOperation() throws Exception {
        final JSONArrayAssert dumpAllThreads = assertThat(client).jsonArrayOperation("java.lang:type=Threading", "dumpAllThreads", true, true);
        dumpAllThreads.assertSize().isGreaterThan(1);

        int size = dumpAllThreads.get().size();
        for (int i = 0; i < size; i++) {
            JSONObjectAssert object = dumpAllThreads.assertJSONObject(i);
            object.isNotNull();
            object.assertString("threadName").isNotEmpty();
        }

        // lets try access an invalid array
        final int badIndex = size + 100;
        assertAssertionError(new Block() {
            @Override
            public void invoke() throws Exception {
                dumpAllThreads.assertJSONObject(badIndex);
            }
        });
    }

}
