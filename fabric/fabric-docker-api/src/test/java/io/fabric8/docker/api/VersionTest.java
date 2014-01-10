/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
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

public class VersionTest extends DockerBaseTest {

    @Test
    public void testVersion() throws IOException {
        String json = Resources.toString(getResource("version.json"), Charsets.UTF_8);
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.play();
        Docker docker = createDockerForMock(server);
        Version version = docker.version();
        assertNotNull(version);
        assertEquals(version.getVersion(), "0.2.2");
        assertEquals(version.getGitCommit(), "5a2a5cc+CHANGES");
        assertEquals(version.getGoVersion(), "go1.0.3");
    }
}
