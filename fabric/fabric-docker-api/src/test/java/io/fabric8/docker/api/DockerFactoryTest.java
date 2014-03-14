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
package io.fabric8.docker.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class DockerFactoryTest {
    @Test
    public void testDockerHost() throws Exception {
        assertDockerHost("http://foo:1234", "foo");
        assertDockerHost("http:foo:1234", "foo");
        assertDockerHost("tcp://foo:1234", "foo");
        assertDockerHost("tcp:foo:1234", "foo");
        assertDockerHost("foo:1234", "foo");
    }

    public static void assertDockerHost(String address, String expectedHost) {
        DockerFactory factory = new DockerFactory(address);
        String dockerHost = factory.getDockerHost();
        assertEquals("docker host for address: " + address, expectedHost, dockerHost);
    }

}
