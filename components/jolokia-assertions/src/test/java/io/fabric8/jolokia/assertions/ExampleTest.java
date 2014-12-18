/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.jolokia.assertions;

import io.fabric8.utils.Block;
import org.jolokia.client.J4pClient;
import org.jolokia.jvmagent.JvmAgent;
import org.junit.Before;
import org.junit.Test;

import static io.fabric8.jolokia.assertions.Assertions.assertThat;
import static io.fabric8.utils.Asserts.assertAssertionError;


/**
 */
public class ExampleTest {
    protected J4pClient client;

    @Before
    public void init() {
        // lets initialise the JVM agent and the client
        JvmAgent.agentmain("");

        client = J4pClient.url("http://localhost:8778/jolokia")
                .connectionTimeout(3000)
                .build();
    }

    @Test
    public void testSomething() throws Exception {
        assertThat(client).doubleAttribute("java.lang:type=OperatingSystem", "SystemCpuLoad").isGreaterThanOrEqualTo(0.0);

        assertAssertionError(new Block() {
            @Override
            public void invoke() throws Exception {
                assertThat(client).doubleAttribute("java.lang:type=OperatingSystem", "SystemCpuLoad").isLessThan(-2000.0);
            }
        });
    }

}
