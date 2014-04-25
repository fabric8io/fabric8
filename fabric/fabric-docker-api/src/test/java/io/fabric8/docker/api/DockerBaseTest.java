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


import com.google.mockwebserver.MockWebServer;

import java.io.IOException;

public class DockerBaseTest {

    public Docker createDocker(String url) throws IOException {
        DockerFactory factory = new DockerFactory(url);
        return factory.createDocker();
    }


    public Docker createDockerForMock(MockWebServer server) throws IOException {
        String url = "http://localhost:" + server.getPort();
        return createDocker(url);
    }
}
