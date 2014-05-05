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
package io.fabric8.container.process;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JolokiaAgentHelperTest {
    @Test
    public void testFindUrl() throws Exception {
        assertJolokiaUrl("-javaagent:jolokia-agent.jar", "http://localhost:8778/jolokia/");
        assertJolokiaUrl("-javaagent:jolokia-agent.jar=host=myhost", "http://myhost:8778/jolokia/");
        assertJolokiaUrl("-javaagent:jolokia-agent.jar=host=0.0.0.0", "http://localhost:8778/jolokia/");
        assertJolokiaUrl("-javaagent:jolokia-agent.jar=host=0.0.0.0,port=8999", "http://localhost:8999/jolokia/");
        assertJolokiaUrl("-javaagent:jolokia-agent.jar=host=myhost,port=8999", "http://myhost:8999/jolokia/");
        assertJolokiaUrl("-javaagent:jolokia-agent.jar=port=9000,host=myhost2", "http://myhost2:9000/jolokia/");
        assertJolokiaUrl("\"-javaagent:jolokia-agent.jar=host=myhost,port=8999\"", "http://myhost:8999/jolokia/");
        assertJolokiaUrl("\"-javaagent:jolokia-agent.jar=port=9000,host=myhost2\"", "http://myhost2:9000/jolokia/");
    }

    public static void assertJolokiaUrl(String javaAgentString, String expected) {
        String url = JolokiaAgentHelper.findJolokiaUrlFromJavaAgent(javaAgentString, "localhost");
        assertEquals("Expected jolokia URL from javaAgent: " + javaAgentString, expected, url);
    }

}
