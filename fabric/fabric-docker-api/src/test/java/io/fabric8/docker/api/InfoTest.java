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
package io.fabric8.docker.api;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import org.junit.Test;

import java.io.IOException;

import static com.google.common.io.Resources.getResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InfoTest extends DockerBaseTest {

    @Test
    public void testInfo() throws IOException {
        String json = Resources.toString(getResource("info.json"), Charsets.UTF_8);
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().addHeader("Content-Type", "application/json").setBody(json));
        server.play();
        Docker docker = createDockerForMock(server);
        Info info = docker.info();
        assertNotNull(info);
        assertEquals(info.getContainers(), 11);
        assertEquals(info.getImages(), 16);
    }
}
