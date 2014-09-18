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
import org.junit.Before;

import java.io.IOException;

import static com.google.common.io.Resources.getResource;

public class DockerBaseTest {

    protected MockWebServer server = new MockWebServer();

    protected Docker docker;

    @Before
    public void dockerBaseTestBefore() throws IOException {
        server.play();
        docker = createDockerForMock(server);
    }

    public Docker createDocker(String url) throws IOException {
        DockerFactory factory = new DockerFactory(url);
        return factory.createDocker();
    }


    public Docker createDockerForMock(MockWebServer server) throws IOException {
        String url = "http://localhost:" + server.getPort();
        return createDocker(url);
    }

    protected void recordResponse(MockWebServer server, String responseName) {
        try {
            String json = Resources.toString(getResource(responseName + ".json"), Charsets.UTF_8);
            server.enqueue(new MockResponse().addHeader("Content-Type", "application/json").setBody(json));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void recordResponse(String responseName) {
        recordResponse(server, responseName);
    }

}
